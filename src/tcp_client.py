import socket

SERVER_IP = "127.0.0.1"
SERVER_PORT = 1234


class TCPClient:
    def __init__(self, ip=SERVER_IP, port=SERVER_PORT):
        self.ip = ip
        self.port = port
        self.sock = None

    def connect(self):
        try:
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.sock.connect((self.ip, self.port))
            print(f"Connected to {self.ip}:{self.port}")
            return True
        except (ConnectionRefusedError, socket.timeout, OSError) as e:
            print(f"[Error] Failed to connect: {e}")
            return False
