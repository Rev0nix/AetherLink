use serde::de::{self, Deserializer};
use serde::{Deserialize, Serialize};
use std::fmt::Display;
use std::str::FromStr;

pub fn from_str_or_number<'de, T, D>(deserializer: D) -> Result<T, D::Error>
where
    T: FromStr + serde::Deserialize<'de>,
    <T as FromStr>::Err: Display,
    D: Deserializer<'de>,
{
    #[derive(Deserialize)]
    #[serde(untagged)]
    enum StringOrNumber<T> {
        String(String),
        Number(T),
    }

    match StringOrNumber::<T>::deserialize(deserializer)? {
        StringOrNumber::String(s) => s.parse::<T>().map_err(de::Error::custom),
        StringOrNumber::Number(n) => Ok(n),
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Message {
    pub version: u8,
    pub message_type: MessageType,
    pub request_id: String,
    pub payload: serde_json::Value,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum MessageType {
    Hello,
    Welcome,
    DeviceInfo,
    Ping,
    Pong,
    PairRequest,
    PairResponse,
    FileRequest,
    FileChunk,
    FileComplete,
    Clipboard,
    Notification,
    Command,
    TerminalExec,
    TerminalOutput,
    Error,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TerminalExecPayload {
    pub command: String,
    #[serde(default)]
    pub cwd: Option<String>,
    #[serde(default)]
    pub sudo_password: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TerminalOutputPayload {
    pub command: String,
    pub stdout: String,
    pub stderr: String,
    pub exit_code: i32,
    pub cwd: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PairRequestPayload {
    pub device_id: String,
    pub device_name: String,
    #[serde(default = "default_platform")]
    pub platform: String,
    pub pin: Option<String>,
}

fn default_platform() -> String {
    "Android".to_string()
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum PairStatus {
    Pending,
    Paired,
    Confirmed,
    Rejected,
    Error,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PairResponsePayload {
    pub status: PairStatus,
    pub pin: Option<String>,
    pub message: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ClipboardPayload {
    pub content: String,
    #[serde(default = "default_timestamp", deserialize_with = "from_str_or_number")]
    pub timestamp: i64,
    #[serde(default)]
    pub source_device: Option<String>,
}

fn default_timestamp() -> i64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.as_millis() as i64)
        .unwrap_or(0)
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NotificationPayload {
    pub id: String,
    pub package_name: String,
    pub app_name: String,
    pub title: String,
    pub text: String,
    #[serde(default = "default_timestamp", deserialize_with = "from_str_or_number")]
    pub timestamp: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileTransferRequest {
    pub file_id: String,
    pub file_name: String,
    #[serde(deserialize_with = "from_str_or_number")]
    pub file_size: u64,
    #[serde(default)]
    pub mime_type: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileChunkPayload {
    pub file_id: String,
    #[serde(deserialize_with = "from_str_or_number")]
    pub chunk_index: usize,
    #[serde(deserialize_with = "from_str_or_number")]
    pub total_chunks: usize,
    pub data_base64: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileCompletePayload {
    pub file_id: String,
    pub file_name: String,
    pub status: String,
}

impl Message {
    pub fn new(
        message_type: MessageType,
        request_id: impl Into<String>,
        payload: serde_json::Value,
    ) -> Self {
        Self {
            version: 1,
            message_type,
            request_id: request_id.into(),
            payload,
        }
    }

    pub fn encode(&self) -> Result<String, serde_json::Error> {
        serde_json::to_string(self)
    }

    pub fn decode(value: &str) -> Result<Self, serde_json::Error> {
        serde_json::from_str(value)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn file_payload_flexible_deserialization() {
        // String numbers
        let json_str = r#"{"file_id":"f1","file_name":"test.png","file_size":"1048576","mime_type":"image/png"}"#;
        let req: FileTransferRequest = serde_json::from_str(json_str).unwrap();
        assert_eq!(req.file_size, 1048576);

        // Numeric numbers
        let json_num = r#"{"file_id":"f1","file_name":"test.png","file_size":1048576,"mime_type":"image/png"}"#;
        let req2: FileTransferRequest = serde_json::from_str(json_num).unwrap();
        assert_eq!(req2.file_size, 1048576);

        // Chunk with string numbers
        let chunk_str = r#"{"file_id":"f1","chunk_index":"2","total_chunks":"10","data_base64":"AQID"}"#;
        let chunk: FileChunkPayload = serde_json::from_str(chunk_str).unwrap();
        assert_eq!(chunk.chunk_index, 2);
        assert_eq!(chunk.total_chunks, 10);
    }
}
