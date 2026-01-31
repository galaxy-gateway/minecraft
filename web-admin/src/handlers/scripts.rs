use axum::{
    extract::{Multipart, Path, State},
    Json,
};
use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

use crate::error::{AppError, Result};
use crate::AppState;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct ScriptMetadata {
    pub name: String,
    pub filename: String,
    pub description: String,
    pub version: String,
    pub category: String,
    pub author: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Manifest {
    pub version: u32,
    pub updated: DateTime<Utc>,
    pub scripts: Vec<ScriptMetadata>,
}

#[derive(Debug, Serialize)]
pub struct ScriptInfo {
    pub name: String,
    pub filename: String,
    pub description: String,
    pub version: String,
    pub category: String,
    pub author: String,
    pub size: u64,
    pub modified: DateTime<Utc>,
}

#[derive(Debug, Serialize)]
pub struct ScriptList {
    pub scripts: Vec<ScriptInfo>,
}

#[derive(Debug, Serialize)]
pub struct ScriptActionResult {
    pub success: bool,
    pub message: String,
}

#[derive(Debug, Serialize)]
pub struct ScriptContent {
    pub filename: String,
    pub content: String,
    pub metadata: Option<ScriptMetadata>,
}

#[derive(Debug, Deserialize)]
pub struct UpdateMetadataRequest {
    pub name: Option<String>,
    pub description: Option<String>,
    pub version: Option<String>,
    pub category: Option<String>,
    pub author: Option<String>,
}

fn validate_filename(name: &str) -> Result<()> {
    if name.contains('/') || name.contains('\\') || name.contains("..") {
        return Err(AppError::BadRequest("Invalid filename".to_string()));
    }
    if name.starts_with('.') {
        return Err(AppError::BadRequest("Filename cannot start with '.'".to_string()));
    }
    if !name.ends_with(".lua") {
        return Err(AppError::BadRequest("Only .lua files are allowed".to_string()));
    }
    Ok(())
}

fn get_manifest_path(lua_scripts_dir: &PathBuf) -> PathBuf {
    lua_scripts_dir.join("manifest.json")
}

fn get_scripts_dir(lua_scripts_dir: &PathBuf) -> PathBuf {
    lua_scripts_dir.join("scripts")
}

fn read_manifest(lua_scripts_dir: &PathBuf) -> Result<Manifest> {
    let manifest_path = get_manifest_path(lua_scripts_dir);

    if !manifest_path.exists() {
        return Ok(Manifest {
            version: 1,
            updated: Utc::now(),
            scripts: vec![],
        });
    }

    let content = std::fs::read_to_string(&manifest_path)?;
    serde_json::from_str(&content)
        .map_err(|e| AppError::Internal(format!("Failed to parse manifest: {}", e)))
}

fn write_manifest(lua_scripts_dir: &PathBuf, manifest: &Manifest) -> Result<()> {
    let manifest_path = get_manifest_path(lua_scripts_dir);
    let content = serde_json::to_string_pretty(manifest)
        .map_err(|e| AppError::Internal(format!("Failed to serialize manifest: {}", e)))?;
    std::fs::write(&manifest_path, content)?;
    Ok(())
}

pub async fn list(State(state): State<AppState>) -> Result<Json<ScriptList>> {
    let lua_scripts_dir = state.config.lua_scripts_dir();
    let scripts_dir = get_scripts_dir(&lua_scripts_dir);
    let manifest = read_manifest(&lua_scripts_dir)?;

    let mut scripts = Vec::new();

    // Build a map of metadata by filename for quick lookup
    let metadata_map: std::collections::HashMap<String, ScriptMetadata> = manifest
        .scripts
        .into_iter()
        .map(|s| (s.filename.clone(), s))
        .collect();

    if scripts_dir.exists() {
        for entry in std::fs::read_dir(&scripts_dir)? {
            let entry = entry?;
            let path = entry.path();

            if let Some(ext) = path.extension() {
                if ext == "lua" {
                    let filename = path.file_name().unwrap().to_string_lossy().to_string();
                    let file_metadata = entry.metadata()?;
                    let modified: DateTime<Utc> = file_metadata.modified()?.into();

                    let script_metadata = metadata_map.get(&filename);

                    scripts.push(ScriptInfo {
                        name: script_metadata.map(|m| m.name.clone()).unwrap_or_else(|| filename.trim_end_matches(".lua").to_string()),
                        filename: filename.clone(),
                        description: script_metadata.map(|m| m.description.clone()).unwrap_or_default(),
                        version: script_metadata.map(|m| m.version.clone()).unwrap_or_else(|| "1.0.0".to_string()),
                        category: script_metadata.map(|m| m.category.clone()).unwrap_or_else(|| "uncategorized".to_string()),
                        author: script_metadata.map(|m| m.author.clone()).unwrap_or_else(|| "unknown".to_string()),
                        size: file_metadata.len(),
                        modified,
                    });
                }
            }
        }
    }

    scripts.sort_by(|a, b| a.name.to_lowercase().cmp(&b.name.to_lowercase()));
    Ok(Json(ScriptList { scripts }))
}

pub async fn upload(
    State(state): State<AppState>,
    mut multipart: Multipart,
) -> Result<Json<ScriptActionResult>> {
    let lua_scripts_dir = state.config.lua_scripts_dir();
    let scripts_dir = get_scripts_dir(&lua_scripts_dir);

    // Ensure directories exist
    if !scripts_dir.exists() {
        std::fs::create_dir_all(&scripts_dir)?;
    }

    let mut filename = String::new();
    let mut file_data: Option<Vec<u8>> = None;
    let mut name = String::new();
    let mut description = String::new();
    let mut version = "1.0.0".to_string();
    let mut category = "uncategorized".to_string();
    let mut author = "admin".to_string();

    while let Some(field) = multipart
        .next_field()
        .await
        .map_err(|e| AppError::BadRequest(e.to_string()))?
    {
        let field_name = field.name().unwrap_or("").to_string();

        match field_name.as_str() {
            "file" => {
                filename = field
                    .file_name()
                    .ok_or_else(|| AppError::BadRequest("No filename provided".to_string()))?
                    .to_string();

                validate_filename(&filename)?;

                let data = field
                    .bytes()
                    .await
                    .map_err(|e| AppError::BadRequest(e.to_string()))?;

                // Limit file size to 1MB for Lua scripts
                if data.len() > 1024 * 1024 {
                    return Err(AppError::BadRequest("File too large (max 1MB)".to_string()));
                }

                file_data = Some(data.to_vec());
            }
            "name" => {
                name = field.text().await.map_err(|e| AppError::BadRequest(e.to_string()))?;
            }
            "description" => {
                description = field.text().await.map_err(|e| AppError::BadRequest(e.to_string()))?;
            }
            "version" => {
                version = field.text().await.map_err(|e| AppError::BadRequest(e.to_string()))?;
            }
            "category" => {
                category = field.text().await.map_err(|e| AppError::BadRequest(e.to_string()))?;
            }
            "author" => {
                author = field.text().await.map_err(|e| AppError::BadRequest(e.to_string()))?;
            }
            _ => {}
        }
    }

    let data = file_data.ok_or_else(|| AppError::BadRequest("No file uploaded".to_string()))?;

    if filename.is_empty() {
        return Err(AppError::BadRequest("No filename provided".to_string()));
    }

    // Use filename as name if not provided
    if name.is_empty() {
        name = filename.trim_end_matches(".lua").to_string();
    }

    // Write the file
    let file_path = scripts_dir.join(&filename);
    std::fs::write(&file_path, &data)?;

    // Update manifest
    let mut manifest = read_manifest(&lua_scripts_dir)?;

    // Remove existing entry if any
    manifest.scripts.retain(|s| s.filename != filename);

    // Add new entry
    manifest.scripts.push(ScriptMetadata {
        name: name.clone(),
        filename: filename.clone(),
        description,
        version,
        category,
        author,
    });

    manifest.updated = Utc::now();
    write_manifest(&lua_scripts_dir, &manifest)?;

    Ok(Json(ScriptActionResult {
        success: true,
        message: format!("Uploaded: {}", name),
    }))
}

pub async fn download(
    State(state): State<AppState>,
    Path(filename): Path<String>,
) -> Result<Json<ScriptContent>> {
    validate_filename(&filename)?;

    let lua_scripts_dir = state.config.lua_scripts_dir();
    let scripts_dir = get_scripts_dir(&lua_scripts_dir);
    let file_path = scripts_dir.join(&filename);

    if !file_path.exists() {
        return Err(AppError::NotFound(format!("Script not found: {}", filename)));
    }

    let content = std::fs::read_to_string(&file_path)?;

    // Get metadata from manifest
    let manifest = read_manifest(&lua_scripts_dir)?;
    let metadata = manifest.scripts.into_iter().find(|s| s.filename == filename);

    Ok(Json(ScriptContent {
        filename,
        content,
        metadata,
    }))
}

pub async fn delete(
    State(state): State<AppState>,
    Path(filename): Path<String>,
) -> Result<Json<ScriptActionResult>> {
    validate_filename(&filename)?;

    let lua_scripts_dir = state.config.lua_scripts_dir();
    let scripts_dir = get_scripts_dir(&lua_scripts_dir);
    let file_path = scripts_dir.join(&filename);

    if !file_path.exists() {
        return Err(AppError::NotFound(format!("Script not found: {}", filename)));
    }

    // Delete the file
    std::fs::remove_file(&file_path)?;

    // Update manifest
    let mut manifest = read_manifest(&lua_scripts_dir)?;
    manifest.scripts.retain(|s| s.filename != filename);
    manifest.updated = Utc::now();
    write_manifest(&lua_scripts_dir, &manifest)?;

    Ok(Json(ScriptActionResult {
        success: true,
        message: format!("Deleted: {}", filename),
    }))
}

pub async fn update_metadata(
    State(state): State<AppState>,
    Path(filename): Path<String>,
    Json(request): Json<UpdateMetadataRequest>,
) -> Result<Json<ScriptActionResult>> {
    validate_filename(&filename)?;

    let lua_scripts_dir = state.config.lua_scripts_dir();
    let scripts_dir = get_scripts_dir(&lua_scripts_dir);
    let file_path = scripts_dir.join(&filename);

    if !file_path.exists() {
        return Err(AppError::NotFound(format!("Script not found: {}", filename)));
    }

    // Update manifest
    let mut manifest = read_manifest(&lua_scripts_dir)?;

    let mut found = false;
    for script in &mut manifest.scripts {
        if script.filename == filename {
            if let Some(ref name) = request.name {
                script.name = name.clone();
            }
            if let Some(ref description) = request.description {
                script.description = description.clone();
            }
            if let Some(ref version) = request.version {
                script.version = version.clone();
            }
            if let Some(ref category) = request.category {
                script.category = category.clone();
            }
            if let Some(ref author) = request.author {
                script.author = author.clone();
            }
            found = true;
            break;
        }
    }

    if !found {
        // Create new metadata entry
        manifest.scripts.push(ScriptMetadata {
            name: request.name.unwrap_or_else(|| filename.trim_end_matches(".lua").to_string()),
            filename: filename.clone(),
            description: request.description.unwrap_or_default(),
            version: request.version.unwrap_or_else(|| "1.0.0".to_string()),
            category: request.category.unwrap_or_else(|| "uncategorized".to_string()),
            author: request.author.unwrap_or_else(|| "admin".to_string()),
        });
    }

    manifest.updated = Utc::now();
    write_manifest(&lua_scripts_dir, &manifest)?;

    Ok(Json(ScriptActionResult {
        success: true,
        message: format!("Updated metadata for: {}", filename),
    }))
}
