use std::io::{BufRead, BufReader, Write};
use std::net::{TcpListener, TcpStream};

fn handle_client(mut stream: TcpStream) {
    println!("Client Connected!");

    let mut reader = BufReader::new(stream.try_clone().unwrap());

    loop {
        let mut message = String::new();

        match reader.read_line(&mut message) {
            Ok(0) => {
                println!("Client Disconnected");
                break;
            }

            Ok(_) => {
                let msg = message.trim();

                println!("Received: {}", msg);

                let response = format!("WELCOME {}\n", msg);

                stream.write_all(response.as_bytes()).unwrap();
            }

            Err(e) => {
                println!("Error: {}", e);
                break;
            }
        }
    }
}

pub fn start_server() {
    println!("==============================");
    println!("AetherLink TCP Server");
    println!("==============================");

    let listener =
        TcpListener::bind("0.0.0.0:45876")
            .expect("Failed to bind port");

    println!("Listening on port 45876...");

    for stream in listener.incoming() {
        match stream {
            Ok(stream) => {
                std::thread::spawn(move || {
                    handle_client(stream);
                });
            }

            Err(e) => {
                println!("Connection Error: {}", e);
            }
        }
    }
}