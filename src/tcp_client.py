import socket
import time

HOST = "server"  # Docker service name (not 127.0.0.1!)
PORT = 1234
RETRY_ATTEMPTS = 5
RETRY_DELAY = 2  # seconds


class TCPClient:
    def __init__(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.connected = False

    def connect(self):
        for attempt in range(1, RETRY_ATTEMPTS + 1):
            try:
                print(f"[Attempt {attempt}] Connecting to {HOST}:{PORT}...")
                self.socket.connect((HOST, PORT))
                print("Connected to server.")

                welcome = self.socket.recv(1024).decode()
                print("Server:", welcome.strip())

                self.connected = True
                return True

            except (ConnectionRefusedError, socket.timeout) as e:
                print(f"[Error] {e}")
                time.sleep(RETRY_DELAY)
        print(" Failed to connect after retries.")
        return False

    def send_command(self, command):
        self.socket.sendall((command + "\n").encode())
        return self.socket.recv(1024).decode()

    def close(self):
        self.socket.close()
