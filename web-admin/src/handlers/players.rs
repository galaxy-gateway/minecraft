use axum::{
    extract::{Path, State},
    Json,
};
use serde::{Deserialize, Serialize};
use std::fs;

use crate::error::{AppError, Result};
use crate::services::tmux;
use crate::AppState;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct OpEntry {
    pub uuid: String,
    pub name: String,
    pub level: u32,
    #[serde(rename = "bypassesPlayerLimit")]
    pub bypasses_player_limit: bool,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct WhitelistEntry {
    pub uuid: String,
    pub name: String,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct BanEntry {
    pub uuid: String,
    pub name: String,
    pub created: String,
    pub source: String,
    pub expires: String,
    pub reason: String,
}

#[derive(Debug, Serialize)]
pub struct PlayerList<T> {
    pub players: Vec<T>,
}

#[derive(Debug, Deserialize)]
pub struct AddPlayerRequest {
    pub name: String,
}

#[derive(Debug, Serialize)]
pub struct PlayerActionResult {
    pub success: bool,
    pub message: String,
}

// Helper to validate player name
fn validate_player_name(name: &str) -> Result<()> {
    if name.is_empty() || name.len() > 16 {
        return Err(AppError::BadRequest("Invalid player name length".to_string()));
    }
    if !name.chars().all(|c| c.is_alphanumeric() || c == '_') {
        return Err(AppError::BadRequest("Player name contains invalid characters".to_string()));
    }
    Ok(())
}

// --- OPS ---

pub async fn list_ops(State(state): State<AppState>) -> Result<Json<PlayerList<OpEntry>>> {
    let path = state.config.minecraft_dir.join("ops.json");
    let players = if path.exists() {
        let content = fs::read_to_string(&path)?;
        serde_json::from_str(&content).unwrap_or_default()
    } else {
        Vec::new()
    };
    Ok(Json(PlayerList { players }))
}

pub async fn add_op(
    State(_state): State<AppState>,
    Json(req): Json<AddPlayerRequest>,
) -> Result<Json<PlayerActionResult>> {
    validate_player_name(&req.name)?;

    // Send op command to server
    if tmux::is_session_active() {
        tmux::send_command(&format!("op {}", req.name))?;
    }

    Ok(Json(PlayerActionResult {
        success: true,
        message: format!("Op command sent for: {}", req.name),
    }))
}

pub async fn remove_op(
    State(_state): State<AppState>,
    Path(name): Path<String>,
) -> Result<Json<PlayerActionResult>> {
    validate_player_name(&name)?;

    // Send deop command to server
    if tmux::is_session_active() {
        tmux::send_command(&format!("deop {}", name))?;
    }

    Ok(Json(PlayerActionResult {
        success: true,
        message: format!("Deop command sent for: {}", name),
    }))
}

// --- WHITELIST ---

pub async fn list_whitelist(
    State(state): State<AppState>,
) -> Result<Json<PlayerList<WhitelistEntry>>> {
    let path = state.config.minecraft_dir.join("whitelist.json");
    let players = if path.exists() {
        let content = fs::read_to_string(&path)?;
        serde_json::from_str(&content).unwrap_or_default()
    } else {
        Vec::new()
    };
    Ok(Json(PlayerList { players }))
}

pub async fn add_whitelist(
    State(_state): State<AppState>,
    Json(req): Json<AddPlayerRequest>,
) -> Result<Json<PlayerActionResult>> {
    validate_player_name(&req.name)?;

    if tmux::is_session_active() {
        tmux::send_command(&format!("whitelist add {}", req.name))?;
    }

    Ok(Json(PlayerActionResult {
        success: true,
        message: format!("Whitelist add command sent for: {}", req.name),
    }))
}

pub async fn remove_whitelist(
    State(_state): State<AppState>,
    Path(name): Path<String>,
) -> Result<Json<PlayerActionResult>> {
    validate_player_name(&name)?;

    if tmux::is_session_active() {
        tmux::send_command(&format!("whitelist remove {}", name))?;
    }

    Ok(Json(PlayerActionResult {
        success: true,
        message: format!("Whitelist remove command sent for: {}", name),
    }))
}

// --- BANNED ---

pub async fn list_banned(State(state): State<AppState>) -> Result<Json<PlayerList<BanEntry>>> {
    let path = state.config.minecraft_dir.join("banned-players.json");
    let players = if path.exists() {
        let content = fs::read_to_string(&path)?;
        serde_json::from_str(&content).unwrap_or_default()
    } else {
        Vec::new()
    };
    Ok(Json(PlayerList { players }))
}

pub async fn ban_player(
    State(_state): State<AppState>,
    Json(req): Json<AddPlayerRequest>,
) -> Result<Json<PlayerActionResult>> {
    validate_player_name(&req.name)?;

    if tmux::is_session_active() {
        tmux::send_command(&format!("ban {}", req.name))?;
    }

    Ok(Json(PlayerActionResult {
        success: true,
        message: format!("Ban command sent for: {}", req.name),
    }))
}

pub async fn unban_player(
    State(_state): State<AppState>,
    Path(name): Path<String>,
) -> Result<Json<PlayerActionResult>> {
    validate_player_name(&name)?;

    if tmux::is_session_active() {
        tmux::send_command(&format!("pardon {}", name))?;
    }

    Ok(Json(PlayerActionResult {
        success: true,
        message: format!("Pardon command sent for: {}", name),
    }))
}
