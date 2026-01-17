use axum::{extract::State, Json};
use serde::{Deserialize, Serialize};

use crate::error::Result;
use crate::services::tmux;
use crate::AppState;

#[derive(Debug, Deserialize)]
pub struct CommandRequest {
    pub command: String,
}

#[derive(Debug, Serialize)]
pub struct CommandResponse {
    pub success: bool,
    pub message: String,
}

#[derive(Debug, Serialize)]
pub struct ConsoleOutput {
    pub output: String,
}

pub async fn send_command(
    State(_state): State<AppState>,
    Json(req): Json<CommandRequest>,
) -> Result<Json<CommandResponse>> {
    tmux::send_command(&req.command)?;

    Ok(Json(CommandResponse {
        success: true,
        message: format!("Command sent: {}", req.command),
    }))
}

pub async fn get_output() -> Result<Json<ConsoleOutput>> {
    let output = tmux::capture_output(100)?;

    Ok(Json(ConsoleOutput { output }))
}
