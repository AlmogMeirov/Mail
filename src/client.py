from tcp_client import TCPClient


def main():
    client = TCPClient()
    if not client.connect():
        print("Failed to connect. Exiting.")
        return

    try:
        while True:
            command = input().strip()
            if not command:
                continue
            response = client.send_command(command)
            if not response:
                print("🔌 Server closed the connection.")
                break
            print(f"Server: {response}")
    except KeyboardInterrupt:
        print("\n👋 Exiting client.")
    finally:
        client.close()


if __name__ == "__main__":
    main()
