    .text
    .globl _word_count_asm_aligned
_word_count_asm_aligned:
    # rdi - str, rsi - len
    # xmm0 - store, xmm1 - cur_cmp, xmm2 - next_cmp, xmm3 - space mask, xmm5 - zeros
    # r10 - result

    pushq %r10

    xorq %r10, %r10
    movdqa (%rdi), %xmm2 # read first chunk from memory
    addq $16, %rdi
    sub $16, %rsi
    pandn %xmm0, %xmm0  # set store to zero
    call .set_space_mask
    call .set_zeros_mask
    pcmpeqb %xmm3, %xmm2

.loop:
    cmpq $16, %rsi
    jb .exit
    movdqa %xmm2, %xmm1 # move next chunk to current chunk
    movdqa (%rdi), %xmm2 # read new chunk from memory to next chunk
    add $16, %rdi
    sub $16, %rsi
    pcmpeqb %xmm3, %xmm2 # in xmm3 ones are on positions where xmm3 had spaces
    movdqa %xmm2, %xmm4
    palignr $1, %xmm1, %xmm4
    pandn %xmm1, %xmm4
    movdqa %xmm5, %xmm6 # copy zeros to xmm6
    psubsb %xmm4, %xmm6
    paddusb %xmm6, %xmm0
    pmovmskb %xmm0, %r11
    cmpq $0, %r11 # check if msk = 0
    jne .flush_buffer
    cmpq $16, %rsi
    jb .flush_buffer
    jmp .loop


.flush_buffer:
    pushq %r8
    pushq %r9
    movdqa %xmm5, %xmm6
    psadbw %xmm0, %xmm6
    movd %xmm6, %r8
    movhlps %xmm6, %xmm6
    movd %xmm6, %r9
    add %r8, %r10
    add %r9, %r10
    movdqa %xmm5, %xmm0
    popq %r9
    popq %r8
    jmp .loop

.exit:
    movq %r10, %rax
    popq %r10
    ret

.set_space_mask: # sets spaces to xmm3 register
    movabsq $0x2020202020202020, %r10 # constant is 8 space chars
    movq %r10, -8(%rsp)
    movq %r10, -16(%rsp)
    movdqu -16(%rsp), %xmm3 # move unaligned here so no problems
    xorq %r10, %r10
    ret

.set_zeros_mask: # sets zeros to xmm5 register
    movq $0, -8(%rsp)
    movq $0, -16(%rsp)
    movdqu -16(%rsp), %xmm5
    ret

