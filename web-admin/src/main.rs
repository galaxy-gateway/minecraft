mod auth;
mod config;
mod error;
mod handlers;
mod services;
mod websocket;

use std::sync::Arc;

use axum::{
    extract::DefaultBodyLimit,
    middleware,
    routing::{delete, get, post, put},
    Router,
};
use tokio::sync::broadcast;
use tower_http::{
    cors::{Any, CorsLayer},
    services::ServeDir,
    trace::TraceLayer,
};
use tower_sessions::{Expiry, MemoryStore, SessionManagerLayer};
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

use config::Config;
use services::log_watcher::LogWatcher;

#[derive(Clone)]
pub struct AppState {
    pub config: Arc<Config>,
    pub log_sender: broadcast::Sender<String>,
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // Initialize tracing
    tracing_subscriber::registry()
        .with(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "minecraft_web_admin=info,tower_http=info".into()),
        )
        .with(tracing_subscriber::fmt::layer())
        .init();

    // Load configuration
    let config = Arc::new(Config::from_env()?);
    tracing::info!("Loaded configuration");
    tracing::info!("Minecraft directory: {:?}", config.minecraft_dir);

    // Create broadcast channel for log streaming
    let (log_sender, _) = broadcast::channel::<String>(1000);

    // Start log watcher
    let _log_watcher = LogWatcher::new(&config.logs_dir(), log_sender.clone())
        .map_err(|e| tracing::warn!("Failed to start log watcher: {}", e))
        .ok();

    let state = AppState {
        config: config.clone(),
        log_sender,
    };

    // Session store
    let session_store = MemoryStore::default();
    let session_layer = SessionManagerLayer::new(session_store)
        .with_secure(false) // Set to true in production with HTTPS
        .with_expiry(Expiry::OnInactivity(time::Duration::hours(24)));

    // CORS layer (restrictive for local use)
    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);

    // Public routes (no auth required)
    let public_routes = Router::new()
        .route("/api/auth/login", post(auth::login))
        .route("/api/auth/logout", post(auth::logout))
        .route("/api/auth/status", get(auth::status));

    // Protected API routes
    let protected_routes = Router::new()
        // Server control
        .route("/api/server/status", get(handlers::server::status))
        .route("/api/server/start", post(handlers::server::start))
        .route("/api/server/stop", post(handlers::server::stop))
        .route("/api/server/restart", post(handlers::server::restart))
        // Console
        .route("/api/console/command", post(handlers::console::send_command))
        .route("/api/console/output", get(handlers::console::get_output))
        .route("/api/console/ws", get(websocket::console::ws_handler))
        // Logs
        .route("/api/logs/latest", get(handlers::logs::latest))
        .route("/api/logs/files", get(handlers::logs::files))
        .route("/api/logs/ws", get(websocket::logs::ws_handler))
        // Mods
        .route("/api/mods", get(handlers::mods::list))
        .route(
            "/api/mods/upload",
            post(handlers::mods::upload).layer(DefaultBodyLimit::max(100 * 1024 * 1024)), // 100MB
        )
        .route("/api/mods/:filename", delete(handlers::mods::delete))
        // Players - Ops
        .route("/api/players/ops", get(handlers::players::list_ops))
        .route("/api/players/ops", post(handlers::players::add_op))
        .route("/api/players/ops/:name", delete(handlers::players::remove_op))
        // Players - Whitelist
        .route("/api/players/whitelist", get(handlers::players::list_whitelist))
        .route("/api/players/whitelist", post(handlers::players::add_whitelist))
        .route("/api/players/whitelist/:name", delete(handlers::players::remove_whitelist))
        // Players - Banned
        .route("/api/players/banned", get(handlers::players::list_banned))
        .route("/api/players/banned", post(handlers::players::ban_player))
        .route("/api/players/banned/:name", delete(handlers::players::unban_player))
        // Backups
        .route("/api/backups", get(handlers::backups::list))
        .route("/api/backups", post(handlers::backups::create))
        .route("/api/backups/:filename", delete(handlers::backups::delete))
        .route("/api/backups/:filename/restore", post(handlers::backups::restore))
        // Scripts
        .route("/api/scripts", get(handlers::scripts::list))
        .route(
            "/api/scripts/upload",
            post(handlers::scripts::upload).layer(DefaultBodyLimit::max(1024 * 1024)), // 1MB
        )
        .route("/api/scripts/:filename", get(handlers::scripts::download))
        .route("/api/scripts/:filename", delete(handlers::scripts::delete))
        .route("/api/scripts/:filename/metadata", put(handlers::scripts::update_metadata))
        .route_layer(middleware::from_fn(auth::require_auth));

    // Combine routes and add static file serving
    let app = Router::new()
        .merge(public_routes)
        .merge(protected_routes)
        .fallback_service(ServeDir::new("static").append_index_html_on_directories(true))
        .layer(session_layer)
        .layer(cors)
        .layer(TraceLayer::new_for_http())
        .with_state(state);

    // Start server
    let listener = tokio::net::TcpListener::bind(&config.bind_address).await?;
    tracing::info!("Starting server on {}", config.bind_address);

    axum::serve(listener, app).await?;

    Ok(())
}
