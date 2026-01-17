use notify::{Config, Event, RecommendedWatcher, RecursiveMode, Watcher};
use std::io::Read;
use std::path::Path;
use std::sync::Arc;
use tokio::sync::broadcast;

pub struct LogWatcher {
    _watcher: RecommendedWatcher,
}

impl LogWatcher {
    pub fn new(
        log_path: &Path,
        sender: broadcast::Sender<String>,
    ) -> anyhow::Result<Self> {
        let log_file = log_path.join("latest.log");
        let log_file_clone = log_file.clone();

        // Track the last position we read from
        let last_pos = Arc::new(std::sync::Mutex::new(
            std::fs::metadata(&log_file)
                .map(|m| m.len())
                .unwrap_or(0),
        ));
        let last_pos_clone = last_pos.clone();

        let mut watcher = RecommendedWatcher::new(
            move |res: Result<Event, notify::Error>| {
                if let Ok(event) = res {
                    if event.kind.is_modify() {
                        // Read new content from the log file
                        if let Ok(file) = std::fs::File::open(&log_file_clone) {
                            use std::io::{BufRead, BufReader, Seek, SeekFrom};

                            let mut reader = BufReader::new(file);
                            let mut pos = last_pos_clone.lock().unwrap();

                            if let Ok(_) = reader.seek(SeekFrom::Start(*pos)) {
                                let mut new_lines = Vec::new();
                                for line in reader.by_ref().lines() {
                                    if let Ok(line) = line {
                                        new_lines.push(line);
                                    }
                                }

                                // Update position
                                if let Ok(current_pos) = reader.stream_position() {
                                    *pos = current_pos;
                                }

                                // Send new lines
                                for line in new_lines {
                                    let _ = sender.send(line);
                                }
                            }
                        }
                    }
                }
            },
            Config::default(),
        )?;

        watcher.watch(log_path, RecursiveMode::NonRecursive)?;

        Ok(Self { _watcher: watcher })
    }
}

pub fn read_last_lines(log_path: &Path, count: usize) -> anyhow::Result<Vec<String>> {
    let log_file = log_path.join("latest.log");
    let content = std::fs::read_to_string(&log_file)?;
    let lines: Vec<String> = content
        .lines()
        .rev()
        .take(count)
        .map(String::from)
        .collect::<Vec<_>>()
        .into_iter()
        .rev()
        .collect();
    Ok(lines)
}

pub fn list_log_files(log_path: &Path) -> anyhow::Result<Vec<String>> {
    let mut files = Vec::new();
    for entry in std::fs::read_dir(log_path)? {
        let entry = entry?;
        let path = entry.path();
        if let Some(name) = path.file_name() {
            let name_str = name.to_string_lossy().to_string();
            if name_str.ends_with(".log") || name_str.ends_with(".log.gz") {
                files.push(name_str);
            }
        }
    }
    files.sort();
    files.reverse(); // Most recent first
    Ok(files)
}
