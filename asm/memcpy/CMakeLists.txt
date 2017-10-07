cmake_minimum_required(VERSION 3.5)
project(memcpy)
set(CMAKE_CXX_STANDARD 14)
enable_language(C ASM)

set(SOURCE_FILES
        main.cpp
        memcpy_aligned.s
        memcpy_asm.h)
add_executable(memcpy ${SOURCE_FILES})
