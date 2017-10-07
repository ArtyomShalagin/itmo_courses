#pragma once

// this is broken, marks second branch as unreachable code
#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCDFAInspection"

#include <cstdlib>
#include <sys/mman.h>
#include <xmmintrin.h>
#include <iostream>
#include "arg_cnt.h"

namespace {
    void **p = nullptr;
    const int BLOCK_SIZE = 128; // actually 123
    const int NPAGES = 1;
    const int PAGE_SIZE = 4096;
    const int PROT = PROT_EXEC | PROT_READ | PROT_WRITE;
    const int FLAGS = MAP_PRIVATE | MAP_ANON; // ANON instead of ANONYMOUS because of my old software

    void alloc() {
        void *mem = mmap(nullptr, PAGE_SIZE * NPAGES, PROT, FLAGS, -1, 0);
        p = (void **) mem;
        for (size_t i = 0; i < PAGE_SIZE * NPAGES; i += BLOCK_SIZE) {
            auto c = (char *) mem + i;
            *(void **) c = nullptr;
            if (i != 0) {
                *(void **) (c - BLOCK_SIZE) = c;
            }
        }
    }

    void *get_next() {
        if (p == nullptr) {
            alloc();
            if (p == nullptr) {
                return nullptr;
            }
        }
        void *ans = p;
        p = (void **) *p;
        return ans;
    }

    void free_ptr(void *ptr) {
        *(void **) ptr = p;
        p = (void **) ptr;
    }
}

template<typename T>
class trampoline;

template<typename R, typename... Args>
void swap(trampoline<R(Args...)>& lhs, trampoline<R(Args...)>& rhs);

template<typename T, typename... Args>
class trampoline<T(Args...)> {

    const char *shifts[6] = {
            `(movq %rdi, %rsi),
            `(movq %rsi, %rdx),
            `(movq %rdx, %rcx),
            `(movq %rcx, %r8),
            `(movq %r8, %r9),
            `(pushq %r9)
    };

    void add(char *&p, const char *command) {
        for (const char *i = command; *i; i++) {
            *(p++) = *i;
        }
    }

public:

    template<typename F>
    trampoline(F func) : func_obj(new F(std::move(func))), deleter(my_deleter<F>) {
        code = get_next();
        auto *pcode = (char *) code;

        if (args_cnt<Args ...>::INT < 6) {
            for (int i = args_cnt<Args ...>::INT - 1; i >= 0; i--) {
                add(pcode, shifts[i]);
            }
            add(pcode, "\x48\xbf");
            *(void **) pcode = func_obj; // movq ptr to func to rdi
            pcode += 8;
            add(pcode, "\x48\xb8");
            *(void **) pcode = (void *) &do_call<F>; // mov addr of do call to rax
            pcode += 8;
            add(pcode, `(jmp * % rax));
        } else {
            int stack_size = 8 * (args_cnt<Args...>::INT - 5
                                  + std::max(args_cnt<Args...>::SSE - 8, 0));
            add(pcode, `(movq( % rsp), %r11));
            for (ssize_t i = 5; i >= 0; i--) {
                add(pcode, shifts[i]);
            }
            add(pcode, `(movq % rsp, % rax));
            add(pcode, "\x48\x05"); // addq $stack_size, %rax
            *(int32_t *) pcode = stack_size;
            pcode += 4;
            add(pcode, "\x48\x81\xc4");
            *(int32_t *) pcode = 8; // addq $8, %rsp, but for some magical reason
            pcode += 4;             // compiling does not work
            char *label_1 = pcode;
            add(pcode, "\x48\x39\xe0\x74"); // cmpq %rax, %rsp
            char *label_2 = pcode;
            pcode++;

            add(pcode, "\x48\x81\xc4"); // adding 8 to %rsp
            *pcode = 8;
            pcode += 4;
            add(pcode, `(movq( % rsp), %rdi));
            add(pcode, `(movq % rdi, -8( % rsp)));
            add(pcode, "\xeb"); // jump to label1
            *pcode = label_1 - pcode - 1;
            pcode++;

            *label_2 = pcode - label_2 - 1;
            add(pcode, `(movq % r11,( % rsp)));
            add(pcode, "\x48\x81\xec");
            *(int32_t *) pcode = stack_size; // sub stack size from rsp
            pcode += 4;

            add(pcode, "\x48\xbf"); // mov func obj to rdi
            *(void **) pcode = func_obj;
            pcode += 8;

            add(pcode, "\x48\xb8"); // mov do_call to rax
            *(void **) pcode = (void *) &do_call<F>;
            pcode += 8;

            // add(pcode, (call *(%rax)));
            add(pcode, "\xff\xd0"); // call %rax, godbolt does this and it works, WHY?
            // my gdb gives \x10
            //restore
            add(pcode, `(popq % r9));
            add(pcode, "\x4c\x8b\x9c\x24"); // movq (%rsp + stack_size - $8), %r11
            *(int32_t *) pcode = stack_size - 8;
            pcode += 4;

            add(pcode, `(movq % r11,( % rsp)));
            add(pcode, `(retq));
        }
    }

    trampoline(trampoline &&other) {
        func_obj = other.func_obj;
        code = other.code;
        deleter = other.deleter;
        other.func_obj = nullptr;
    }

    trampoline(const trampoline &) = delete;

    template<class TR>
    trampoline &operator=(TR &&func) {
        trampoline tmp(std::move(func));
        ::swap(*this, tmp);
        return *this;
    }

    T (*get() const )(Args... args) {
        return (T(*)(Args... args)) code;
    }

    void swap(trampoline &other) {
        ::swap(*this, other);
    }

    friend void ::swap<>(trampoline &a, trampoline &b);

    ~trampoline() {
        if (func_obj) deleter(func_obj);
        free_ptr(code);
    }

private:
    template<typename F>
    static T do_call(void *obj, Args... args) {
        return (*static_cast<F *>(obj))(std::forward<Args>(args)...);
    }

    template<typename F>
    static void my_deleter(void *func_obj) {
        delete static_cast<F *>(func_obj);
    }

    void *func_obj;
    void *code;

    void (*deleter)(void *);
};

template<typename R, typename... Args>
void swap(trampoline<R(Args...)> &lhs, trampoline<R(Args...)> &rhs) {
    std::swap(lhs.func_obj, rhs.func_obj);
    std::swap(lhs.code, rhs.code);
    std::swap(lhs.deleter, rhs.deleter);
}

#pragma clang diagnostic pop