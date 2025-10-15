use log::{Log, Metadata, Record, LevelFilter};
use once_cell::sync::OnceCell;
use std::sync::{Arc, Mutex};
use std::collections::VecDeque;

#[derive(Debug, Clone, uniffi::Enum)]
pub enum LogLevel {
    Error,
    Warn,
    Info,
    Debug,
    Trace,
}

#[derive(Debug, Clone, uniffi::Enum)]
pub enum LoggerError {
    AlreadyInitialized,
    FailedToSetLogger,
    FailedToAcquireLock,
    LoggerNotInitialized,
}

impl std::fmt::Display for LoggerError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            LoggerError::AlreadyInitialized => write!(f, "Logger already initialized"),
            LoggerError::FailedToSetLogger => write!(f, "Failed to set logger"),
            LoggerError::FailedToAcquireLock => write!(f, "Failed to acquire lock"),
            LoggerError::LoggerNotInitialized => write!(f, "Logger not initialized"),
        }
    }
}

impl From<log::Level> for LogLevel {
    fn from(level: log::Level) -> Self {
        match level {
            log::Level::Error => LogLevel::Error,
            log::Level::Warn => LogLevel::Warn,
            log::Level::Info => LogLevel::Info,
            log::Level::Debug => LogLevel::Debug,
            log::Level::Trace => LogLevel::Trace,
        }
    }
}

impl From<LogLevel> for log::LevelFilter {
    fn from(level: LogLevel) -> Self {
        match level {
            LogLevel::Error => log::LevelFilter::Error,
            LogLevel::Warn => log::LevelFilter::Warn,
            LogLevel::Info => log::LevelFilter::Info,
            LogLevel::Debug => log::LevelFilter::Debug,
            LogLevel::Trace => log::LevelFilter::Trace,
        }
    }
}

#[derive(Debug, Clone, uniffi::Record)]
pub struct LogMessage {
    pub tag: String,
    pub level: LogLevel,
    pub message: String,
    pub timestamp: u64, // Unix timestamp in milliseconds
}

// Logger struct that stores messages for external consumption
struct CustomLogger {
    custom_tag: String,
    message_queue: Arc<Mutex<VecDeque<LogMessage>>>,
    max_level: LevelFilter,
    max_queue_size: usize,
}

impl Log for CustomLogger {
    fn enabled(&self, metadata: &Metadata) -> bool {
        metadata.level() <= self.max_level
    }

    fn log(&self, record: &Record) {
        if self.enabled(record.metadata()) {
            // Store the log message for external consumption
            let log_message = LogMessage {
                tag: self.custom_tag.clone(),
                level: record.level().into(),
                message: record.args().to_string(),
                timestamp: std::time::SystemTime::now()
                    .duration_since(std::time::UNIX_EPOCH)
                    .unwrap_or_default()
                    .as_millis() as u64,
            };
            
            if let Ok(mut queue) = self.message_queue.lock() {
                queue.push_back(log_message);
                // Keep queue size manageable
                while queue.len() > self.max_queue_size {
                    queue.pop_front();
                }
            }
            
            // Print to console for debugging (all platforms)
            //println!("[{}] {} - {}", self.custom_tag, record.level(), record.args());
        }
    }

    fn flush(&self) {}
}

static LOGGER: OnceCell<CustomLogger> = OnceCell::new();

#[uniffi::export]
pub fn init_rust_logger(
    custom_tag: String,
    max_level: LogLevel,
) -> Result<(), LoggerError> {
    let level_filter: log::LevelFilter = max_level.into();
    let logger = CustomLogger { 
        custom_tag, 
        message_queue: Arc::new(Mutex::new(VecDeque::new())),
        max_level: level_filter,
        max_queue_size: 1000, // Configurable queue size
    };
    if LOGGER.set(logger).is_err() {
        return Err(LoggerError::AlreadyInitialized);
    }
    log::set_max_level(level_filter);
    log::set_logger(LOGGER.get().unwrap()).map_err(|_| LoggerError::FailedToSetLogger)
}

#[uniffi::export]
pub fn get_log_messages(count: Option<u32>) -> Vec<LogMessage> {
    if let Some(logger) = LOGGER.get() {
        if let Ok(mut queue) = logger.message_queue.lock() {
            let take_count = count.unwrap_or(10) as usize;
            let mut messages = Vec::new();
            for _ in 0..take_count {
                if let Some(message) = queue.pop_front() {
                    messages.push(message);
                } else {
                    break;
                }
            }
            return messages;
        }
    }
    Vec::new()
}

#[uniffi::export]
pub fn clear_log_messages() -> Result<(), LoggerError> {
    if let Some(logger) = LOGGER.get() {
        if let Ok(mut queue) = logger.message_queue.lock() {
            queue.clear();
            Ok(())
        } else {
            Err(LoggerError::FailedToAcquireLock)
        }
    } else {
        Err(LoggerError::LoggerNotInitialized)
    }
}
