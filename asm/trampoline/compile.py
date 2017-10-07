# coding=utf-8
from subprocess import call

code_templ = '    .text\n_code:\n    {}\n'

def command2bytes(cmd_str):
    file = open('test.s', 'w')
    file.write(code_templ.format(cmd_str))
    file.close()
    call(['gcc', '-c', 'test.s'])
    file = open('test.o', 'rb')
    data = file.read()
    data = [int(i) for i in data]

    offset = 8 * 18 * 2
    command = []
    for i in range(offset, len(data)):
        if data[i] == 1 or data[i] == 0:
            break
        command.append(data[i])
    return command

file = open('trampoline_source.h', 'r')
text = file.read()
processed = ''
i = 0
while i < len(text):
    if text[i] == '`':
        i += 2
        cmd = ''
        balance = 0
        while text[i] != ')' or text[i] == ')' and balance != 0:
            if text[i] == '(':
                balance += 1
            if text[i] == ')':
                balance -= 1
            cmd += text[i]
            i += 1
        bytes = command2bytes(cmd)
        processed += '"'
        for b in bytes:
            processed += '\\x' + '{:02x}'.format(b)
        processed += '"'
    else:
        processed += text[i]
    i += 1

outfile = open('trampoline.h', 'w')
outfile.write(processed)
outfile.close()
