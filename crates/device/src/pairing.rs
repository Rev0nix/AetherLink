use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::fs;
use std::path::PathBuf;
use std::sync::{Arc, OnceLock, RwLock};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PairedDevice {
    pub id: String,
    pub name: String,
    pub paired_at: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PairingSession {
    pub device_id: String,
    pub device_name: String,
    pub pin: String,
    pub created_at: i64,
}

#[derive(Default)]
struct PairingState {
    paired_devices: HashMap<String, PairedDevice>,
    pending_sessions: HashMap<String, PairingSession>,
}

#[derive(Clone)]
pub struct PairingManager {
    state: Arc<RwLock<PairingState>>,
    storage_path: PathBuf,
}

static INSTANCE: OnceLock<PairingManager> = OnceLock::new();

impl Default for PairingManager {
    fn default() -> Self {
        Self::new()
    }
}

impl PairingManager {
    pub fn global() -> &'static PairingManager {
        INSTANCE.get_or_init(PairingManager::new)
    }

    pub fn new() -> Self {
        let storage_path = Self::default_storage_path();
        let mut initial_devices = HashMap::new();

        if let Ok(data) = fs::read_to_string(&storage_path) {
            if let Ok(devices) = serde_json::from_str::<Vec<PairedDevice>>(&data) {
                for dev in devices {
                    // Deduplicate by name: keep newest paired record
                    if let Some(existing_key) = initial_devices.iter().find_map(|(k, v): (&String, &PairedDevice)| {
                        if v.name == dev.name {
                            Some(k.clone())
                        } else {
                            None
                        }
                    }) {
                        if dev.paired_at > initial_devices[&existing_key].paired_at {
                            initial_devices.remove(&existing_key);
                            initial_devices.insert(dev.id.clone(), dev);
                        }
                    } else {
                        initial_devices.insert(dev.id.clone(), dev);
                    }
                }
            }
        }

        let mgr = Self {
            state: Arc::new(RwLock::new(PairingState {
                paired_devices: initial_devices,
                pending_sessions: HashMap::new(),
            })),
            storage_path,
        };
        mgr.persist();
        mgr
    }

    fn default_storage_path() -> PathBuf {
        let base_dir = dirs::config_dir()
            .or_else(dirs::home_dir)
            .unwrap_or_else(|| PathBuf::from("."));
        let config_dir = base_dir.join("vexyron");
        let _ = fs::create_dir_all(&config_dir);
        config_dir.join("paired_devices.json")
    }

    pub fn is_paired(&self, device_id: &str) -> bool {
        let state = self.state.read().unwrap();
        state.paired_devices.contains_key(device_id)
    }

    pub fn is_paired_by_name(&self, name: &str) -> bool {
        let state = self.state.read().unwrap();
        state.paired_devices.values().any(|d| d.name == name)
    }

    pub fn update_device_id_for_name(&self, name: &str, new_id: &str) {
        let mut state = self.state.write().unwrap();
        if let Some(old_key) = state.paired_devices.iter().find_map(|(k, v)| {
            if v.name == name && k != new_id {
                Some(k.clone())
            } else {
                None
            }
        }) {
            if let Some(mut dev) = state.paired_devices.remove(&old_key) {
                dev.id = new_id.to_string();
                dev.paired_at = chrono::Utc::now().timestamp();
                state.paired_devices.insert(new_id.to_string(), dev);
            }
        }
        drop(state);
        self.persist();
    }

    pub fn get_paired_devices(&self) -> Vec<PairedDevice> {
        let state = self.state.read().unwrap();
        let mut list: Vec<PairedDevice> = Vec::new();
        for dev in state.paired_devices.values() {
            if let Some(existing) = list.iter_mut().find(|d| d.name == dev.name) {
                if dev.paired_at > existing.paired_at {
                    *existing = dev.clone();
                }
            } else {
                list.push(dev.clone());
            }
        }
        list
    }

    pub fn request_pairing(&self, device_id: &str, device_name: &str) -> String {
        let pin = format!("{:04}", fastrand::u32(1000..=9999));
        let session = PairingSession {
            device_id: device_id.to_string(),
            device_name: device_name.to_string(),
            pin: pin.clone(),
            created_at: chrono::Utc::now().timestamp(),
        };

        let mut state = self.state.write().unwrap();
        state
            .pending_sessions
            .insert(device_id.to_string(), session);

        println!("Pairing requested for {device_name} ({device_id}) -> Verification PIN: {pin}");
        pin
    }

    pub fn get_pending_session(&self, device_id: &str) -> Option<PairingSession> {
        let state = self.state.read().unwrap();
        state.pending_sessions.get(device_id).cloned()
    }

    pub fn confirm_pairing(&self, device_id: &str, pin: &str) -> bool {
        let mut state = self.state.write().unwrap();

        let session = match state.pending_sessions.get(device_id) {
            Some(s) if s.pin == pin => s.clone(),
            _ => return false,
        };

        state.pending_sessions.remove(device_id);

        // Remove any existing paired record with the same name or device_id to prevent duplicates
        state
            .paired_devices
            .retain(|_, d| d.name != session.device_name && d.id != session.device_id);

        let paired = PairedDevice {
            id: session.device_id.clone(),
            name: session.device_name,
            paired_at: chrono::Utc::now().timestamp(),
        };

        state.paired_devices.insert(session.device_id, paired);
        drop(state);

        self.persist();
        true
    }

    pub fn reject_pairing(&self, device_id: &str) {
        let mut state = self.state.write().unwrap();
        state.pending_sessions.remove(device_id);
    }

    pub fn unpair_device(&self, device_id: &str) -> bool {
        let mut state = self.state.write().unwrap();
        let removed = state.paired_devices.remove(device_id).is_some();
        drop(state);

        if removed {
            self.persist();
        }
        removed
    }

    fn persist(&self) {
        let state = self.state.read().unwrap();
        let mut list: Vec<PairedDevice> = Vec::new();
        for dev in state.paired_devices.values() {
            if let Some(existing) = list.iter_mut().find(|d| d.name == dev.name) {
                if dev.paired_at > existing.paired_at {
                    *existing = dev.clone();
                }
            } else {
                list.push(dev.clone());
            }
        }
        if let Ok(json) = serde_json::to_string_pretty(&list) {
            let _ = fs::write(&self.storage_path, json);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_pairing_flow() {
        let manager = PairingManager::new();
        let device_id = "test-device-42";
        let device_name = "Test Android";

        assert!(!manager.is_paired(device_id));

        let pin = manager.request_pairing(device_id, device_name);
        assert_eq!(pin.len(), 4);
        assert!(pin.chars().all(|c| c.is_ascii_digit()));

        // Wrong PIN should fail
        assert!(!manager.confirm_pairing(device_id, "0000"));
        assert!(!manager.is_paired(device_id));

        // Correct PIN should succeed
        assert!(manager.confirm_pairing(device_id, &pin));
        assert!(manager.is_paired(device_id));

        // Device should be in paired devices list
        let paired = manager.get_paired_devices();
        assert!(paired.iter().any(|d| d.id == device_id));

        // Unpair
        assert!(manager.unpair_device(device_id));
        assert!(!manager.is_paired(device_id));
    }
}
