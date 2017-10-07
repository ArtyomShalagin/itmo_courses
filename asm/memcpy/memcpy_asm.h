#pragma once

const size_t block = 16;

extern "C" void memcpy_asm_aligned(void *, const void *, size_t);

inline void align(void *dst, const void *src, size_t size) {
    for (size_t i = 0; i < size; i++) {
        ((char*) dst)[i] = ((char*) src)[i];
    }
}

inline void memcpy_asm(void *dst, const void *src, size_t size) {
    int offset = (block - ((size_t) dst % block)) % block;
    size -= offset;
    align(dst, src, offset);
    dst = (char*) dst + offset;
    src = (char*) src + offset;
    memcpy_asm_aligned(dst, src, size);
    size_t copied = size - size % block;
    dst = (char*) dst + copied;
    src = (char*) src + copied;
    align(dst, src, size % block);
}