use crate::adb;

pub struct FileService;

impl FileService {
    pub fn list(serial: &str, path: &str) -> Vec<String> {
        adb::list_files(serial, path)
    }
}