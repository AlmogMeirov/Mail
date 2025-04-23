#include "bloom_setup.h"
#include "HashStd.h"
#include "HashDouble.h"

#include <sstream>
#include <iostream>
#include <cctype>

/**
 * Parses a configuration line to initialize a BloomFilter.
 * If the input is invalid (non-number or out of range), an error is printed and the program exits.
 *
 * @param line A string like "256" to specify bit array size.
 * @return A BloomFilter instance with default hash functions.
 */
BloomFilter createFromConfigLine(const std::string& line) {
    std::stringstream ss(line);
    std::string numberPart;
    ss >> numberPart;

    // Ensure input is a pure number
    if (numberPart.empty() || numberPart.find_first_not_of("0123456789") != std::string::npos) {
        std::cerr << "Error: Bit array size must be a positive integer." << std::endl;
        std::exit(1);
    }

    size_t bit_array_size = std::stoul(numberPart);

    if (bit_array_size == 0 || bit_array_size > 1000000) {
        std::cerr << "Error: Bit array size must be between 1 and 1,000,000." << std::endl;
        std::exit(1);
    }

    // Use default hash functions (std + double)
    std::vector<std::shared_ptr<HashFunction>> hash_funcs;
    hash_funcs.push_back(std::make_shared<HashStd>());
    hash_funcs.push_back(std::make_shared<HashDouble>());

    return BloomFilter(bit_array_size, hash_funcs);
}