use std::{
    any::{Any, TypeId},
    collections::HashMap,
    sync::{Arc, RwLock},
};

pub struct ServiceRegistry {
    services: RwLock<HashMap<TypeId, Arc<dyn Any + Send + Sync>>>,
}

impl Default for ServiceRegistry {
    fn default() -> Self {
        Self {
            services: RwLock::new(HashMap::new()),
        }
    }
}

impl ServiceRegistry {
    pub fn register<T>(&self, service: Arc<T>)
    where
        T: Any + Send + Sync + 'static,
    {
        self.services
            .write()
            .unwrap()
            .insert(TypeId::of::<T>(), service);
    }

    pub fn get<T>(&self) -> Option<Arc<T>>
    where
        T: Any + Send + Sync + 'static,
    {
        self.services
            .read()
            .unwrap()
            .get(&TypeId::of::<T>())
            .cloned()
            .and_then(|s| s.downcast::<T>().ok())
    }
}