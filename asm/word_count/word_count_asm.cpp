#include "word_count_asm.h"
#include <cstdio>
#include <cstring>
#include <cstdint>
#include <string>

//bool LOG_ENABLED = true;
bool LOG_ENABLED = false;

void LOG(std::string s) {
    if (LOG_ENABLED) {
        printf("%s", s.c_str());
    }
}

size_t word_count_naive(const char *str, size_t size) {
    uint32_t result = 0;
    bool prev_is_space = true;
    for (size_t i = 0; i < size; i++) {
        if (str[i] != ' ' && prev_is_space) {
            result++;
            prev_is_space = false;
        } else if (str[i] == ' ') {
            prev_is_space = true;
        }
    }
    return result;
}

// important: in assembly part a word is a word + next space
size_t word_count_asm(const char *str, size_t size) {
    if (size < 64) {
        return word_count_naive(str, size);
    }
    LOG("counting words in string: \"");
    LOG(str);
    LOG("\"\n");
    int64_t result = 0;
    if ((size_t) str % 16 != 0) {
        size_t offset = 16 - (size_t) str % 16;
        LOG("address % 16 = 0, offset = " + std::to_string(offset) + "\n");
        size_t starting_result = word_count_naive(str, offset);
        LOG("words in unaligned prefix: " + std::to_string(starting_result) + "\n");
        result += starting_result;
        if (str[offset - 1] == ' ' && str[offset] == ' ') { // "lala | lala"
            result--; // first space does not follow a word
        } else if (str[offset - 1] == ' ' && str[offset] != ' ') { // "lala |lala"

        } else if (str[offset - 1] != ' ' && str[offset] == ' ') { // "lala| lala"
            result--; // first space does not follow a word
        } else if (str[offset - 1] != ' ' && str[offset] != ' ') { // "lala|lala"
            result--; // the word was already counted in prefix
        }
        str = (char *) ((size_t) str + offset);
        LOG("cutted string, now str = \"");
        LOG(str);
        LOG("\"\n");
    } else {
        if (str[0] == ' ') {
            result--; // first space in the line does not follow a word
        }
    }
    size_t len = strlen(str);
    size_t asm_result = word_count_asm_aligned(str, len);
    LOG("asm len = " + std::to_string(asm_result) + "\n");
    result += asm_result;
    size_t suffix_len = 16 + len % 16;
    if (suffix_len != 0) {
        LOG("unaligned suffix length = " + std::to_string(suffix_len) + "\n");
        size_t main_len = len - suffix_len;
        size_t ending_result = word_count_naive(str + main_len, suffix_len);
        LOG("unaligned suffix: \"");
        LOG(str + main_len);
        LOG("\"\n");
        LOG("words in unaligned suffix: " + std::to_string(ending_result) + "\n");
        result += ending_result;
        if (str[main_len - 1] == ' ' && str[main_len] == ' ') { // "lala | lala"
            result++; // asm looked up the first char in the next chunk, it was space, word didn't count
        } else if (str[main_len - 1] == ' ' && str[main_len] != ' ') { // "lala |lala"

        } else if (str[main_len - 1] != ' ' && str[main_len] == ' ') { // "lala| lala"
            result++; // asm looked up the first char in the next chunk, it was space, word didn't count
        } else if (str[main_len - 1] != ' ' && str[main_len] != ' ') { // "lala|lala"

        }
    }
    if (result < 0) {
        LOG("CRITICAL: result < 0\n");
        return 0;
    }
    return static_cast<size_t>(result);
}