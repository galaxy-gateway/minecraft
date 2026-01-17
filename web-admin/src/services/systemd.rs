use std::process::Command;
use crate::error::{AppError, Result};

const FABRIC_SERVICE: &str = "minecraft-fabric";

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ServiceStatus {
    Active,
    Inactive,
    Failed,
    Unknown,
}

impl ServiceStatus {
    pub fn as_str(&self) -> &'static str {
        match self {
            ServiceStatus::Active => "active",
            ServiceStatus::Inactive => "inactive",
            ServiceStatus::Failed => "failed",
            ServiceStatus::Unknown => "unknown",
        }
    }
}

pub fn get_service_status() -> Result<ServiceStatus> {
    let output = Command::new("systemctl")
        .args(["is-active", FABRIC_SERVICE])
        .output()?;

    let status_str = String::from_utf8_lossy(&output.stdout).trim().to_string();

    Ok(match status_str.as_str() {
        "active" => ServiceStatus::Active,
        "inactive" => ServiceStatus::Inactive,
        "failed" => ServiceStatus::Failed,
        _ => ServiceStatus::Unknown,
    })
}

pub fn start_server() -> Result<()> {
    let output = Command::new("sudo")
        .args(["systemctl", "start", FABRIC_SERVICE])
        .output()?;

    if output.status.success() {
        Ok(())
    } else {
        let stderr = String::from_utf8_lossy(&output.stderr);
        Err(AppError::CommandFailed(format!(
            "Failed to start server: {}",
            stderr
        )))
    }
}

pub fn stop_server() -> Result<()> {
    let output = Command::new("sudo")
        .args(["systemctl", "stop", FABRIC_SERVICE])
        .output()?;

    if output.status.success() {
        Ok(())
    } else {
        let stderr = String::from_utf8_lossy(&output.stderr);
        Err(AppError::CommandFailed(format!(
            "Failed to stop server: {}",
            stderr
        )))
    }
}

pub fn restart_server() -> Result<()> {
    let output = Command::new("sudo")
        .args(["systemctl", "restart", FABRIC_SERVICE])
        .output()?;

    if output.status.success() {
        Ok(())
    } else {
        let stderr = String::from_utf8_lossy(&output.stderr);
        Err(AppError::CommandFailed(format!(
            "Failed to restart server: {}",
            stderr
        )))
    }
}
