use argon2::{Argon2, PasswordHash, PasswordHasher, PasswordVerifier};
use argon2::password_hash::rand_core::OsRng;
use argon2::password_hash::SaltString;
use axum::{
    extract::{Request, State},
    middleware::Next,
    response::Response,
    Json,
};
use serde::{Deserialize, Serialize};
use tower_sessions::Session;

use crate::error::{AppError, Result};
use crate::AppState;

const SESSION_USER_KEY: &str = "user";

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    pub username: String,
}

#[derive(Debug, Deserialize)]
pub struct LoginRequest {
    pub username: String,
    pub password: String,
}

#[derive(Debug, Serialize)]
pub struct AuthStatus {
    pub authenticated: bool,
    pub username: Option<String>,
}

pub fn verify_password(password: &str, hash: &str) -> bool {
    let parsed_hash = match PasswordHash::new(hash) {
        Ok(h) => h,
        Err(_) => return false,
    };
    Argon2::default()
        .verify_password(password.as_bytes(), &parsed_hash)
        .is_ok()
}

pub fn hash_password(password: &str) -> anyhow::Result<String> {
    let salt = SaltString::generate(&mut OsRng);
    let argon2 = Argon2::default();
    let hash = argon2
        .hash_password(password.as_bytes(), &salt)
        .map_err(|e| anyhow::anyhow!("Failed to hash password: {}", e))?;
    Ok(hash.to_string())
}

pub async fn login(
    State(state): State<AppState>,
    session: Session,
    Json(req): Json<LoginRequest>,
) -> Result<Json<AuthStatus>> {
    if req.username == state.config.admin_username
        && verify_password(&req.password, &state.config.admin_password_hash)
    {
        let user = User {
            username: req.username,
        };
        session
            .insert(SESSION_USER_KEY, user.clone())
            .await
            .map_err(|e| AppError::Internal(format!("Session error: {}", e)))?;

        Ok(Json(AuthStatus {
            authenticated: true,
            username: Some(user.username),
        }))
    } else {
        Err(AppError::InvalidCredentials)
    }
}

pub async fn logout(session: Session) -> Result<Json<AuthStatus>> {
    session.flush().await.map_err(|e| AppError::Internal(format!("Session error: {}", e)))?;
    Ok(Json(AuthStatus {
        authenticated: false,
        username: None,
    }))
}

pub async fn status(session: Session) -> Result<Json<AuthStatus>> {
    let user: Option<User> = session
        .get(SESSION_USER_KEY)
        .await
        .map_err(|e| AppError::Internal(format!("Session error: {}", e)))?;

    Ok(Json(AuthStatus {
        authenticated: user.is_some(),
        username: user.map(|u| u.username),
    }))
}

pub async fn require_auth(
    session: Session,
    request: Request,
    next: Next,
) -> std::result::Result<Response, AppError> {
    let user: Option<User> = session
        .get(SESSION_USER_KEY)
        .await
        .map_err(|e| AppError::Internal(format!("Session error: {}", e)))?;

    if user.is_some() {
        Ok(next.run(request).await)
    } else {
        Err(AppError::Unauthorized)
    }
}
