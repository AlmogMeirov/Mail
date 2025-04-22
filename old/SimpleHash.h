#ifndef SIMPLEHASH_H
#define SIMPLEHASH_H

#include "HashFunction.h"
#include <functional>

class SimpleHash : public HashFunction {
public:
    explicit SimpleHash(std::function<size_t(const std::string&)> func)
        : func_(std::move(func)) {}

    size_t operator()(const std::string& input) const override {
        return func_(input);
    }

private:
    std::function<size_t(const std::string&)> func_;
};

#endif // SIMPLEHASH_H
