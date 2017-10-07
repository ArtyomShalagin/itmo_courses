#pragma once

#include <cstddef>

extern "C" size_t word_count_asm_aligned(const char *, size_t);

size_t word_count_naive(const char *str, size_t size);

size_t word_count_asm(const char *str, size_t size);