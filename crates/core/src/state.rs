use std::sync::Arc;

use crate::{
    event::EventBus,
    registry::ServiceRegistry,
};

pub struct AppState {
    pub events: Arc<EventBus>,
    pub services: Arc<ServiceRegistry>,
}

impl Default for AppState {
    fn default() -> Self {
        Self {
            events: Arc::new(EventBus::default()),
            services: Arc::new(ServiceRegistry::default()),
        }
    }
}

impl AppState {
    pub fn new() -> Self {
        Self::default()
    }
}