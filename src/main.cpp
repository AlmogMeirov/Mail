#include <iostream>
#include <string>
#include <filesystem>      // for std::filesystem::exists()
#include "bloom_setup.h"
#include "BloomFilter.h"
#include "HashStd.h"
#include "HashDouble.h"
#include "URLChecker.h"
#include "UrlStorage.h"
#include <sstream>


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

    // 2) Build the filter
    BloomFilter bf = createFromConfigLine(line);
    UrlStorage storage("../data/urls.txt");

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

    while (true) {
        std::string input, url;
        int cmd;
        std::getline(std::cin, input);
        auto splitInput = splitArguments(input);
        try {
            cmd = std::stoi(splitInput[0]);
        } catch (const std::invalid_argument& e) {
            //std::cerr << "Invalid command: " << splitInput[0] << "\n";
            continue;
        }
        if (splitInput.size() != 2) {
            continue;
        }
        url = splitInput[1];
        //TODO: check if url is valid

        /*if (cmd == "exit") {
            // Persist one last time on exit
            if (!bf.saveToFile(STATE_FILE)) {
                std::cout << "Warning: failed to save state on exit\n";
            }
            break;
        }
        if (!(std::cin >> url)) {
            //std::cout << "Error: missing URL argument\n";
            continue;
        }*/

        if (cmd == 1) { // Add URL
            bf.add(url);
            //std::cout << "added\n";
            // Save immediately after each update
            if (!bf.saveToFile(STATE_FILE)) {
                std::cerr << "Error: failed to save BloomFilter state\n";
            }

        }
        if (cmd == 2) {
            std::cout << UrlChecker::outputString(url, bf, storage) << "\n";

        } 
    }

    return 0;
}
