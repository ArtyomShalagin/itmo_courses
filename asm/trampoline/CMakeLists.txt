project(trampoline)
cmake_minimum_required(VERSION 3.5)
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_FLAGS -g)
set(SOURCE_FILES
        arg_cnt.h
        trampoline.h
        main.cpp)
add_custom_target(flash
        COMMAND python compile.py}
        DEPENDS ${PROJECT_NAME}
        )
add_executable(${PROJECT_NAME} ${SOURCE_FILES})
