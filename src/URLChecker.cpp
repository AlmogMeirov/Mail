#include "UrlStorage.h"
#include "BloomFilter.h"
#include <string>
#include <iostream>

// Determines the output string based on URL presence in BloomFilter and UrlStorage
std::string outputString(const std::string& url, BloomFilter& BloomFilter, UrlStorage& UrlStorage) {
    // Check if the URL is possibly in the BloomFilter
    if (!BloomFilter.possiblyContains(url)) {
        return "false"; // URL is definitely not present
    } else {
        // Check if the URL is confirmed in UrlStorage
        return UrlStorage.contains(url) ? "true true" : "true false";
    }
}