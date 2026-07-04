use tokio::sync::broadcast;

/// Events shared across the application.
#[derive(Debug, Clone)]
pub enum Event {
    AppStarted,
    AppShutdown,

    DeviceConnected(String),
    DeviceDisconnected(String),

    NotificationReceived(String),

    ClipboardUpdated(String),
}

/// Global event bus.
#[derive(Clone)]
pub struct EventBus {
    sender: broadcast::Sender<Event>,
}

impl EventBus {
    pub fn new() -> Self {
        let (sender, _) = broadcast::channel(512);
        Self { sender }
    }

    pub fn emit(&self, event: Event) {
        let _ = self.sender.send(event);
    }

    pub fn subscribe(&self) -> broadcast::Receiver<Event> {
        self.sender.subscribe()
    }
}

impl Default for EventBus {
    fn default() -> Self {
        Self::new()
    }
}