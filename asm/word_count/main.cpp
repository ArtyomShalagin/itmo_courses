#include <cstring>
#include <cstdio>
#include <string>
#include <cstdlib>
#include <random>
#include <chrono>
#include <iostream>
#include "word_count_asm.h"

template<typename T>
void sink(T const& t) {
    volatile T sinkhole = t;
}

double rand_double() {
    static std::uniform_real_distribution<double> unif(0, 1);
    static std::default_random_engine engine;
    return unif(engine);
}

char *rand_string(size_t len, double letters_spaces_ratio) {
    char *data = new char[len + 1];
    data[len] = '\0';

    for (size_t i = 0; i < len; i++) {
        bool is_space = rand_double() > letters_spaces_ratio;
        data[i] = is_space ? ' ' : (char) ('a' + std::rand() % 26);
    }
    return data;
}

bool assert(char *str, size_t res_naive, size_t res_asm) {
    if (res_naive != res_asm) {
        printf("error: str:\n\"%s\"\ncorrect result = %zu, asm result = %zu\n\n", str, res_naive, res_asm);
        return false;
    }
    return true;
}

void test_brute(const size_t n, const size_t len) {
    printf("running brute test: n = %zu, len = %zu\n", n, len);
    srand(239566);
    size_t errors = 0;
    for (double ratio = 0.1; ratio < 1; ratio += 0.1) {
        for (size_t i = 0; i < n; i++) {
            if (i % 100 == 0) {
                printf("\rratio = %f, cnt = %zu", ratio, i);
                std::cout << std::flush;
            }
            char *str = rand_string(len, ratio);
            if (!assert(str, word_count_naive(str, len), word_count_asm(str, len))) {
                errors++;
            }
            delete[] str;
        }
    }
    printf("\r                                     \r");
    printf("done, errors: %zu\n", errors);
}

void test_speed(const size_t n, const size_t len) {
    printf("running speed test: n = %zu, len = %zu\n", n, len);
    typedef std::chrono::duration<double> duration;
    typedef std::chrono::time_point<std::chrono::system_clock> time_point;
    duration dur_naive = std::chrono::duration_values<duration>::zero();
    duration dur_asm = std::chrono::duration_values<duration>::zero();
    for (size_t i = 0; i < n; i++) {
        if (i % 100 == 0) {
            printf("\rcnt = %zu", i);
            std::cout << std::flush;
        }
        time_point start;
        char *str = rand_string(len, 0.7);
        start = std::chrono::system_clock::now();
        sink(word_count_naive(str, len));
        dur_naive += std::chrono::system_clock::now() - start;
        start = std::chrono::system_clock::now();
        sink(word_count_asm(str, len));
        dur_asm += std::chrono::system_clock::now() - start;
        delete[] str;
    }
    printf("\n");
    printf("naive time = %s\n", std::to_string(dur_naive.count()).c_str());
    printf("asm time = %s\n", std::to_string(dur_asm.count()).c_str());
}

void test_manual() {
    const char *str = "qenzkcposkjnqmbzosqsrcfas amuklvfjyfsgfpr nmu  n kqfeckau   rjv bww dpknjedydsbldzxdrrlhl czkm mus ";
    size_t x1 = word_count_naive(str, strlen(str));
    size_t x2 = word_count_asm(str, strlen(str));
    printf("%zu\n", x1);
    printf("%zu\n", x2);
}

int main() {
    test_brute(100000, 1000);
    test_brute(500, 100000);
    test_speed(1000, 500000);

//    test_manual();
}