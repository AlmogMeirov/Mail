#ifndef HASHFUNCTION_H
#define HASHFUNCTION_H

#include <string>

/**
 * @class HashFunction
 * @brief Abstract interface for string hash functors.
 *
 * Subclasses should override operator() to provide
 * a concrete hashing strategy for input strings.
 */
class HashFunction {
public:
    virtual ~HashFunction() = default;

    /**
     * @brief Compute hash value for a given string.
     * @param input The string to be hashed.
     * @return size_t Hash code for the input.
     */
    virtual size_t operator()(const std::string& input) const = 0;
};

#endif // HASHFUNCTION_H
