use axum::{
    extract::{
        ws::{Message, WebSocket, WebSocketUpgrade},
        State,
    },
    response::Response,
};
use futures::{SinkExt, StreamExt};
use serde::Serialize;
use tokio::sync::broadcast;

use crate::AppState;

#[derive(Debug, Serialize)]
#[serde(tag = "type")]
enum ServerMessage {
    #[serde(rename = "line")]
    Line { payload: String },
    #[serde(rename = "initial")]
    Initial { payload: Vec<String> },
}

pub async fn ws_handler(
    ws: WebSocketUpgrade,
    State(state): State<AppState>,
) -> Response {
    ws.on_upgrade(move |socket| handle_socket(socket, state))
}

async fn handle_socket(socket: WebSocket, state: AppState) {
    let (mut sender, mut receiver) = socket.split();

    // Send initial log lines
    if let Ok(lines) = crate::services::log_watcher::read_last_lines(&state.config.logs_dir(), 100) {
        let msg = ServerMessage::Initial { payload: lines };
        if let Ok(json) = serde_json::to_string(&msg) {
            let _ = sender.send(Message::Text(json.into())).await;
        }
    }

    // Subscribe to log updates
    let mut rx = state.log_sender.subscribe();

    // Task to send log updates
    let send_task = tokio::spawn(async move {
        loop {
            match rx.recv().await {
                Ok(line) => {
                    let msg = ServerMessage::Line { payload: line };
                    if let Ok(json) = serde_json::to_string(&msg) {
                        if sender.send(Message::Text(json.into())).await.is_err() {
                            break;
                        }
                    }
                }
                Err(broadcast::error::RecvError::Lagged(_)) => {
                    // Skip lagged messages
                    continue;
                }
                Err(broadcast::error::RecvError::Closed) => {
                    break;
                }
            }
        }
    });

    // Task to keep connection alive / handle client messages
    let recv_task = tokio::spawn(async move {
        while let Some(Ok(msg)) = receiver.next().await {
            match msg {
                Message::Close(_) => break,
                Message::Ping(data) => {
                    // Pong is handled automatically by axum
                    let _ = data;
                }
                _ => {}
            }
        }
    });

    // Wait for either task to complete
    tokio::select! {
        _ = send_task => {}
        _ = recv_task => {}
    }
}
