use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PhoneFile {
    pub name: String,
    pub is_directory: bool,
}