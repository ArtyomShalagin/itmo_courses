	.text
	.globl _memcpy_asm_aligned
_memcpy_asm_aligned:
	# rdi - dst, rsi -- src, rdx -- len, ecx, r8d  
.loop:
	cmpq 		$16, %rdx
	jb 			.exit
	movdqa 		(%rsi), %xmm0	# aligned double quadword
	movntdq 	%xmm0, (%rdi)	# store double quadword using non-temporal hint
	add			$16, %rdi
	add			$16, %rsi
	sub			$16, %rdx
	jmp 		.loop
.exit:
	ret
