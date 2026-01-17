use axum::{
    extract::{Multipart, Path, State},
    Json,
};
use serde::Serialize;

use crate::error::{AppError, Result};
use crate::services::file_manager::{self, FileInfo};
use crate::AppState;

#[derive(Debug, Serialize)]
pub struct ModList {
    pub mods: Vec<FileInfo>,
}

#[derive(Debug, Serialize)]
pub struct ModActionResult {
    pub success: bool,
    pub message: String,
}

pub async fn list(State(state): State<AppState>) -> Result<Json<ModList>> {
    let mods = file_manager::list_mods(&state.config.mods_dir())?;
    Ok(Json(ModList { mods }))
}

pub async fn upload(
    State(state): State<AppState>,
    mut multipart: Multipart,
) -> Result<Json<ModActionResult>> {
    while let Some(field) = multipart
        .next_field()
        .await
        .map_err(|e| AppError::BadRequest(e.to_string()))?
    {
        let filename = field
            .file_name()
            .ok_or_else(|| AppError::BadRequest("No filename provided".to_string()))?
            .to_string();

        if !filename.ends_with(".jar") {
            return Err(AppError::BadRequest("Only .jar files are allowed".to_string()));
        }

        let data = field
            .bytes()
            .await
            .map_err(|e| AppError::BadRequest(e.to_string()))?;

        // Limit file size to 100MB
        if data.len() > 100 * 1024 * 1024 {
            return Err(AppError::BadRequest("File too large (max 100MB)".to_string()));
        }

        file_manager::save_mod(&state.config.mods_dir(), &filename, &data)?;

        return Ok(Json(ModActionResult {
            success: true,
            message: format!("Uploaded: {}", filename),
        }));
    }

    Err(AppError::BadRequest("No file uploaded".to_string()))
}

pub async fn delete(
    State(state): State<AppState>,
    Path(filename): Path<String>,
) -> Result<Json<ModActionResult>> {
    file_manager::delete_mod(&state.config.mods_dir(), &filename)?;

    Ok(Json(ModActionResult {
        success: true,
        message: format!("Deleted: {}", filename),
    }))
}
