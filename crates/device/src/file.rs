use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PhoneFile {
    pub name: String,
    pub path: String,
    pub is_directory: bool,
    pub size: u64,
}