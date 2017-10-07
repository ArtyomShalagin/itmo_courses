#pragma once

template<typename ... Args>
struct args_cnt;

template<>
struct args_cnt<> {
    static const int INT = 0;
    static const int SSE = 0;
};

template<typename T, typename... Args>
struct args_cnt<T, Args...> {
    static const int INT = args_cnt<Args ...>::INT + 1;
    static const int SSE = args_cnt<Args ...>::SSE;
};

template<typename T, typename... Args>
struct args_cnt<T*, Args...> {
    static const int INT = args_cnt<Args ...>::INT + 1;
    static const int SSE = args_cnt<Args ...>::SSE;
};

template<typename... Args>
struct args_cnt<char, Args...> {
    static const int INT = args_cnt<Args ...>::INT + 1;
    static const int SSE = args_cnt<Args ...>::SSE;
};

template<typename... Args>
struct args_cnt<short, Args...> {
    static const int INT = args_cnt<Args ...>::INT + 1;
    static const int SSE = args_cnt<Args ...>::SSE;
};

template<typename... Args>
struct args_cnt<int, Args...> {
    static const int INT = args_cnt<Args ...>::INT + 1;
    static const int SSE = args_cnt<Args ...>::SSE;
};

template<typename... Args>
struct args_cnt<long, Args...> {
    static const int INT = args_cnt<Args ...>::INT + 1;
    static const int SSE = args_cnt<Args ...>::SSE;
};

template<typename... Args>
struct args_cnt<long long, Args...> {
    static const int INT = args_cnt<Args ...>::INT + 1;
    static const int SSE = args_cnt<Args ...>::SSE;
};

template<typename... Args>
struct args_cnt<double, Args...> {
    static const int INT = args_cnt<Args ...>::INT;
    static const int SSE = args_cnt<Args ...>::SSE + 1;
};

template<typename... Args>
struct args_cnt<float, Args...> {
    static const int INT = args_cnt<Args ...>::INT;
    static const int SSE = args_cnt<Args ...>::SSE + 1;
};

template<typename... Args>
struct args_cnt<__m64, Args...> {
    static const int INT = args_cnt<Args ...>::INT;
    static const int SSE = args_cnt<Args ...>::SSE + 1;
};

