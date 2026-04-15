cmake_minimum_required(VERSION 3.22.1)
project("localmind")
find_package(OpenMP REQUIRED)
# 1. Define where your static .a files are located
# This assumes you put them in: app/src/main/cpp/libs/arm64-v8a/
set(LIBS_DIR ${CMAKE_CURRENT_SOURCE_DIR}/libs/${ANDROID_ABI})

# 2. Import the "Bricks" (Static Libraries)
# We tell CMake these are already built (IMPORTED) and where to find them.

add_library(llama_static STATIC IMPORTED)
set_target_properties(llama_static PROPERTIES IMPORTED_LOCATION ${LIBS_DIR}/libllama.a)

add_library(ggml_static STATIC IMPORTED)
set_target_properties(ggml_static PROPERTIES IMPORTED_LOCATION ${LIBS_DIR}/libggml.a)

add_library(ggml_base_static STATIC IMPORTED)
set_target_properties(ggml_base_static PROPERTIES IMPORTED_LOCATION ${LIBS_DIR}/libggml-base.a)

add_library(ggml_cpu_static STATIC IMPORTED)
set_target_properties(ggml_cpu_static PROPERTIES IMPORTED_LOCATION ${LIBS_DIR}/libggml-cpu.a)

# 3. Create your Bridge Shared Library
# This is the ONLY .so file your Android app will actually see.
add_library(llama_bridge SHARED llama_bridge.cpp)

# 4. Include Headers
# This tells the compiler to look in the current folder for llama.h, ggml.h, etc.
target_include_directories(llama_bridge PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

# 5. The Final Link
# This "bakes" all the .a files into libllama_bridge.so.
# Order is important: Higher level libraries should come first.
target_link_libraries(llama_bridge
    llama_static
    ggml_static
    ggml_base_static
    ggml_cpu_static
    OpenMP::OpenMP_CXX
    log     # Required for Android logging (e.g., __android_log_print)
    android # Required for native Android APIs
)