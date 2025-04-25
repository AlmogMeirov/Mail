#include <iostream>
#include <string>
#include <filesystem>      // for std::filesystem::exists()
#include "bloom_setup.h"
#include "BloomFilter.h"
#include "HashStd.h"
#include "HashDouble.h"
#include <sstream>
#include <vector>

std::vector<std::string> splitArguments(const std::string& line) {
    std::stringstream ss(line);
    std::vector<std::string> args;
    std::string arg;

    while (ss >> arg) {
        args.push_back(arg);
    }

    return args;
}

static constexpr char STATE_FILE[] = "bloom_state.bin";

int main() {
    // 1) Prompt for configuration
    std::cout << "Enter config line (e.g., 256 2 or 256 2 1): ";
    std::string line;
    std::getline(std::cin, line);

    BloomFilter bf = createFromConfigLine(line);
    std::cout << "BloomFilter successfully created." << std::endl;

    // 3) Check for an existing state file
    if (!std::filesystem::exists(STATE_FILE)) {
        std::cout << "No saved state file found - starting with an empty filter.\n";
    } else {
        // 4) Attempt to load it
        if (bf.loadFromFile(STATE_FILE)) {
            std::cout << "Loaded existing BloomFilter state.\n";
        } else {
            std::cerr << "Save file exists but is corrupted/invalid - starting with an empty filter.\n";
        }
    }

    std::cout << "Ready.\n";

    // 5) Interactive loop
    while (true) {
        std::cout << "> ";
        std::string cmd, url;
        if (!(std::cin >> cmd)) {
            // EOF or input error → exit
            break;
        }
        if (cmd == "exit") {
            // Persist one last time on exit
            if (!bf.saveToFile(STATE_FILE)) {
                std::cerr << "Warning: failed to save state on exit\n";
            }
            break;
        }
        if (!(std::cin >> url)) {
            std::cout << "Error: missing URL argument\n";
            continue;
        }

        if (cmd == "1") {
            bf.add(url);
            std::cout << "added\n";
            // Save immediately after each update
            if (!bf.saveToFile(STATE_FILE)) {
                std::cerr << "Error: failed to save BloomFilter state\n";
            }

        } else if (cmd == "2") {
            bool found = bf.possiblyContains(url);
            std::cout << (found
                ? "maybe in filter\n"
                : "definitely not in filter\n");

        } else {
            std::cout << "unknown command\n";
        }
    }

    return 0;
}
