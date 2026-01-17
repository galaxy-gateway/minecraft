use axum::{
    extract::{
        ws::{Message, WebSocket, WebSocketUpgrade},
        State,
    },
    response::Response,
};
use futures::{SinkExt, StreamExt};
use serde::{Deserialize, Serialize};
use std::time::Duration;
use tokio::time::interval;

use crate::services::tmux;
use crate::AppState;

#[derive(Debug, Deserialize)]
#[serde(tag = "type")]
enum ClientMessage {
    #[serde(rename = "command")]
    Command { payload: String },
}

#[derive(Debug, Serialize)]
#[serde(tag = "type")]
enum ServerMessage {
    #[serde(rename = "output")]
    Output { payload: String },
    #[serde(rename = "error")]
    Error { payload: String },
}

pub async fn ws_handler(
    ws: WebSocketUpgrade,
    State(_state): State<AppState>,
) -> Response {
    ws.on_upgrade(handle_socket)
}

async fn handle_socket(socket: WebSocket) {
    let (mut sender, mut receiver) = socket.split();

    // Task to periodically send console output
    let send_task = tokio::spawn(async move {
        let mut poll_interval = interval(Duration::from_millis(500));
        let mut last_output = String::new();

        loop {
            poll_interval.tick().await;

            if let Ok(output) = tmux::capture_output(50) {
                // Only send if output changed
                if output != last_output {
                    last_output = output.clone();
                    let msg = ServerMessage::Output { payload: output };
                    if let Ok(json) = serde_json::to_string(&msg) {
                        if sender.send(Message::Text(json.into())).await.is_err() {
                            break;
                        }
                    }
                }
            }
        }
    });

    // Task to receive commands from client
    let recv_task = tokio::spawn(async move {
        while let Some(Ok(msg)) = receiver.next().await {
            if let Message::Text(text) = msg {
                if let Ok(client_msg) = serde_json::from_str::<ClientMessage>(&text) {
                    match client_msg {
                        ClientMessage::Command { payload } => {
                            if let Err(e) = tmux::send_command(&payload) {
                                tracing::warn!("Failed to send command: {}", e);
                            }
                        }
                    }
                }
            }
        }
    });

    // Wait for either task to complete
    tokio::select! {
        _ = send_task => {}
        _ = recv_task => {}
    }
}
