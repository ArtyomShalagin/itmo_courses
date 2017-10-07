cmake_minimum_required(VERSION 3.5)
project(word_count)
set(CMAKE_CXX_STANDARD 14)
# set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O0")
# set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -O0")
enable_language(C ASM)

set(SOURCE_FILES
        main.cpp
        word_count_aligned.s
        word_count_asm.cpp
        word_count_asm.h)
add_executable(word_count ${SOURCE_FILES})