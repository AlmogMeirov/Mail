#include "TCP_Server.h"
#include <iostream>
#include <cstdlib> // atoi
#include <string>

int main(int argc, char *argv[])
{
    if (argc != 3)
    {
        std::cerr << "Usage: " << argv[0] << " <IP_ADDRESS> <PORT>\n";
        return EXIT_FAILURE;
    }

    std::string ip = argv[1];
    int port = std::atoi(argv[2]);

    if (port <= 0 || port > 65535)
    {
        std::cerr << "Invalid port number.\n";
        return EXIT_FAILURE;
    }

    try
    {
        TCPServer server(ip, port);
        std::cout << "Starting server on IP: " << ip << ", Port: " << port << "\n";
        server.run();
    }
    catch (const std::exception &e)
    {
        std::cerr << "Exception: " << e.what() << "\n";
        return EXIT_FAILURE;
    }

    return EXIT_SUCCESS;
}
