#[derive(Debug, Clone)]
pub struct Config {
    pub app_name: String,
    pub app_version: String,
}

impl Default for Config {
    fn default() -> Self {
        Self {
            app_name: "AetherLink".into(),
            app_version: env!("CARGO_PKG_VERSION").into(),
        }
    }
}
