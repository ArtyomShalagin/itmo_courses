#include <stdio.h>
#include <cstring>
#include <chrono>
#include <unistd.h>
#include "memcpy_asm.h"

void memcpy_naive(void *dst, const void *src, size_t size) {
	char* mdst = (char*) dst;
	char const* msrc = (char*) src;
	for (size_t i = 0; i < size; i++) {
		mdst[i] = msrc[i];
	}
}

char *generate(size_t len) {
	char *res = new char[len];
	for (size_t i = 0; i < len; i++) {
		res[i] = 'a' + i % 26;
	}
	return res;
}

int main() {
	size_t n = 1000000000;
	const char *src = generate(n);
	char *dst = new char[n];

	typedef std::chrono::time_point<std::chrono::system_clock> time_point;
	typedef std::chrono::duration<int64_t, std::ratio<1l, 1000000000l>> timeout_t;
	typedef std::chrono::system_clock clock;

	time_point start = clock::now();
	memcpy_asm(dst, src, strlen(src));
	timeout_t time = clock::now() - start;
	printf("copied in %lld ms\n", time.count() / 1000000);
	if (strcmp(src, dst) == 0) {
		printf("%s\n", "ok");
	} else {
		printf("%s\n", "ne ok");
	}

	delete[] src;
	delete[] dst;
}