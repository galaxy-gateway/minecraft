use axum::Json;
use serde::Serialize;

use crate::error::Result;
use crate::services::{systemd, tmux};

#[derive(Debug, Serialize)]
pub struct ServerStatus {
    pub status: String,
    pub tmux_active: bool,
}

#[derive(Debug, Serialize)]
pub struct ActionResult {
    pub success: bool,
    pub message: String,
}

pub async fn status() -> Result<Json<ServerStatus>> {
    let service_status = systemd::get_service_status()?;
    let tmux_active = tmux::is_session_active();

    Ok(Json(ServerStatus {
        status: service_status.as_str().to_string(),
        tmux_active,
    }))
}

pub async fn start() -> Result<Json<ActionResult>> {
    systemd::start_server()?;

    Ok(Json(ActionResult {
        success: true,
        message: "Server start command sent".to_string(),
    }))
}

pub async fn stop() -> Result<Json<ActionResult>> {
    systemd::stop_server()?;

    Ok(Json(ActionResult {
        success: true,
        message: "Server stop command sent".to_string(),
    }))
}

pub async fn restart() -> Result<Json<ActionResult>> {
    systemd::restart_server()?;

    Ok(Json(ActionResult {
        success: true,
        message: "Server restart command sent".to_string(),
    }))
}
