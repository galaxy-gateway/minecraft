use axum::{
    extract::{Query, State},
    Json,
};
use serde::{Deserialize, Serialize};

use crate::error::Result;
use crate::services::log_watcher;
use crate::AppState;

#[derive(Debug, Deserialize)]
pub struct LatestQuery {
    #[serde(default = "default_lines")]
    pub lines: usize,
}

fn default_lines() -> usize {
    100
}

#[derive(Debug, Serialize)]
pub struct LogLines {
    pub lines: Vec<String>,
}

#[derive(Debug, Serialize)]
pub struct LogFiles {
    pub files: Vec<String>,
}

pub async fn latest(
    State(state): State<AppState>,
    Query(query): Query<LatestQuery>,
) -> Result<Json<LogLines>> {
    let lines = log_watcher::read_last_lines(&state.config.logs_dir(), query.lines)
        .map_err(|e| crate::error::AppError::Internal(e.to_string()))?;

    Ok(Json(LogLines { lines }))
}

pub async fn files(State(state): State<AppState>) -> Result<Json<LogFiles>> {
    let files = log_watcher::list_log_files(&state.config.logs_dir())
        .map_err(|e| crate::error::AppError::Internal(e.to_string()))?;

    Ok(Json(LogFiles { files }))
}
