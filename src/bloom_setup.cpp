#include "bloom_setup.h"
#include "HashStd.h"
#include "HashDouble.h"
#include <sstream>
#include <iostream>

BloomFilter createFromConfigLine(const std::string& line) {
    std::stringstream ss(line);
    std::vector<std::string> args;
    std::string part;

    while (ss >> part) {
        args.push_back(part);
    }

    if (args.size() < 2 || args.size() > 3) {
        std::cerr << "Error: Enter 2 or 3 arguments." << std::endl;
        std::exit(1);
    }

    size_t bit_array_size = std::stoul(args[0]);
    if (bit_array_size <= 0) {
        std::cerr << "Error: Bit array size must be Positive." << std::endl;
        std::exit(1);
    }

    int hashStdCount = std::stoi(args[1]);
    int hashDoubleCount = 0;

    if (args.size() == 3) {
        hashDoubleCount = std::stoi(args[2]);
    }

    /*
    if (hashStdCount < 0 || hashStdCount > 10 || hashDoubleCount < 0 || hashDoubleCount > 10) {
        std::cerr << "Error: Hash function counts must be between 0 and 10." << std::endl;
        std::exit(1);
    }
    */

    std::vector<std::shared_ptr<HashFunction>> hash_funcs;

    for (int i = 0; i < hashStdCount; ++i) {
        hash_funcs.push_back(std::make_shared<HashStd>());
    }

    for (int i = 0; i < hashDoubleCount; ++i) {
        hash_funcs.push_back(std::make_shared<HashDouble>());
    }

    return BloomFilter(bit_array_size, hash_funcs);
}
