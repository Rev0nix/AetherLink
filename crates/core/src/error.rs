use thiserror::Error;

#[derive(Debug, Error)]
pub enum CoreError {
    #[error("Initialization failed")]
    Initialization,

    #[error("Service unavailable")]
    ServiceUnavailable,
}

pub type Result<T> = std::result::Result<T, CoreError>;
