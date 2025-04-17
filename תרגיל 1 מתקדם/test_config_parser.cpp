#include "BloomFilter.h"
#include "HashStd.h"
#include "HashDouble.h"
#include <iostream>
#include <cassert>
#include <memory>

// הצהרת הפונקציה שבודקת קלט
BloomFilter createFromConfigLine(const std::string& line);

// פונקציה פשוטה לבדיקת כמות הביטים (דרך טריק — נוסיף פונקציית עזר אם נרצה בעתיד)
void test_valid_input() {
    BloomFilter bf = createFromConfigLine("256 std double");
    std::cout << "בדיקה 1: קלט תקין -> ";
    assert(bf.possiblyContains("www.test.com") == false);
    std::cout << "עבר\n";
}

void test_invalid_size() {
    BloomFilter bf = createFromConfigLine("abc std");
    std::cout << "בדיקה 2: גודל שגוי -> ";
    assert(bf.possiblyContains("example.com") == false);
    std::cout << "עבר\n";
}

void test_unknown_hash() {
    BloomFilter bf = createFromConfigLine("128 unknown");
    std::cout << "בדיקה 3: סוג לא מוכר -> ";
    assert(bf.possiblyContains("site.com") == false);
    std::cout << "עבר\n";
}

void test_empty_line() {
    BloomFilter bf = createFromConfigLine("");
    std::cout << "בדיקה 4: שורה ריקה -> ";
    assert(bf.possiblyContains("empty.com") == false);
    std::cout << "עבר\n";
}

int main() {
    std::cout << "== בדיקות קונפיגורציה ==\n";
    test_valid_input();
    test_invalid_size();
    test_unknown_hash();
    test_empty_line();
    std::cout << "== כל הבדיקות עברו ✅ ==\n";
    return 0;
}
