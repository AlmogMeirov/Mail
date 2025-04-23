#include "bloom_setup.h"
#include "HashStd.h"
#include "HashDouble.h"

#include <sstream>

/**
 * Parse a config line containing just the size (e.g. "256").
 * Always uses both std::hash and HashDouble.
 *
 * @param line A string containing the desired bit-array size.
 * @return A BloomFilter initialized with that size and two hash functions.
 */
BloomFilter createFromConfigLine(const std::string& line) {
    std::stringstream ss(line);

    // Try to read the size; default to 128 on failure or out of bounds
    size_t bit_array_size = 128;
    if (!(ss >> bit_array_size) || bit_array_size == 0 || bit_array_size > 1000000) {
        bit_array_size = 128;
    }

    // Always include both hash functions
    std::vector<std::shared_ptr<HashFunction>> hash_funcs;
    hash_funcs.push_back(std::make_shared<HashStd>());
    hash_funcs.push_back(std::make_shared<HashDouble>());

    return BloomFilter(bit_array_size, hash_funcs);
}
