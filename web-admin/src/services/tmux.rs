use std::process::Command;
use crate::error::{AppError, Result};

const TMUX_SESSION: &str = "minecraft";

// Characters that could be used for shell injection
const FORBIDDEN_CHARS: &[char] = &['|', ';', '&', '$', '`', '\\', '"', '\'', '<', '>', '\n', '\r'];

pub fn sanitize_command(input: &str) -> Result<String> {
    // Check for forbidden characters
    for c in FORBIDDEN_CHARS {
        if input.contains(*c) {
            return Err(AppError::BadRequest(format!(
                "Command contains forbidden character: '{}'",
                c
            )));
        }
    }

    // Trim and limit length
    let sanitized = input.trim();
    if sanitized.len() > 256 {
        return Err(AppError::BadRequest(
            "Command too long (max 256 characters)".to_string(),
        ));
    }

    if sanitized.is_empty() {
        return Err(AppError::BadRequest("Command cannot be empty".to_string()));
    }

    Ok(sanitized.to_string())
}

pub fn send_command(command: &str) -> Result<()> {
    let sanitized = sanitize_command(command)?;

    let output = Command::new("tmux")
        .args(["send-keys", "-t", TMUX_SESSION, &sanitized, "Enter"])
        .output()?;

    if output.status.success() {
        Ok(())
    } else {
        let stderr = String::from_utf8_lossy(&output.stderr);
        Err(AppError::CommandFailed(format!(
            "Failed to send command: {}",
            stderr
        )))
    }
}

pub fn capture_output(lines: u32) -> Result<String> {
    let line_arg = format!("-{}", lines);

    let output = Command::new("tmux")
        .args([
            "capture-pane",
            "-t",
            TMUX_SESSION,
            "-p",
            "-S",
            &line_arg,
        ])
        .output()?;

    if output.status.success() {
        Ok(String::from_utf8_lossy(&output.stdout).to_string())
    } else {
        let stderr = String::from_utf8_lossy(&output.stderr);
        Err(AppError::CommandFailed(format!(
            "Failed to capture output: {}",
            stderr
        )))
    }
}

pub fn is_session_active() -> bool {
    Command::new("tmux")
        .args(["has-session", "-t", TMUX_SESSION])
        .status()
        .map(|s| s.success())
        .unwrap_or(false)
}
