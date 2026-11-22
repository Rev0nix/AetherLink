use mdns_sd::{ServiceDaemon, ServiceInfo};
use std::net::UdpSocket;

fn get_local_lan_ip() -> String {
    let socket = match UdpSocket::bind("0.0.0.0:0") {
        Ok(s) => s,
        Err(_) => return "127.0.0.1".to_string(),
    };

    if socket.connect("8.8.8.8:80").is_ok() {
        if let Ok(addr) = socket.local_addr() {
            return addr.ip().to_string();
        }
    }

    "127.0.0.1".to_string()
}

pub fn start_advertisement(port: u16) -> Result<ServiceDaemon, Box<dyn std::error::Error>> {
    let mdns = ServiceDaemon::new()?;

    let service_type = "_vexyron._tcp.local.";
    let instance_name = "VEXYRON Desktop";
    let host_name = "vexyron-desktop.local.";
    let properties = [("version", "1.0.0"), ("platform", "desktop")];
    let host_ip = get_local_lan_ip();

    let my_service = ServiceInfo::new(
        service_type,
        instance_name,
        host_name,
        host_ip.as_str(),
        port,
        &properties[..],
    )?;

    mdns.register(my_service)?;
    println!("mDNS service registered: {instance_name} ({service_type}) at {host_ip}:{port}");

    Ok(mdns)
}
