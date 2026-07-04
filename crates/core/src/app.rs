use crate::{config::Config, event::Event, logging, state::AppState};

pub struct Application {
    pub config: Config,
    pub state: AppState,
}

impl Application {
    pub fn new() -> Self {
        Self {
            config: Config::default(),
            state: AppState::new(),
        }
    }

    pub async fn start(&self) {
        logging::init_logging();

        println!(
            "Starting {} {}",
            self.config.app_name, self.config.app_version
        );

        self.state.events.emit(Event::AppStarted);
    }

    pub async fn shutdown(&self) {
        self.state.events.emit(Event::AppShutdown);

        println!("Application stopped");
    }
}

impl Default for Application {
    fn default() -> Self {
        Self::new()
    }
}
