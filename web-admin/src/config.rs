use std::path::PathBuf;

#[derive(Clone, Debug)]
pub struct Config {
    pub bind_address: String,
    pub admin_username: String,
    pub admin_password_hash: String,
    pub minecraft_dir: PathBuf,
    pub session_secret: String,
}

impl Config {
    pub fn from_env() -> anyhow::Result<Self> {
        dotenvy::dotenv().ok();

        Ok(Self {
            bind_address: std::env::var("BIND_ADDRESS")
                .unwrap_or_else(|_| "127.0.0.1:8080".to_string()),
            admin_username: std::env::var("ADMIN_USERNAME")
                .unwrap_or_else(|_| "admin".to_string()),
            admin_password_hash: std::env::var("ADMIN_PASSWORD_HASH")
                .map_err(|_| anyhow::anyhow!("ADMIN_PASSWORD_HASH must be set"))?,
            minecraft_dir: PathBuf::from(
                std::env::var("MINECRAFT_DIR")
                    .unwrap_or_else(|_| "/home/caavere/Projects/minecraft".to_string()),
            ),
            session_secret: std::env::var("SESSION_SECRET")
                .unwrap_or_else(|_| "development_secret_change_in_production".to_string()),
        })
    }

    pub fn logs_dir(&self) -> PathBuf {
        self.minecraft_dir.join("logs")
    }

    pub fn mods_dir(&self) -> PathBuf {
        self.minecraft_dir.join("mods")
    }

    pub fn backups_dir(&self) -> PathBuf {
        self.minecraft_dir.join("backups")
    }

    pub fn backup_script(&self) -> PathBuf {
        self.minecraft_dir.join("backup.sh")
    }

    pub fn restore_script(&self) -> PathBuf {
        self.minecraft_dir.join("restore.sh")
    }
}
