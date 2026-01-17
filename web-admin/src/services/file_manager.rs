use std::path::{Path, PathBuf};
use std::process::Command;
use chrono::{DateTime, Utc};
use serde::Serialize;

use crate::error::{AppError, Result};

#[derive(Debug, Serialize)]
pub struct FileInfo {
    pub name: String,
    pub size: u64,
    pub modified: DateTime<Utc>,
}

// Validate filename to prevent path traversal
fn validate_filename(name: &str) -> Result<()> {
    if name.contains('/') || name.contains('\\') || name.contains("..") {
        return Err(AppError::BadRequest("Invalid filename".to_string()));
    }
    if name.starts_with('.') {
        return Err(AppError::BadRequest("Filename cannot start with '.'".to_string()));
    }
    Ok(())
}

pub fn list_mods(mods_dir: &Path) -> Result<Vec<FileInfo>> {
    let mut mods = Vec::new();

    if !mods_dir.exists() {
        return Ok(mods);
    }

    for entry in std::fs::read_dir(mods_dir)? {
        let entry = entry?;
        let path = entry.path();

        if let Some(ext) = path.extension() {
            if ext == "jar" {
                let metadata = entry.metadata()?;
                let modified: DateTime<Utc> = metadata.modified()?.into();

                mods.push(FileInfo {
                    name: path.file_name().unwrap().to_string_lossy().to_string(),
                    size: metadata.len(),
                    modified,
                });
            }
        }
    }

    mods.sort_by(|a, b| a.name.to_lowercase().cmp(&b.name.to_lowercase()));
    Ok(mods)
}

pub fn delete_mod(mods_dir: &Path, filename: &str) -> Result<()> {
    validate_filename(filename)?;

    if !filename.ends_with(".jar") {
        return Err(AppError::BadRequest("Only .jar files can be deleted".to_string()));
    }

    let path = mods_dir.join(filename);
    if !path.exists() {
        return Err(AppError::NotFound(format!("Mod not found: {}", filename)));
    }

    std::fs::remove_file(&path)?;
    Ok(())
}

pub fn save_mod(mods_dir: &Path, filename: &str, data: &[u8]) -> Result<PathBuf> {
    validate_filename(filename)?;

    if !filename.ends_with(".jar") {
        return Err(AppError::BadRequest("Only .jar files can be uploaded".to_string()));
    }

    // Create mods directory if it doesn't exist
    if !mods_dir.exists() {
        std::fs::create_dir_all(mods_dir)?;
    }

    let path = mods_dir.join(filename);
    std::fs::write(&path, data)?;
    Ok(path)
}

pub fn list_backups(backups_dir: &Path) -> Result<Vec<FileInfo>> {
    let mut backups = Vec::new();

    if !backups_dir.exists() {
        return Ok(backups);
    }

    for entry in std::fs::read_dir(backups_dir)? {
        let entry = entry?;
        let path = entry.path();

        if let Some(ext) = path.extension() {
            if ext == "gz" {
                let metadata = entry.metadata()?;
                let modified: DateTime<Utc> = metadata.modified()?.into();

                backups.push(FileInfo {
                    name: path.file_name().unwrap().to_string_lossy().to_string(),
                    size: metadata.len(),
                    modified,
                });
            }
        }
    }

    backups.sort_by(|a, b| b.modified.cmp(&a.modified)); // Most recent first
    Ok(backups)
}

pub fn create_backup(backup_script: &Path, name: Option<&str>) -> Result<String> {
    let mut cmd = Command::new(backup_script);

    if let Some(n) = name {
        validate_filename(n)?;
        cmd.arg(n);
    }

    // Set non-interactive mode by using 'yes' pipe for overwrite prompt
    cmd.stdin(std::process::Stdio::null());

    let output = cmd.output()?;

    if output.status.success() {
        Ok(String::from_utf8_lossy(&output.stdout).to_string())
    } else {
        let stderr = String::from_utf8_lossy(&output.stderr);
        let stdout = String::from_utf8_lossy(&output.stdout);
        Err(AppError::CommandFailed(format!(
            "Backup failed: {} {}",
            stderr, stdout
        )))
    }
}

pub fn delete_backup(backups_dir: &Path, filename: &str) -> Result<()> {
    validate_filename(filename)?;

    if !filename.ends_with(".tar.gz") {
        return Err(AppError::BadRequest("Invalid backup file".to_string()));
    }

    let path = backups_dir.join(filename);
    if !path.exists() {
        return Err(AppError::NotFound(format!("Backup not found: {}", filename)));
    }

    std::fs::remove_file(&path)?;
    Ok(())
}

pub fn restore_backup(restore_script: &Path, backup_name: &str) -> Result<String> {
    validate_filename(backup_name)?;

    // The restore script expects just the backup name without extension
    let name = backup_name
        .trim_end_matches(".tar.gz")
        .trim_start_matches("world_");

    let output = Command::new(restore_script)
        .arg(name)
        .stdin(std::process::Stdio::null())
        .output()?;

    if output.status.success() {
        Ok(String::from_utf8_lossy(&output.stdout).to_string())
    } else {
        let stderr = String::from_utf8_lossy(&output.stderr);
        let stdout = String::from_utf8_lossy(&output.stdout);
        Err(AppError::CommandFailed(format!(
            "Restore failed: {} {}",
            stderr, stdout
        )))
    }
}
