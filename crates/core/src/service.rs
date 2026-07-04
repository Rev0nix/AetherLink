pub trait Service: Send + Sync {
    fn name(&self) -> &'static str;

    fn start(&self) {}

    fn stop(&self) {}
}
