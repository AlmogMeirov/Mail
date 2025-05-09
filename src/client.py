from Mail.src.tcp_client import TCPClient


def main():
    client = TCPClient()
    if client.connect():
        print("Connected successfully. Ready to send commands.")
    else:
        print("Failed to connect. Exiting.")


if __name__ == "__main__":
    main()
