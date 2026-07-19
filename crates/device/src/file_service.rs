use crate::{
    adb,
    PhoneFile,
};

pub struct FileService;

impl FileService {
    pub fn list(serial: &str, path: &str) -> Vec<PhoneFile> {
        adb::list_files(serial, path)
    }

    pub fn upload(
        serial: &str,
        local: &str,
        remote: &str,
    ) {
        adb::upload_file(serial, local, remote);
    }

    pub fn download(
        serial: &str,
        remote: &str,
        local: &str,
    ) {
        adb::download_file(serial, remote, local);
    }

    pub fn delete(
        serial: &str,
        path: &str,
    ) {
        adb::delete_file(serial, path);
    }

    pub fn rename(
        serial: &str,
        old: &str,
        new: &str,
    ) {
        adb::rename_file(serial, old, new);
    }
}