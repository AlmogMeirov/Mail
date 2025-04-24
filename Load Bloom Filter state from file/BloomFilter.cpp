#include "BloomFilter.h"
#include <fstream>
#include <iostream>
#include <fstream>
#include <cstdint> // For uint8_t
#include <string>
#include <vector>
// Adds a string (URL) to the Bloom Filter by marking bits based on hash functions
void BloomFilter::add(const std::string& url) {
    for (const auto& hash : hash_functions) {
        size_t idx = (*hash)(url) % size;// Apply the hash function, get index within bounds
        bit_array[idx] = true; // Set the bit at that index to true
    }
    if (!this->saveToFile("bloom_state.bin")) {
        std::cerr << "Warning: failed to save BloomFilter state\n";
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

bool BloomFilter::saveToFile(const std::string& filename) const {
    std::ofstream out{ filename, std::ios::binary };
    if (!out) return false;

    // 1) Write the size so we can verify it during load
    out.write(reinterpret_cast<const char*>(&size), sizeof(size));

    // 2) Pack each group of 8 bits into a single byte and write it
    uint8_t byte = 0;
    int bits_filled = 0;
    for (bool b : bit_array) {
        byte = (byte << 1) | (b ? 1 : 0);
        if (++bits_filled == 8) {
            out.put(static_cast<char>(byte));
            byte = 0;
            bits_filled = 0;
        }
    }
    // 3) Write any remaining bits, if there are less than 8
    if (bits_filled) {
        byte <<= (8 - bits_filled);
        out.put(static_cast<char>(byte));
    }

    return true;
}


// Loads the Bloom Filter's bit array from a binary file
bool BloomFilter::loadFromFile(const std::string& filename) {
    std::ifstream in{ filename, std::ios::binary };
    if (!in) {
        // File does not exist or cannot be opened
        return false;
    }

    // 1) read stored size
    size_t file_size = 0;
    in.read(reinterpret_cast<char*>(&file_size), sizeof(file_size));
    if (!in || file_size == 0) {
        // couldn't read size or got zero
        return false;
    }
    if (file_size != this->size) {
        // stored filter size differs from our in‑memory size
        return false;
    }

    // 2) read packed bits into a temporary array
    std::vector<bool> new_bits(size, false);
    char byte = 0;
    size_t idx = 0;
    while (in.get(byte) && idx < size) {
        // unpack one byte → up to 8 bits
        for (int shift = 7; shift >= 0 && idx < size; --shift, ++idx) {
            new_bits[idx] = ((byte >> shift) & 1) != 0;
        }
    }

    if (idx < size) {
        // didn't read enough bits → corrupted file
        return false;
    }

    // 3) only now, replace the in‑memory bit_array
    this->bit_array = std::move(new_bits);
    return true;
}
