use axum::{
    extract::{Path, State},
    Json,
};
use serde::{Deserialize, Serialize};

use crate::error::Result;
use crate::services::file_manager::{self, FileInfo};
use crate::AppState;

#[derive(Debug, Serialize)]
pub struct BackupList {
    pub backups: Vec<FileInfo>,
}

#[derive(Debug, Deserialize)]
pub struct CreateBackupRequest {
    pub name: Option<String>,
}

#[derive(Debug, Serialize)]
pub struct BackupActionResult {
    pub success: bool,
    pub message: String,
    pub output: Option<String>,
}

pub async fn list(State(state): State<AppState>) -> Result<Json<BackupList>> {
    let backups = file_manager::list_backups(&state.config.backups_dir())?;
    Ok(Json(BackupList { backups }))
}

pub async fn create(
    State(state): State<AppState>,
    Json(req): Json<CreateBackupRequest>,
) -> Result<Json<BackupActionResult>> {
    let output = file_manager::create_backup(
        &state.config.backup_script(),
        req.name.as_deref(),
    )?;

    Ok(Json(BackupActionResult {
        success: true,
        message: "Backup created".to_string(),
        output: Some(output),
    }))
}

pub async fn delete(
    State(state): State<AppState>,
    Path(filename): Path<String>,
) -> Result<Json<BackupActionResult>> {
    file_manager::delete_backup(&state.config.backups_dir(), &filename)?;

    Ok(Json(BackupActionResult {
        success: true,
        message: format!("Deleted: {}", filename),
        output: None,
    }))
}

pub async fn restore(
    State(state): State<AppState>,
    Path(filename): Path<String>,
) -> Result<Json<BackupActionResult>> {
    let output = file_manager::restore_backup(&state.config.restore_script(), &filename)?;

    Ok(Json(BackupActionResult {
        success: true,
        message: "Restore completed".to_string(),
        output: Some(output),
    }))
}
