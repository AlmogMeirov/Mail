#include "BloomFilter.h"

// Adds a string (URL) to the Bloom Filter by marking bits based on hash functions
void BloomFilter::add(const std::string& url) {
    for (const auto& hash : hash_functions) {
        size_t idx = (*hash)(url) % size;// Apply the hash function, get index within bounds
        bit_array[idx] = true; // Set the bit at that index to true
    }
}


// Checks if a string (URL) might be in the Bloom Filterand returns true only if all bits for all hash functions are set
// False means definitely not present, true means "maybe" 
bool BloomFilter::possiblyContains(const std::string& url) const {
    for (const auto& hash : hash_functions) {
        size_t idx = (*hash)(url) % size; // Apply the hash and get index
        if (!bit_array[idx]) return false; // If even one bit is 0, URL is definitely not in the filter
    }
    return true; // URL might be in the filter
}