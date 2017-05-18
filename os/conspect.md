# Операционные системы
# Важно
Если вы решили написать сюда что-нибудь (ну вдруг), отметьтесь, пожалуйста, [тут](https://docs.google.com/spreadsheets/d/1AJEm1yy9jE40z0mLnJmQ2Kcot9vmSdlvfLRWaJwZbLk/edit?usp=sharing), чтобы избежать коллизий. 

# А где читать?
* [sandwwraith's git](https://github.com/sandwwraith/os2016-conspect/)
* [nikitaevg's conspects](https://github.com/nikitaevg/Lections/tree/master/os-course/fourth_sem)
* [awesomecool git about system programming](https://github.com/angrave/SystemProgramming/wiki)
* [wiki osdev (for some specific things)](http://wiki.osdev.org/Main_Page)
* [Roman's conspects](https://drive.google.com/file/d/0B2pQVaWeOLJoRFF1MEFBLVBGRlU/view?usp=sharing)

# А что тут есть?
1. [Память](#memory)
2. [Файловые системы](#file_systems)
3. [Процессы и потоки](#processes_and_threads)
4. [Пользователи](#users)
5. [Сигналы](#signals)
6. [Job control](#job_control)
7. [Терминалы](#terminals)
8. [Сеть](#network)
9. [Ввод-вывод](#input_output)
10. [Загрузка](#loading)
11. [Объектные файлы](#object_files)
12. [Динамическая линковка](#dynamic_linking)
13. [ОС в железе](#os_in_hardware)
14. [Аллокация](#allocation)

# OS notes

<a name="memory"></a>
## Память 
Верим, что про виртуальную и физическую память, страничную адресацию etc. мы знаем.

**MMU** - memory management unit. Отвечает за маппинг виртуальной памяти в физическую, memory protection etc. Нужен, потому что иначе все работает очень медленно, нужна хардверная поддержка этого. Данные MMU - это часть контекста исполнения процесса (там еще регистры, credentials типа PID, PPID, SID, UID...). При переключении контекста ядро загружает данные в MMU.

**Page table / page directory** - как внутри устроен MMU. На 32-битном x86 MMU мапит память через 2 таблицы, каждая по 1024 4-байтных элемента. В page directory каждый элемент указывает на page table, в page table каждый элемент указывает на физический адрес, виртуальный адрес определяется по сдвигу в обеих таблицах. Вхождения в обеих таблицах -- адрес и куча флагов. Очень подробно на [wiki osdev](http://wiki.osdev.org/Paging)

Если процесс пытается получить страницу виртуальной памяти, на которую не замаплена никакая физическая, ему кидается page fault, перед этим процессор пушит на стек код ошибки (но там только первые 3 бита используются -- present, r/w, user/supervisor). 

**TLB** - translation lookaside buffer. Кеширует физические адреса, чтобы не скакать по MMU.

**Swap** - ну понятно, когда мало оперативной памяти, можем страницы, которые давно не использовались, скинуть на жесткий диск, а в освободившееся место еще что-нибудь замапить. Для этого используют access bit, он ставится 1, когда используется страница, иногда сбрасывается, если он 0, то можно скинуть на диск. При page fault из MMU ядро сначала посмотрит, нет ли страницы в свопе.

**OOM Killer** - linux при недостатке памяти должен выбрать, какой процесс убивать. Можно сделать так, чтобы процесс не убивался oom killer-ом - флаг `OOM_DISABLE` в `/proc/<pid>/oomadj`. Есть функция `badness()`, которая вызывается, чтобы понять, угадайте что. Подробнее на [man page](https://linux-mm.org/OOM_Killer). 

**Memory overcommit** -- фича, которая позволяет виртуальной машине использовать больше памяти, чем есть физически. Выглядит опасно, потому что есть физическая память закончится, то все сломается, но обычно работает нормально, потому что реально используется намного меньше памяти, чем было аллоцировано. Вообще обычно используется в виртуализации, когда надо выделить гигабайт на виртуальную машину, но использовать она может сильно меньше.

<a name="file_systems"></a>
## Файловые системы
В linux нет разделения на разные устройства (в отличие от Windows), все находится в `/`. `INode` - структура данных, которая описывает объект в файловой системе. Директории - это списки имен, приписанных к `INode`-ам. 

	data FS = File data
		| Dir (Map String FS)
		| Mounted FSID
	data Inode = Inode {
		atime :: Time
		mtime :: Time 
		ctime :: Time 
		uid   :: UID
		gid   :: GID 
		modes :: Modes
		data  :: [Byte]
	}
	type MountNamespace = [ (Prefix, FSID, Inode) ]

`MountNamespace` разные для разных процессов.

Полезные системные вызовы (и соответствующие команды, собственно):

* `int mount(src, trg)` - смонтировать новое поддерево
* `int umount(trg)`
* `chroot new_root proc` - изменить корень. `proc` - процесс, который нужно запустить в новом корне. Например, `chroot /mnt /bin/bash`

Inode разная для разных систем. Абсолютная адресация по Inode: 

`type AInode = (FSID, Inode)`

~~~ c++
const char msg [] = "Hello";
write (1, msg, sizeof(msg));
~~~

	type FD = Int
	type FDState = Int
	type FDTable = Map FD (AInode, FDState)

`FDState` - что-то вроде offset-а.

~~~ c++
int open(const char * path, int flags, mode_t mode);
int close(int file_descriptor);
~~~

Перенаправление потоков вывода: `make > /dev/null`, `make > log.txt`. По сути хотим подменить файловый дескриптор, который есть сейчас, файловым дескриптором файла, в который хотим перенаправлять. Системные вызовы:

~~~ c++
int dup(int oldfd);
int dup2(int oldfd, int newfd);
~~~

Например:

~~~ c++
int fd = open("log.txt", ...);
close(fd);
dup(fd);
~~~

Это эквивалентно

~~~ c++
dup2(fd, 1);
~~~

	data Modes = Modes {
		x		:: X
		owner 	:: RWX // read write execute
		group	:: RWX
		al1  	:: RWX
	}
	
	data X = X {
		setuid 	:: Bool
		getuid 	:: Bool
		stickybit :: Bool
	}
	
stickybit - раньше "не выгружай меня как можно дольше из памяти", теперь чтобы указать, что файлы из директории может удалять только владелец. Обычно такие права имеет `tmp`, потому что туда могут скидываться файлы со всей системы, но хочется, чтобы нельзя было загадить чужие файлы, поэтому у `tmp` типично права 1777 - кто угодно читать, писать и исполнять, но удалять чужие файлы нельзя.

Загадка про `sudo` - ему надо узнать, правильный ли пароль рута я ввел, но он не может этого сделать без повышенных привелегий, которые мы и пытаемся дать. Что-то пошло не так?

У `sudo` изначально есть повышенные привилегии. А как это происходит? `sudo` находится в `/usr/sbin/` с правами `u rwxr-xr-x`, `u` - это `setuid`

* `setuid` - процесс запускается с правами владельца файла, а не запускающего
* `setgid` - то же самое для групп

**Umask** - при создании файла хочется определить, что ему можно делать. Такая маска называется ***file mode creation mask***. Каждый бит определяет, будет ли дано какое-то разрешение для нового созданного файла. Неочевидно: если бит 1, то соответствующего разрешения у нового файла **не** будет, если 0, то это разрешение будет определено программой/системой. У каждого процесса своя маска, и процесс может ее менять системным вызовом. `umask` наследуется при `fork`, хранится в `proc_info`. 

`umask` - команда для смены этой маски. 

Cистемные вызовы для управления правами файла:

~~~ c++
int chmod(path, mode_t);
int chown(path, uid_t, gid_t)
~~~

**Символьные ссылки** или `soft links` - вид файла, который указывает на другой файл. В отличие от `hard link`, не хранит данные файла. Симлинк просто хранит имя соответствущего `INode`. Когда система резолвит симлинк, она находит этот `INode` и дальше просто его использует. Поэтому, например, если есть большой файл и симлинг на него, можно начать читать этот файл, потом удалить симлинк, и при этом ничего не сломается, потому что `INode` уже найден и всё хорошо.

<a name="processes_and_threads"></a>
## Процессы и потоки
> Process is an instance of a computer program that is being executed

Идентификаторы процесса:

* pid: The is the process ID (PID)
* ppid: The PID of the parent process (the process that spawned the current one). For example, if you run ruby test.rb in a bash shell, PPID in that process would be the PID of Bash.
* uid: The UNIX ID of the user the process is running under.
* euid: The effective user ID that the process is running under. The EUID determines what a program is allowed to do, based on what the user with this UID is allowed to do. Typically the same as uid, but can be different with commands like sudo.
* gid: The UNIX group ID the program is running under.
* egid: Like euid, but for groups.

**init** process - в unix это первый процесс, который запускается при запуске системы. Это демон, и он продолжает работать до завершения работы системы. При убивании процесса его дети подвешиваются к init (кажется).

Послать сигнал процессу:

~~~ cpp
int kill(pid_t pid, int sig);
~~~

Создание нового процесса - системный вызов `fork()`. Возвращает pid ребенка в родителе и 0 в ребенке, чтобы можно было их различать из кода. 

~~~ cpp
int main() {
	if (!fork()) // если мы ребенок
		return 42;
	sleep(10);
	return 0;
}
~~~

Проблема - непонятно, как можно посмотреть код возврата ребенка, потому можем посмотреть с помощью $? только код возврата последнего процесса. Решение - zombie процессы, процессы, которые уже завершились, "осталось только собрать код возврата и закопать". 

	data Zombie = Zombie {
		ppid :: PID
		pid  :: PID
		ret  :: RetCode
		stat :: Statistic
	}
	
Какие бывают состояния у процесса:

* `S` - `sleep`
* `R` - `run`
* `Z` - `zombie`
* `T` - `traced`
* `D` - `disk sleep`

`ps -o pid,ppid,state,comm` - посмотреть `ps` в определенном формате

Системный вызов `waitpid`:

~~~ cpp
pid_t waitpid(pid_t pid, int *wstatus, int options);
pid_t wait(int *wstatus) === waitpid(-1, int * wstatus);
~~~

> The `wait()` function suspends execution of its calling process until `stat_loc` information is available for a terminated child process, or a signal is received.  On return from a successful `wait()` call, the `stat_loc` area contains termination information about the process that exited as defined below.
> The `wait4()` call provides a more general interface for programs that need to wait for certain child processes, that need resource utilization statistics accumulated by child processes, or that require options. The other wait functions are implemented using `wait4()`.
> If `rusage` is non-zero, a summary of the resources used by the terminated process and all its children is returned (this information is currently not available for stopped processes).
> The `waitpid()` call is identical to `wait4()` with an `rusage` value of zero.

**Контекст процесса** - MMU, регистры и `proc_info`. Неявные данные, не видные для процесса, которые позволяют ему нормально работать.

	proc_info {
		pid, uid, ppid, gid, ...
	} // credentials

**fork bomb**:

~~~ cpp
while (1)
	if (fork())
		return 0;
~~~

Количество разных pid ограничено, зомби займут все возможные pid и всё плохо, всё зависло. Можно побороться так: `exec` будет запускать в какой-то из текущих процессов, а не в новом.

**process vs thread** - в `proc_info` есть всякие pid, ppid, fdtable, regs etc. Часть из них - числа и влезают в регистр. Большие же структуры (типа fdtable) хранятся по указателям. Какие-то из них наследуются при `fork`-e, но, например, не общая память. Если же мы хотим расшарить больше, есть системный вызов `clone`. Например, если мы передадим наследнику всё, кроме регистров, у нас получится не новый процесс, а новый тред.

> 1. Threads share the address space of the process that created it; processes have their own address space.
> 1. Threads have direct access to the data segment of its process; processes have their own copy of the data segment of the parent process.
> 1. Threads can directly communicate with other threads of its process; processes must use interprocess communication to communicate with sibling processes.
> 1. Threads have almost no overhead; processes have considerable overhead.
> 1. New threads are easily created; new processes require duplication of the parent process.
> 1. Threads can exercise considerable control over threads of the same process; processes can only exercise control over child processes.
> 1. Changes to the main thread (cancellation, priority change, etc.) may affect the behavior of the other threads of the process; changes to the parent process do not affect child processes.

> One way of looking at a process is that it is a way to group related resources together. A process has an address space containing program text and data, as well as other resources. These resource may include open files, child processes, pending alarms, signal handlers, accounting information, and more. By putting them together in the form of a process, they can be managed more easily. The other concept a process has is a thread of execution, usually shortened to just thread. The thread has a program counter that keeps track of which instruc­tion to execute next. It has registers, which hold its current working variables. It has a stack, which contains the execution history, with one frame for each proce­dure called but not yet returned from. Although a thread must execute in some process, the thread and its process are different concepts and can be treated sepa­rately. Processes are used to group resources together; threads are the entities scheduled for execution on the CPU.

Per process items             | Per thread items
------------------------------|-----------------
Address space                 | Program counter
Global variables              | Registers
Open files                    | Stack
Child processes               | State
Pending alarms                |
Signals and signal handlers   |
Accounting information        |

pthreads - сокращение от POSIX Threads. Можно юзать из С.

При создании нового треда все страницы памяти из стека становятся copy on write (т.е. создается новый стек), если пользователь не аллоцировал его сам.

В софте можно делать либо userspace threads, либо kernel threads (либо комбинация).

**process namespaces** - очень полезная фича в linux, позволяет делать что-то вроде песочницы. Родитель может создать новый процесс, отделив его в новый namespace. Тогда ребенок будет думать, что он тут корень, и не будет видеть ничего выше. Родитель будет видеть все процессы ниже. Это делается вызовом `clone()` с флагом `CLONE_NEWPID`. Важно, что теперь у процесса может быть несколько pid, в каждом namespace свой. В linux также есть networking namespaces, флаг `CLONE_NEWNET`, и mount namespaces, `CLONE_NEWNS`, по действию как `chroot`.   Init может установить связь между родительским процессом и ребенком, для этого могут использоваться unix сокеты / TCP. 

[Хорошая статья про namespaces](https://www.toptal.com/linux/separation-anxiety-isolating-your-system-with-linux-namespaces)

<a name="users"></a>
## Пользователи
**IPC** - interprocess communication. В linux всё файл - все абстракции реализуются через файловые дескрипторы. 

* pipe (unnamed) - 2 fd, в один валится, из другого вываливается. Если закрывают все fd на один из концов, в другой прилетает EOF(в читающий) или EPIPE (в пишущий). Например, `cmd1 | cmd2` - output `cmd1` -> input `cmd2`
* fifo (named pipe) - 1 fd, один процесс открывает на чтение, другой на запись. Чтобы работать с fifo, нужно чтобы оба конца были открыты.

Системные вызовы:

~~~ cpp
int pipe(int pipefd[2]) // pipefd - файловые дескрипторы двух концов пайпа
int mkfifo(const char *pathname, mode_t mode);
~~~

Общая схема: `pipefd[1]` -> `buf` -> `pipefd[0]`

Как стрелять в ноги:

	echo -n x | cat - pipe1 > pipe2 &
	cat <pipe2 > pipe1

x будет в бесконечном цикле копироваться через пайп.

Флаги:

* `O_CLOEXEC` - чтобы файловый дескриптор закрывался при окончании процесса

**Права доступа**: идентификация происходит по uid, их можно найти в `/etc/passwd`. Различают real UID и effective UID. Проблема: дефолтный пользователь запускает что-то под `sudo`, получается процесс с большими правами, и пользователь не имеет права его убивать. Решение: команда `kill` смотрит на real UID.

> So, the real user id is who you really are (the one who owns the process), and the effective user id is what the operating system looks at to make a decision whether or not you are allowed to do something (most of the time, there are some exceptions).

Системные вызовы:

~~~ cpp
int setuid(uid_t uid);
int setreuid(uid_t ruid, uid_t euid);
~~~

**shared memory** - родительский процесс и ребенок работают в разных адресных пространствах, но можно сделать shared memory segment, который можно аллоцировать и прикрепить к нескольким адресным пространствам, тогда из все этих пространств будет туда доступ. Бойся race condition-ов!

В POSIX есть стандартное API для этого. `shm_open` - создается ссылка на блок памяти из (виртуальной) файловой системы `/tmp/shm`. Есть разные функции для работы с shared memory: 

* `shmat` - прикрепить shared memory к адресному пространству
* `shmctl` - deallocate
* `shmdt` - открепить от адресного пространства
* `shmget` - аллоцировать shared memory

[Хорошая pdf про shared memory](http://home.deib.polimi.it/fornacia/lib/exe/fetch.php?media=teaching:piatt_sw_rete_polimi:unix-shm.pdf)

**memory mapped files** - есть глобально два параметра при использовании `mmap`: shared/private и file/anonymous. Типичное использование `mmap` для file mapping:

1. `open()` file to get a file descriptor.
1. `fstat()` the file to get the size from the file descriptor data structure.
1. `mmap()` the file using the file descriptor returned from open(2).
1. `close()` the file descriptor.
1. do whatever to the memory mapped file. 

Когда `mmap` private, изменения не меняют сам файл, получается такая in-memory копия. В случае shared файл меняется. shared mapped files могут использоваться для IPC. 

> You would use a memory mapped file for IPC instead of a shared memory segment if you need the persistence of the file

(но я не очень понял, что в этом контексте значит persistence)

**Anonymous mapping** - работает вообще без файла. Для этого нужно передать флаг `MAP_ANONYMOUS`, тогда 4 и 5 аргументы `mmap` игнорируются. Shared anonynous memory можно использовать для IPC, но не особо понятно зачем.

Забавный факт - anonymous mapping гарантирует, что память будет занулена, и его можно использовать вместо `malloc` + `memset`. 

<a name="signals"></a>
## Сигналы
Чтобы взаимодействовать с пайпом, надо, чтобы оба процесса заранее знали об этом. Сигналы позволяют "неожиданно" обратиться к программе. Посылаются они системным вызовом `kill` (не перепутайте с одноименной утилитой). Обновление `proc_info` - теперь там есть маска `sigpending`. 

	sigpending : vector<bool> 
	sigmask : vector<bool>
	proc_info {
		pid, uid, ppid, gid, sigpending, 
		sigmask, sighandler // контекст исполнения
	} // credentials

У каждого типа сигнала есть номер и соответствующее ему имя. Бывают сигналы:

* `SIGINT` - interrupt, `ctrl`+`C`, номер 2
* `SIGKILL` - `ctrl`+`\`, номер 9
* `SIGSTOP` - `ctrl`+`Z`, приостановить, процесс будет лежать в списке заблокированных, как будто совершен системный вызов, пока не придет `SIGCONT`
* `SIGCONT` - продолжить исполнение после `sigstop`
* `SIGTSTP` - "мягкий" `SIGSTOP`
* `SIGSEGV` - sig fault
* `SIGTERM` - terminate
* `SIGCHLD`
* `SIGWINCH` - window change, изменение размера
* ещё много

Системный вызов `kill`:

~~~ cpp
int kill(pid_t pid, int sig);
~~~

Битовая маска показывает, какие сигналы пришли в процесс, но ещё не обработаны. Из этого следует, что мы не запоминаем порядок и не можем одновременно "держать" два одинаковых сигнала. Ещё в `proc_info`:

* `sighandlers` - массив указателей на функции-обработчики соответствующих сигналов
* `sigmask` - поле позволяет игнорировать сигналы по той же маске

	kernel() {
	    ...
	    p,args = pop_process()
	    for i = 0..signal:
			if sigpend[i] and !sigmask[i]
				sighandler[i]()
	    ...
	}

`man 2 signals` - deprecated, перенаправляет на `man 2 sigaction`

Можно сказать, что делать при получении определенного сигнала:

~~~ cpp
int sigaction(int signum, const struct sigaction *act, struct sigaction *oldact);
~~~

`SIGKILL`, `SIGSTOP` - необычные сигналы. Они не доходят до процесса, а обрабатываются ядром. По `SIGKILL` мы просто выкидываем процесс из списка. `SIGTERM` - аналог `SIGKILL`, только доходит до приложения, _намекая_ ему, что время умирать. `SIGSTOP` - приостановить процесс, он будет лежать в списке заблокированных, как будто совершен системный вызов, пока не придет `SIGCONT`.

Что происходит, когда во время выполнения обработки сигнала приходит другой? Очень жаль. Функция-обработчик должна обладать **reentrancy** - не затрагивать внешние данные, поддерживать инварианты всегда внутри себя, а не только при входе и выходе из неё. Короче говоря, обработчик сигнала может ВНЕЗАПНО вызваться во время своего же выполнения.

Если сигнал пришёл во время системного вызова, они могут прерваться. Например `read()` по сигналу возвращает -1 и выставляет код ошибки `EINTR`. Сигналы приходят и для незаблокированных, и для заблокированных процессов. После обработки процесс "разблокируется". Исключением является `SIGSTOP` - после него прилетает только `SIGCONT`.

**sigmask** - сет сигналов, которые мы блокируем. У каждого процесса своя signal mask, при создании нового процесса наследуется. 

В C ([man sigprocmask](https://www.gnu.org/software/libc/manual/html_node/Process-Signal-Mask.html)):

~~~ cpp
int sigprocmask (int how, const sigset_t *restrict set, sigset_t *restrict oldset)
~~~

[Pending Signals and Signal Masks](https://github.com/angrave/SystemProgramming/wiki/Signals,-Part-2:-Pending-Signals-and-Signal-Masks)

**Realtime сигналы** - расширение стандартных сигналов. Было грустно, потому что нет никаких гарантий порядка, с realtime сигналами порядок сохранятся, и можно послать несколько сигналов. Могут таскать за собой данные (небольшие, размером с регистр). В `proc_info` появляется поле со списком (очередь) из таких сигналов.

<a name="job_control"></a>
## Job control
**Группы процессов** - bash создает новую группу процессов для каждой команды, при этом запущенные через пайп процессы будут в одной группе: `cmd1 | cmd2 &` - `cmd1` и `cmd2` в одной группе процессов. При этом bash всегда в отдельной группе процессов, сам создает новые, а после этого запущенные из процесса другие процессы будут в одной группе

`kill -s SIGKILL -2000` - убьет всю группу процессов с корнем с PID 2000

Создание группы процессов:

~~~ cpp
pid_t getpgid(pid_t pid);
int setpgid(pid_t pid, pid_t pgid);
~~~

Менять группу процесса можно только до `exec()`, процесс не ожидает, что у него внезапно поменяется группа. При этом это надо делать и в ребенке, и в родителе.

**Сессии** - объединяем задачи. Если два процесса находятся в одной и той же группе, то они находятся в одной и той же сессии. Можно считать (не совсем правда), что сессия захватывает все процессы одного shell-а.

Один из способов создать новую группу:

~~~ cpp
pid_t setsid(pid_t pid);
pid_t setsid(void);
~~~

**Лидер группы**: номер группы - это номер одного из процессов в этой группе, который называется лидером группы. Проблема: был родитель с pid=1000, был ребенок. Родитель сделал `setsid`, создалась новая сессия и новая группа, которая тоже получила номер 1000. Теперь две группы с одинаковыми номерами - в которой ребенок и в которой родитель. Что делать? Запретить! Лидер группы не может делать `setsid`.

Виден новый способ сделать так, чтобы закончились pid (кроме fork бомбы). Был родитель с pid 1000 и ребенок. Родитель завершился, а ребенок теперь имеет pid 1001 и pgid 1000. ОС не может теперь создать процесс с id 1000, потому что в таком случае он объединится в одну группу с ребенком, это плохо.

Кроме лидера группы есть еще лидер сессии.

**foreground/background groups** - процессы можно запускать в background и foreground. Когда запускается foreground процесс, shell дает ему доступ к контролирующему его терминалу системным вызовом `tcsetpgrp`. После этого shell ждёт завершения/остановки этой группы процессов. Foreground процесс может оставить терминал в странном состоянии, поэтому ему надо уметь восстанавливаться. Если процесс background, то надо просто его запустить и продолжить принимать команды.

Сигналы:

* `SIGHUP` - signal hang up. Посылается процессу, когда умирает контролирующий его терминал. Когда умирает родитель, пытаемся завершить всех детей, потому что сироты - плохо (только unintentionally orphaned, то есть родитель завершился или упал)
* `SIGTTIN`, `SIGTTOU` - посылаются background процессам, когда они пытаются читать или писать в терминал, который их контролирует. [deep dive to sigttin/sigttou](http://curiousthing.org/sigttin-sigttou-deep-dive-linux)

Системные вызовы:

~~~ cpp
int tcsetpgrp(int fd, pid_t pgrp);
pid_t tcgetpgrp(int fd);
~~~

Проблема: **группы-сироты**. Группа - сирота, если в ней нет процесса, у которого родитель в другой группе, но в той же сессии. Процессы сироты бывают intentionally и unintentionally orphaned. Во втором случае им посылается `SIGHUP`, чтобы завершить, в первом они продолжатся в фоне. Такие обычно называются демонами.

**Демоны** - хотим процессы, которые не умирали бы при умирании терминала. Для этого перед `setsid` сделаем `fork()`. Это все равно плохо, потому что он может случайно привязаться к терминалу через fd. Можно еще раз сделать `fork()`, если он не станет лидером сессии, значит, мы вообще не можем получить терминал. Коротко о том, что должен сделать каждый уважающий себя демон:

* Dissociating from the controlling tty
* Becoming a session leader.
* Becoming a process group leader.
* Executing as a background task by forking and exiting (once or twice). This is required sometimes for the process to become a session leader. It also allows the parent process to continue its normal execution.
* Setting the root directory (`/`) as the current working directory so that the process does not keep any directory in use that may be on a mounted file system (allowing it to be unmounted).
* Changing the umask to 0 to allow `open()`, `creat()`, and other operating system calls to provide their own permission masks and not to depend on the umask of the caller
* Closing all inherited files at the time of execution that are left open by the parent process, including file descriptors 0, 1 and 2 for the standard streams (`stdin`, `stdout` and `stderr`). Required files will be opened later.
* Using a logfile, the console, or `/dev/null` as `stdin`, `stdout`, and `stderr`

Бесполезный (или нет) факт - в unix имена демонов принято заканчивать буквой d.

<a name="terminals"></a>
## Терминалы
UART. Line discipline. Драйвер tty. Псевдотерминалы.

Когда мы нажимаем кнопку (ну или раньше так было)
Сигнал идет по проводам в UART устройство, передается на другой конец (другому UART устройству), дальше проходит уровень Line Discipline, а потом передается в TTY (а потом в PTTY).

[Вроде неплохая статья](https://ru.wikipedia.org/wiki/TTY-%D0%B0%D0%B1%D1%81%D1%82%D1%80%D0%B0%D0%BA%D1%86%D0%B8%D1%8F) на весь этот билет (или не на весь)

Это все протоколы для работы с терминалами (первые три). На нижнем уровне UART - физический уровень. Дальше идет Line discipline - обертка над UART, позволяющая обрабатывать всякие команды типа `Enter`. А дальше уже tty, который обрабатывает уже команды.

**UART(Universal Asynchronous Receiver-Transmitter)** - физический протокол передачи данных (безумно старый протокол). Определяет стандарт передачи битов между двумя устройствами. Эта штука позволяет подключаться к другим устройствам к их терминалам. Пример из лекции: Андрей подрубил ардуинку, написал
`screen /dev/ttyUSB0 some_speed` и смог подрубиться к терминалу ардуинки.
Примерное описание работы данных устройств:

* Два соединенных между собой устройства работают на одной и той же частоте (частот много, можно привести пример 9600 бит в секунду.
* Договорились что во время простоя посылается логическая единица.
* Когда хотим что-то послать, посылается 0, потом 8 бит информации, потом один бит четности, и в конце единица, означающая конец сообщения. Сделано для того, чтобы эти устройства работали не рассинхронизируясь.
* Преобразует параллельную передачу бит в последовательную. [красивые картинки с неплохой статьей](http://www.circuitbasics.com/basics-uart-communication/)
* Для соединения двух устройств требуется всего два провода.
* Работают два устройства асинхронно (название намекает), и эти всякие мета-биты позволяют им работать асинхронно и тем не менее понимать друг друга. 

**Line discipline** - обертка над UART. Люди смекнули, что хотят изменять данные, посланные терминалу. Для этого придумали Line Discipline, которая ловит всякие `^C` и т.п. Позволяет редактировать написанное. Определяет поеведение данных, которые передаются по UART. У Bluetooth свой LD, который разбивает приходящие данные на пакеты. Изменив у себя LD с помощью `stty` можно во-первых передавать сразу символы, то есть они будут считываться сразу, а не после нажатия `Enter` (к примеру команда `cat` будет сразу выводить то, что нажал). И еще позволяет не выводить вообще то, что ты вводишь. Печатаешь в терминале а там ничего не появляется, но оно работает. 

**TTY(TeleTYpewriter** - драйвер, занимающийся job control'ом, позволяет запускать несколько процессов одновременно, некоторые в фоне. Позволяет работать с этими процессами, данные, полученные через line discipline отправляет только в главный процесс, не в фоновые.

**Псевдотерминалы** - абстракция над телетайпами. Создаются два терминала - master и slave. Ты подключаешься к master, который в свою очередь подключается к slave и управляет им. Зачем это нужно? Хрен его знает. Одно из применений - ssh. К примеру ты подключился к одному master'у, у которого запущен bash. Форкнем bash, создастся новый slave, а master останется тот же и соединение будет то же. Вроде сам Андрей с трудом объяснил зачем там два терминала. Если кто знает другие применения или знает в чем я ошибся - welcome. 
[Статья с картинкой, которые не помогли мне все равно этого понять](https://en.wikipedia.org/wiki/Pseudoterminal)

<a name="network"></a>
## Сеть
**Сокет** - интерфейс для обмена данными между процессами, использующий файловые дескрипторы.

Важные системные вызовы:

- [`socket(2)`](http://man7.org/linux/man-pages/man2/socket.2.html)
- [`bind(2)`](http://man7.org/linux/man-pages/man2/bind.2.html)
- [`listen(2)`](http://man7.org/linux/man-pages/man2/listen.2.html)
- [`connect(2)`](http://man7.org/linux/man-pages/man2/connect.2.html)
- [`accept(2)`](http://man7.org/linux/man-pages/man2/accept.2.html)

Для взаимодействия нужно иметь сокет сервера и клиента. Последовательность действий для сервера:

`socket` -> `bind` (хотя его не всегда надо делать) -> `listen` -> `accept` -> `read/write` -> `close`

Последовательность действий для клиента: `socket` -> `connect` -> `read/write` -> `close`

Системные вызовы:

~~~ cpp
// Создание сокета. Возвращает sockfd - дескриптор сокета
int socket(int domain, int type, int protocol);
int bind(int sockfd, const struct sockaddr *addr, socklen_t addrlen);
~~~

domain:

* `AF_UNIX` / `AF_LOCAL` - локальные взаимодействия
* `AF_INET` - IPv4
* `AF_INET6` - IPv6

type:

* `SOCK_STREAM` - последовательный двусторонний поток байт, даёт гарантии передачи данных
* `SOCK_DGRAM` - без гарантий и поддержки соединения, передаёт датаграммы - сообщения фиксированной длины. Типа очередь, в которую можно положить какие-то сообщения и вытаскивать их оттуда
* `SOCK_SEQPACKET` - первые два вместе, обмен датаграммами, но побольше гарантий, но есть не для всех семейств, для сетевых, например, нет

protocol: указывает протокол, который будет использоваться с сокетом, обычно в конструктор передается 0 - протокол по умолчанию

`sockfd` - файловый дескриптор, который соответствует сокету, по этому дескриптору понимает, какую структурку надо собрать

~~~ cpp
struct sockaddr {
	sa_family_t sa_family;
	char sa_data[14];
}

struct sockaddr_un {
	sa_family_t sun_family;
	char sun_path[108];
} // AF_UNIX

struct sockaddr_in {
	sa_family_t sin_family;
	in_port_t sin_port;
	struct in_addr sin_addr;
} // AF_INET
~~~

В структурах `sockaddr_*` первое поле - информация о том, какой именно это `sockaddr`. Получается ООП на С: как будто `sockaddr_in` и `sockaddr_un` наследуются от `sockaddr`.

Системные вызовы:

~~~ cpp
int listen(int sockfd, int backlog); 
int accept(int sockfd, struct sockaddr* addr, socklen_t addrlen);
int connect(int sockfd, const struct* addr, socklen_t addrlen);
~~~

* `backlog` - максимальный размер очереди ждущих сокетов, нужно, чтобы ограничить число тратимых ресурсов
* `accept()` возвращает файловый дескриптор клиента
* `connect()` соединяет сокет, специфицированный файловым дескриптором `sockfd`, с адресом, специфицированным addr

**UNIX-сокеты** (Unix domain socket, IPC socket) - сокеты для локального межпроцессного взаимодействия, используют файловую систему как адресное пространство имен, то есть они представляются процессами как иноды в файловой системе. Это позволяет двум различным процессам открывать один и тот же сокет для взаимодействия между собой. Однако, конкретное взаимодействие, обмен данными, не использует файловую систему, а только буферы памяти ядра.

`AF_UNIX` - по сути своей пайпы на стероидах.

~~~ cpp
int socketpait(int domain, int type, int protocol, int sv[2]);
~~~

`socketpait` создает пару соединенных между собой сокетов, в `sv` лежат 2 сокета, которые надо соединить

**Datagram sockets** - могут отправлять запросы на какой-то адрес и слушать с какого-то адреса без непосредственного подключения

~~~ cpp
// отправить информацию на какой-то адрес
ssize_t sendto(int sockfd, const void *buf, size_t len, int flags, const struct sockaddr *dest_addr, socklen_t addrlen); 
// послушать какой-то адрес
ssize_t recvfrom(int socket, void *restrict buffer, size_t length, int flags, struct sockaddr *restrict address, socklen_t *restrict address_len);
~~~

**Модель OSI** - 7 уровней:

* Прикладной: HTTP, DNS, много всяких протоколов
* Представления: SSL, TLS, мало
* Сеансовый: RPC, мало
* Транспортный: TCP, UDP, SCTP...
* Сетевой: IPv4, IPv6, ICMP, IGMP
* Канальный: ARP, PPP, Ethernet...
* Физический: USB, Bluetooth, стандарты Eth...

SSH - прикладной протокол (но в некотором смысле сеансовый тоже). В свое время протокол TCP/IP всех победил, он работает на прикладном, транспортном, сетевом и канальном уровнях

Бывают разные способы коммутации - канальная (как на старых телефонах), пакетная сейчас. MTU - maximum transmission unit, минимальная пропускная способность

Стек протоколов **TCP/IP**:

* HTTP прикладной (1GB)
* TCP транспортный (1GB)
* IP сетевой (65000B)
* Eth канальный (1500B) - вот тут то и bottleneck

`[MAC-address[1.2.3.4[port80 [GET/]]]]`: HTTP (GET /) обёрнут в TCP (port 80), это обёрнуто в IP, это обёрнуто в Eth (mac address) (`[Eth[IP[TCP[HTTP]]]]`). Это все нарезается на маленькие пакеты, потому что каждый уровень не резиновый, и потом склеивается. Вся конструкция называется стеком, потому что какой-то уровень волнует только то, что находится на один уровень глубже, а остальное не волнует. 

**IP**:

* метаданные примерно 20 байт
* примечательные поля:
	* версия
	* длина
	* сдвиг фрагмента
	* TTL
	* протокол
	* checksum
	* адрес отправителя
	* адрес получателя

**TTL** - число переходов, которое переживет пакет. **Tracepath** с помощью TTL: посылать запросы с TTL от 1 до тех пор, пока не дойдёт. Тот, на ком TTL закончился, отправит сообщение "у меня сдохло". Так можно отследить весь маршрут.

**UDP**: +8 байт метаданных:

* порт отправителя
* порт получателя
* длина
* checksum

**TCP**: +20 байт:

* порт отправителя
* порт получателя
* seq no - сколько данных отправлено
* ack no - информирует о том, что доставка прошла успешно
* флаги (SYN(synchronize) - initiates connection, FIN(final) - terminates connection, ACK -- acknowlegments recieved data)

**ICMP** (Internet Control Message Protocol) - используется для передачи сообщений об ошибках и служебной информации, например, о том, что запрошенное устройство недоступно. Редко используется для передачи сообщений между системами или приложениями. ICMP используют тулзы типа ping и traceroute.

> For example, every device (such as an intermediate router) forwarding an IP datagram first decrements the time to live (TTL) field in the IP header by one. If the resulting TTL is 0, the packet is discarded and an ICMP Time To Live exceeded in transit message is sent to the datagram's source address.

> Although ICMP messages are contained within standard IP packets, ICMP messages are usually processed as a special case, distinguished from normal IP processing, rather than processed as a normal sub-protocol of IP. In many cases, it is necessary to inspect the contents of the ICMP message and deliver the appropriate error message to the application responsible for transmission of the IP packet that prompted the sending of the ICMP message.

Бывают сырые сокеты: `raw(7)`, `sock_raw`

~~~ cpp
socket(AF_INET, SOCK_RAW, int protocol);
~~~

`socket` - на него будут валиться все пакеты с протоколов, которые указаны в protocol. Создавать сырые сокеты может только рут.

**DNS** - name -> ip. Бывает:

* Рекурсивный: приходишь на сервер, он говорит "я не знаю, но можешь посмотреть вон там", и ты там смотришь
* Нерекурсивный: приходишь на сервер, он говорит "я не знаю, но я сейчас сам там посмотрю и скажу"

Системный вызов:

~~~ cpp
int getaddrinfo(const char *node, const char *service, const struct addrinfo *hints, struct addrinfo **res);
~~~

Полезный (или нет) факт: в valgrind используй `--tool=cachegrind`

<a name="input_output"></a>
## Ввод-вывод
**Мультиплексирование ввода-вывода**. Мотивация: есть много файловых дескрипторов, хотим со всеми работать одновременно (в какие-то писать, из каких-то читать), не понятно, в каком порядке будут действия.

Что делать?

***1 способ***: сделать операции ввода-вывода неблокирующими. Читать будем сразу после вызова `read()`, если ничего нет, то `errno` = `EWOULDBLOCK` (говорит о том, что данных еще нет)

Как сделать файловый дескриптор неблокирующим?

~~~ cpp
int flags = fcntl(STDIN_FILENO, F_GETFL);
flag |= O_NONBLOCK;
fcntl(STDIN_FILENO, F_SETFl, flags);
char ch;
int r = read(STDIN_FILENO, &ch, 1);

inf fds[] = ...;
for (int fd : fds) {
	if (read(fd) == 0) {
		continue;
	}
	...
}
~~~

Тут есть busy-wait, поэтому этот способ медленный и вообще не очень :(

***2 способ***: `fork()`, создадим для каждого клиента свой процесс со своим файловым дескриптором

~~~ cpp
while(true) {
	int client = accept(...);
	if (fork() == 0) {
		close_all();
		// закрыть все остальные файловые дескрипторы, кроме текущего
		// в родителе нужно закрыть текущий файловый дескриптор, иначе протечём
		process(client);
		exit();
	};
}
~~~

Если так сделаем, то создается очень много процессов, система может этому не очень обрадоваться. Более того, тут нет никакой обработки zombie процессов, если сервер работает долго, то кончатся pid. Можно сделать обработчик `SIGCHLD`, будет получше

***3 способ***: `select(2)`: ждет `timeout` времени (или меньше, если что-то можно делать раньше), возвращает с каким количество файловых дескрипторов можно работать и расставит единички в `bitset`-ах

~~~ cpp
int select(int nfds, fd_set *readfds, fd_set *writefds, fd_set *exceptfds, struct timeval *timeout);
~~~

`fd_set` - это битсет примерно на 1000 элементов, маловато будет

~~~ cpp
struct timeval {
	long tv_sec;
	long tv_used;
}
~~~

Можно использовать как таймер. Перед каждым вызовом `select`'а нужно заполнять все заново. Плюс в том, что такой метод таки работает. Минусы в том, что работает долго и может держать мало файловых дескрипторов

***4 способ***: `poll(2)`

~~~ cpp
int poll(struct pollfd *fds, nfds_t nfds, int timeout);

struct pollfd {
	int fd; 		//file descriptor
	short events; 	//requested events;
	short revents; 	//returned events;
}
~~~

Забавная фича: отрицательные fd игнорируются (если временно хотим игнорить какой-то дескриптор), работает на самом деле плохо

* events: `POLLIN`, `POLLOUT`, `POLLRDHUP`, ...
* revents: `events` + `POLLERR`, `POLLHUP`, `POLLNVAL`

Плюсы: работает с большими номерами фд. Минусы: много файловых дескрипторов работают медленно :(

***5 способ***: `epoll(7)`. В целом как `poll`, но хранит список дескрипторов в ядре.

Системные вызовы:

~~~ cpp
int epoll_create(int size);
// параметр игнорируется, можно передать что угодно >0
int epoll_ctl(int epfd, int op, int fd, struct epoll_event *event);
int epoll_wait(int epfd, struct epoll_event *events, int maxevents, int timeout);
~~~

* `epoll_create` возвращает файловый дескриптор
* `epoll_ctl`
* `epoll_wait` возвращает число файловых дескрипторов, которые хотят что-то поделать

~~~ cpp
void union epoll_data {
	void *ptr;
	int fd;
	uint32_t u32;
	uint64_t u64;
} epoll_data_t;

struct epoll_event {
	uint32_t events;
	epoll_data_t data;
}
~~~

**Level-triggered** vs **edge-triggered**:

* level_triggered - уведомление на каждый запрос 
	* `select poll`, `epoll`
* edge_triggered - уведомление только при изменениях файловых дескрипторов
	* `epoll`, ввод-вывод на сигналах

Еще одна фича `epoll`: `EPOLLONESHOT` - события одноразовые. Нужно в многопоточности, когда несколько потоков работают с одним `epoll`-ом это будет гарантировать, что уведомление о событии придет ровно в 1 поток.

Еще немного об `O_NONBLOCK`:

~~~ cpp
while(true) {
	int cnt = poll(...);
	for (fd : fds) {
		// если клиет отвалится где-то здесь, то accept подвиснет и будет ждать 
		// следующего клиента, чтобы это побороть, можно сделать nonblocking accept
		accept(fd, ...);
	}
}
~~~

Про сигналы. Пусть хотим дождать сигнала или файлового дескриптора.

* **self-pipe trick**: создадим пайп, в обработчике сигнала запишем 1 байт в пайп, сам пайп повесим в `poll`
* `pselect(2)`

~~~ cpp
int pselect(int nfds, fd_set *readfds, fd_set *write_fds, fd_set *exceptfds, const struct timespec *timeout, const sigset_t *sigmask); 
~~~

В `sigmask` информация о том, какие сигналы хотим ловить, а еще он атомарный, это полезно

Более того, есть `ppoll(2)`, `epoll_pwait(2)`

~~~ cpp
int ppoll(struct pollfd *fds, nfds_t nfds, const struct timespec *tmo_p, const sigset_t *sigmask);
int epoll_pwait(int epfd, struct epoll_event *events, int maxevents, int timeout, const sigset_t *sigmask);
~~~

А еще есть `signalfd(2)`

~~~ cpp
int signalfd(int fd, const sigset_t *mask, int flags);
~~~

Создает файловый дескриптор, которым можно ловить нужные нам сигналы, его уже можно повесить туда, куда нам нужно, будь то `select`, `poll` или `epoll`.

***6 способ*** (та еще наркомания): `SIGIO`. Идея в том, чтобы слать сигнал каждый раз, когда стало можно что-то сделать с каким-то файловым дескриптором

~~~ cpp
fcntl(fd, F_SETOWN, pid);
flags |= O_ASYNC | O_NONBLOCK;
fcntl(fd, F_SETFL, flags);
~~~

Минусы: непонятно, как с каким фд работать. Какая-то странная штука, не очень понятно, как применять на практике, но концепция прикольная. Ко всему этому можно прикрутить realtime-сигналы: будем получать несколько сообщений о сигнале, информацию о том, в каком дескрипторе что произошло.

<a name="loading"></a>
## Загрузка
Загрузка. BIOS vs EFI. MBR. GRUB. multiboot spec. initrd, initramfs. Системы инициализации.

**План лекции**:

* откуда берется первый код
* system firmware
	* BIOS
	* EFI
* загрузчик
	* MBR
	* GRUB
	* chainloading
	* multiboot
* загрузка ядра 
	* настройка CPU
	* initrd / initramfs
	* pivot_root

Презентации Андрея с лекции про [init](http://neerc.ifmo.ru/~os/static/09-init.pdf) и [лицензии](http://neerc.ifmo.ru/~os/static/09-licenses.pdf).
Конспекты Леонида отправляют нас читать [презенташку](https://github.com/sandwwraith/os2016-conspect/blob/master/lectures/OS-Init.pdf).

Краткая выдержка из конспекта Леонида.

###Как загружается ОС?

* Embedded controller. Маленький контроллер со своей архитектурой и кучей пинов. На материнке.
* Intel Managament. Внутри процессора. Код не получить. Может использоваться для remote management
    
Порядок загрузки:

1. Нажали на кнопку. EC просыпается, проверяет батарею/доступность электричества, если ему всё нравится, пинает процессор с Intel Managament.
2. Процессор отправляется по адресу 0xFFFFFFF0 в незащищенном режиме. По историческим причинам (640Кб хватит всем) это оказывается джамп на какую-то часть биоса.
3. Два стула: BIOS/EFI. Они должны проинициализировать железо. BIOS ничего не знает о современных технологиях. Зато EFI знает про разметку диска, разделы и т.п.
4. Если мы загрузились из биоса, загрузчик лежит в первом секторе диска. EFI может просто выполнить код с /boot/efi/... , не используя загрузчики
5. Ядро: init (на самом деле два - один минимальный, чтобы смониторовать норм FS, другой реальный, который может находиться хрен пойми где). Udev - для Plug'n'Play
6. SysVinit - овно, upstart - что-то не взлетевшее из убунты, systemd - норм.

Теперь подробнее.

Нажали кнопку включить. Есть только мать, остальное выключено. На материнской плате есть внутренняя флеш-память, в которой есть код. Размер памяти примерно 256 килобайт. Процессор идет по адресу 4Гб (0xfffffff0) - 16 по шине памяти. Контроллер памяти понимает, что этот адрес - это память материнки. Когда мы идем по адресу x, контроллер сам определяет, к какому физическому устройству надо обращаться. Там есть `jmp` на начало кода прошивки (BIOS), `jmp`, а не сама прошивка, потому что сама прошивка не очень стандартизована и может не поместиться. 

**BIOS** - Basic Input-Output System. Пока все выключено, кроме процессора, и памяти тоже нет. BIOS расположен в тех же 256 килобайтах. 

Загрузились в BIOS. Он включает и настраивает память и различные устройства, включает и настраивает кеши (в это время памяти как таковой нет, только регистры). В устройствах может быть флеш-память с кодом, который будет исполняться при инициализации.

Идем в CMOS RAM - память, в которой записан порядок загрузки (с чего дальше грузить ось):

* спрашиваем у девайса (у него есть флажок), можем ли мы с него загрузиться
* загружаем первый сектор (512 байт) в память по адресу `0x7c00` (уже в оперативу, она инициализирована)
* если они закончились в `0x55 0xaa`, то это загрузчик
* если нет, то идем на другое устройство

Проблемы BIOS-а:
 
* старый
* не стандартизован
* не знает про концепцию загрузчика (просто делает `jmp` на первый код с нужной меткой, но не знает, что будет дальше)
* не знает про концепцию разделов на диске (не умеет искать файлы загрузчика на разделах)
* 16-битный
* не переносится на другие архитектуры

Середина 90-х: **EFI** (Extendeable Firmware Interface)

* IA64 попытка Intel сделать 64 битную архитектуру, но она не имела обратной совместимости с 32-битной, очень жаль
* стандартизован
* имеет драйвера
* знает про GPT (разделы на диске)
* знает про FAT
* имеет Trusted Platform Model (TPM), включает в себя какое-то количество регистров, в которые можно писать значения (хеш от операнда и предыдущего значения в этом регистре) и читать. Если загрузка происходит одинаково, то после загрузки там будут постоянные значения, значит загручик может проверить, нормально ли мы загрузились (есть какой-то measure boot)
* поддержка пока не очень

Сейчас **UEFI** (Unified EFI).

Загрузка с EFI:

* настраиваем кеш
* инициализируем память и устройства
* загрузка драйверов (т.к. знает про разделы и файловые системы)
* выбор boot device:
	* или legacy/сеть/CD
* запустить загрузчик с boot device по указанному пути: по умолчанию `/EFI/BOOT/BOOTX64.EFI`

**MBR** (Master Boot Record):

* первые 12 байт жесткого диска (где есть загрузчик и таблица разделов)
* таблица разделов (partition disk) максимум 4 раздела: 4 записи (тип, начало, размер, bootable)
* extended разделы (поделить один раздел (запись) на кучу других логических разделов, если 4 не хватает, костыль и не всегда работает)
* загрузчик может полностью в MBR не влезть (поэтому там есть `jmp` на следующую подстадию загрузки)

Сейчас загрузчики жирные. Поэтому во втором третьем секторе лежит код того же загрузчика. На первом этапе этот код из 2-у сектора загружается в оперативу и джампается на него. Тогда начинается новый подэтап загрузки.

Загрузчики(bootloaders):

* GRUB 0.99 (legacy)
* GRUB2 (актуальный)
* LILO <3 (старый и тупой как пробка)
* Syslinux (для загрузки по сети)
* NTLDR (для винды)

**GRUB**:

* может дойти до следующей стадии загрузки
* знает про модули ядра
* знает про файловую систему
* знает про конфиги
* знает про chainloading (чтобы подружиться в виндовым загрузчиком)

**multiboot specification**:

* загрузчик хочет знать, что он грузит
* ОС хочет знать, куда и как ее загрузили
* multiboot указывает:
	* в каком состоянии будет находиться железо при передаче управления ядру ОС
	* как передавать ядру параметры

BIOS -> ищет в MBR загрузчик -> GRUB (или что-то еще) -> ядро

EFI -> куча разных вариантов -> ядро.

Сейчас в основном ставят EFI (в легаси или без) и грузят GRUB, хотя есть возможность загружать сразу из EFI.

Ядро:

* что за процессор и какие у него возможности (`man dmesg`)
* сколько есть всякой разной памяти
* как настроен кеш и как его перенастроить под себя
* какие есть процессоры (ACPI)
* запуск других процессоров
* initrd (init ram disk)/initramfs (init ram file system) (образ корневой файловой системы) в качестве rootfs (тут у нас появляется какая-то файловая система)
* `init` из ramdisk (первый процесс, который загружает всякие драйвера и всякие другие сервисы)
* загрузка модулей
* запуск udev (`man udev` - менеджер девайсов (при втыкании флешки он делает свою работу)) создает в dev устройства монтирование реального корня
* монтирование реального корня - монтируем корень реальной файловой системы
* pivot_root/chroot (меняем корень с тем, который был в initrd/initramfs)
* запуск реального `init` на реальной файловой системе

![Cм.картинку.](20.04_1.jpg)

**init**: `/boot/initramfs-linux.img` - какой-то архив. Когда распакуешь его, будет еще архив. Распаковав его, увидим кучу файлов. Там есть `init`, который запускается в самом начале (он в конце запускает настойщий `init`). Настоящий `init` - `/sbin/init`. Чем занимается:

* запускает все при запуске системы;
* добивает зомби.

**SysVInit**: несколько уровней запуска (runlevel):

* 0 - выключить компьютер
* 1 - однопользовательский режим
* 3 - многопользовательский режим
* 5 - многопользовательский + GUI
* 6 - перезагрузка

При запуске выбирается уровень. Настройки SysVInit - `etc/inittab`, строки формата `id:runlevels:action:cmd`, формат: `id:список уровней на которых можно запускать:действие:команда для запуска этого действия`. Действия:

* initdefault - уровень по умолчанию
* respawn - флажок "перезагружать при завершении процесса"
* wait - запускают один раз, `init` не пойдет по списку дальше, пока сервис не отработает, т.к. ждет завершения процесса
* once - запускат единожды процесс. Но wait ждет завершения процесса
* ctraltdel - что при Ctrl+Alt+del

`telinit` - перейти на другой уровень. `telinit n` - поменять уровень на n.

Что такое сервисы:

* все файлы, которые отвечают за сервисы, хранятся в `/etc/init.d/`
* сервисы - просто скрипты для запуска программ

`etc/rc[0-9].d` - файлы сервисы. Симлинк в `/etc/init.d`. Сервисы надо запускать в каком-то порядке. Будем писать `KDDname`, `SDDname`, `K` - ничего, `S` - запустить, `DD` - приоритет 00-99, `name` - имя сервиса

bash-скрипты

* service ssh start
* ... stop
* ... restart
* ... status

Недостатки:

* не параллельно
* неудобно следить за зависимостями (сервисы исполняются в порядке `DD`, без зависимости друг от друга)
* не следит за запущенными процессами

Альтернативы:

* Upstart (Ubuntu 6.10)
* OpenRC (Gentoo)
* systemd (кроме Gentoo)

systemd следит за всем, заменил кучу всяких утилит в linux.

Для каждого сервиса есть unit файл с описанием, от чего он зависит, как себя ведет и тд. Зависимости для сервиса:

* Requires - кого хотим, если нужный сервис не удался, не запускаем и этот
* Wants - отличие от Requires в том, что если сфейлился то от чего зависим, то все таки попробовать запуститься
* WantedBy - кому нужны

Порядок:

* After - перед кем
* Before - после кого

Тип (action из init)

* Restart:
	* no
	* always

Еще полезное в systemd

* journalctl
* socket activation - когда кто-то постучался на сокет, systemd запускает программу и отдает управление ей
* Шаблоны

**Лицензии**: Свободное ПО(Своды Столлмана)

Copyleft:

* антоним copyright
* можно без чьего-либо согласия модифицировать
* нужно сохранять лицензию

**GPL**

* свободная лицензия
* Copyleft-лицензия
* производные работы можно распространять только по GPL
	* LGPL
		* можно использовать в не-GPL проекта
		* при изменении кода нужно перелицензировать в GPL
	* AGPL
		* сетевые пользователи могут получить код

Другие лицензии:

* BSD, Apache, MIT...
	* не copylef
* Public Domain - публично достояние

**Creative Commons** - частаня лицензия для не-кода. Ограничения:

* Attribution (BY)
* Share-alike (SA)
* Non-commercial (NC)
* No deriative works (ND)

Выбирай любое подмножество

Теперь немного про лицензии от Леонида (те же яйца, что и выше, только в профиль)

### Лицензии на программы (ВНЕЗАПНО)

Изначально всем было пофиг. А потом появился Столлман. 4 свободы:

1. Запускать
2. Изучать (смотреть исходники)
3. Распространять
4. Изменять и делиться.

Это GNU GPL. `copyleft` - это как копирайт, только наоборот. Если кто-то использует ваш код под GNU GPL,
его разработка тоже должна быть под этой лицензией.

В `GPLv2` завезли LGPL и AGPL - первая более мягкая, вторая более жесткая по сравнению с обычной GPL.
`LGPL` разрешает не лицензировать под GPL скопированный код, если его не меняли.
`AGPL` требует открывать ваш исходный код, даже если вы не используете сам продукт под AGPL, но даже если взаимодействуете с ним (по сети, например).

`GPLv3` содержит ограничения и на железную часть. (В v2 был прецедент с тем, что исходники под какую-то приставку были выложены, но в самой приставке аппаратно были заблокированы сторонние прошивки).

`BSD, MIT, Apache` - можно использовать код под этой лицензией в своей закрытой разработке.

`Creative Commons` - лицензии на медиа (картинки, музыку, ...). В наборе есть лицензии и типа "Общественное достояние", и copyleft, и запрет изменений.

**Thomson's compiler hack**

Допустим, у нас есть компилятор и его исходники. Мы хотим сделать себе бэкдор в систему, для этого нужно заменить программу `login`. Встроим в компилятор штуку типа "если мы компилируем что-то типа `login.c`, то встроить бэкдор". Теперь, даже если я прочитал исходник login'а и уверен, что всё в порядке, это не так. Даже если мы захотим скомпилировать заново хороший компилятор, плохой бинарник может скомпилировать его в себя.

Мораль: ***НЕ ДОВЕРЯЙ НИКОМУ***


###Эпилог
> All software is shit. Всё очень плохо.

###Копипаста письма
Библиография:

* [Про не-x86 железки в компьютере: Joanna Rutkowska, x86 considered harmful](http://blog.invisiblethings.org/papers/2015/x86_harmful.pdf)
* Про socket activation, про которую я плохо рассказал: [Lennart Poettering, systemd for Administrators XX](http://0pointer.de/blog/projects/socket-activated-containers.html)
* Про то, как инициализируется процессор: [Intel® 64 and IA-32
Architectures Software Developer’s Manual Volume 3 (3A, 3B, 3C & 3D):
System Programming Guide, Chapter 9](https://www-ssl.intel.com/content/dam/www/public/us/en/documents/manuals/64-ia-32-architectures-software-developer-system-programming-manual-325384.pdf)
* Про EFI: [Beyond BIOS](http://www.microbe.cz/docs/Beyond_BIOS_Second_Edition_Digital_Edition_(15-12-10)%20.pdf)

Еще немного [статей](http://wiki.osdev.org/Boot_Sequence).

BIOS vs EFI. MBR. GRUB. multiboot spec. initrd, initramfs. Системы инициализации.

<a name="object_files"></a>
## Объектные файлы
Таблицы импорта и экспорта. Таблица релокаций. Запуск статически слинкованных файлов.

Тут кажется нечего рассказывать. Есть объектные файлы. Таблица экспорта - таблица символов, которые определены в данном объектном файле. Таблица импорта - таблица символов, которые нужны этому объектному файлу (дырки, смотри ниже). Таблица релокаций, если я правильно понимаю, то же, что и таблица импорта. При статической линковке дырки заполняются, как могут. Когда все дырки заполнены и объектный файл содержит символ `_start`, это получается уже исполняемый файл. Если кто-то знает больше обо всем этом, добавь, не проходи мимо.

<a name="dynamic_linking"></a>
## Динамическая линковка
GOT/PLT. LD_PRELOAD. Запуск динамически слинкованных файлов. Ленивая динамическая линковка. Интерпретатор программ.

**Динамическая линковка**. Как это все устроено (в общих чертах). Есть библиотека, к примеру `lib.so` с функцией `f()`. И есть файл `main.c`, который где-то вызывает эту функцию `f()`. Можно сказать, объектный файл `main` будет содержать дырку - вызов неизвестной функции `f`. До заполнения дырки там содержится `jmp` на нулевой адрес. Как заполнять дырки? Укажем компилятору (или линкеру) что некоторые функции из `main.c` содержатся в `lib.so`. А потом, уже в рантайме, когда до этого вызова дойдем, загрузим библиотеку и вызовем эту функцию. Также хочется еще, чтобы библиотека не загружалась дважды в физическую память. У нас будет только одна копия библиотеки в физической памяти, а в виртуальной у каждого процесса будет выделена память под эту библиотеку, которая будет указывать на один и тот же физический адрес. 

Предположим процесс `a` и `b` захотели функцию `f` из библиотеки `lib`. Эта библиотека загружена в виртуальную память каждого процесса (в физической памяти она только в одном месте). И скорее всего она находится в разных местах в разных процессах, к примеру в первом процессе по адресу `5`, а в другом по адресу `10`. Функция `f` соответственно тоже расположена по разным адресам. И в общем надо сделать так, чтобы функцию `f` можно было вызывать легко и просто. Если у нас 100 вызовов функции, менять все 100 вызовов на какой-то адрес будет долгим и кривым решением. Давайте создадим таблицу (GOT, Global offset table) для текущего процесса, в которой будут храниться указатели на функции из динамически загруженных библиотек. А вместо вызова `f` будем вызывать `got@f`. Теперь при загрузке динамической библиотеки не надо менять 100 вхождений, надо поменять только GOT. И разные процессы, где библиотека в разных местах, будут различаться только в GOT.

Потом (или не потом) придумали еще PLT. Очередная таблица, которая содержит функции. Возьмем старую функцию `f`. Для нее создастся функция `plt@f` примерно такого вида: если еще неизвестно, где эта функция расположена, найди ее и запомни. А если известно, то прыгай туда. Это позволяет лениво загружать функции. PLT кстати не меняется и ее можно поместить в read-only секцию. [Отличная статья про это все (GOT и PLT).](http://eli.thegreenplace.net/2011/11/03/position-independent-code-pic-in-shared-libraries//)

**LD_PRELOAD** - штуковина, которая позволяет загружать какую-нибудь библиотеку перед тем, как загружать все остальные. Пример от sandwwraith:
Например, LD_PRELOAD=mymalloc.so emacs запускает emacs со своим аллокатором памяти, который должен быть определён в вашей mymalloc.so. 

В душе не знаю, что рассказывать про интерпретатор программ.

<a name="os_in_hardware"></a>
## ОС в железе
**Кольца защиты**

> Rings offer a protection layer for programs. They allow certain levels of resource access to processes. This is good, because it keeps bad programs from messing things up. There are, however, several downsides: The more CPU rings you use, the more the OS is tied to the architecture. You can, however, have several architectures each with it's own ring switching code. Another issue with this is that you OS must have a TSS set up and several other features, making ring switching much more difficult than just running all programs in kernel mode. There are a total of 4 rings in most common architectures. However, many architectures have only two rings (e.g. x86_64), corresponding to ring 0 and 3 in this description.

* **ring 3** - usermode. Минимальный уровень доступа
* **ring 1-2** - что-то между, в x86_64 не используется
* **ring 0** - kernel/supervisor mode. В этом режиме работает ОС при старте системы, в этом режиме работают обработчики прерываний.
* -1 - hypervisor, виртуализация. hypervisor предоставляет интерфейс для виртуальных машин. Сами виртуалки работают в нулевом кольце. Они не могут испортить ОС, так как она работает через hypervisor-а.
* -2 - SMM (system management mode): железо, питание. Биос работает где-то здесь
* -3 - AMT администрирование, работает независимо от того, работает ли основной процессор

Обычный код пишется для третьего кольца. -1 для разработчиков виртуальных машин, в -2 иногда пускают простых смертных (coreboot), -3 для богов, но в понедельник [все сломалось](https://www.theregister.co.uk/2017/05/01/intel_amt_me_vulnerability/) на всех интелах 2008-2017 (даже боги совершают ошибки)

**Прерывания**: сигналы

* Железные
	* данные считались
	* таймер тикнул
* программные
	* программа хочет внимания системы
	* syscalls
* исключения
	* деление на ноль
	* page fault

* real mode: 0x100 адресов по адресу 0 (байты 0 - 1024), никакой доп информации
* protected mode: массив структур по адресу `%idtr`, P - есть ли, DPL - права

Прилетело прерывание, что делать? Надо положить в стек регистры. Кладем в стек ядра, так как пользовательский стек может указывать куда угодно (даже на код ядра)

**Hardware Multitasking**:	TSS - табличка с данными процесса, адрес стека для прерываний

`iret` - выход из обработчика, снимает из стека данные и возвращается обратно

Примеры прерываний:

* Division by zero
* Breakpoint
* General Protection Fault 
* Page Fault - обращение к памяти без бита P (present)
* Double Fault - при попытке запустить обработчик всё сломалось
* Triple Fault - перезагрузка

Таймер позволяет рапределять процессорное время

Direct memory access - прерывание на конец записи чтения

**PAE**: 32-х битные виртуальные адреса, 64-битные физические адреса 

Было 32 = 10 + 10 + 12

* 1024 записи по 4 байта 
* 2 уровня таблиц

Стало 32 = 2 + 9 + 9 + 12

* 512 записей по 8 байт
* Три уровня таблиц
* +NX бит - бит можно ли исполнять

**TLB** кеширует физические адреса. Не надо прыгать по MMU

**swap**:	выгружаем давно не использовавшуюся память на диск. Accessed бит иногда сбрасывается в 0, если он давно 0, то можно кинуть на диск. Летит page fault, ядро смотрит в swap, и если он там есть, то страница загружается обратно. Есть nonswappable (к примеру код загрузки в swap и из него)

**Ленивое выделение страниц**. Пусть просит 1Г. Отобразим сначала все page table в одну страницу, заполненную нулями. Когда к ней обращаются на запись, бросается page fault, и только тогда выделяется память.

Еще с помощью page fault можно использовать memory-mapped файлы. Лениво загружаем файлы в память, а не сразу

dirty-bit бит - данные поменялись

**vdso**: хотим еще более быстрые системные вызовы, к примеру текущее время. Пошарим страницу со временем между всеми процессами и будем просто обращаться к этой странице, если надо.

реализация системных вызовов? trapcc?

<a name="allocation"></a>
## Аллокация
Есть куча способов получить себе память:

* `new` / `delete`
	* реализация в userspace
	* можно переопределять
* `malloc` / `free` + `calloc`, `realloc`, `alloca`
	* реализация в userspace
	* тоже можно переопределять, если очень хочется (а нам хочется)
* `mmap` / `munmap`
	* реализация в kernelspace
	* на самом деле представляют собой системные вызовы

Был еще `sbrk`, но он скорее мертв, чем жив, и не используется. Сейчас основной способ попросить памяти у системы - `mmap` и `munmap`

Память процесса:

Как было в `sbrk`, сейчас не так:

Стек растет вниз, куча растет вверх, если они встретились, то все плохо, память кончилась.

стек
  |
  V
куча
  ^
  |
данные (всякие глобальные переменные)
код

Сейчас как-то так:
стек
  |
  V
куча
данные
код

А что мы, собственно, хотим?

* реализовать `malloc` и `free`
	* запрашивать данные произвольного размера
* через `mmap` и `munmap`
	* нельзя выделить меньше страницы
	* только целое число страниц

**SLAB** - Странный непрактичный аллокатор. Будем выдавать данные фиксированного размера k:

* Суперблок - большой кусок памяти
	* разбитый на куски по k
	* будем хранить список свободных ячеек
	* при запросе смотрим на голову списка, отдаем память, двигаем голову и помечаем старое как занятое

Проблемы:

* где хранить список свободных?
* суперблок кончился
* как делать `free`?

Решения:

* положим в начале суперблока служебную информацию (указатель на голову списка, например)
* будем хранить связный список прямо в свободных ячейках (в начале свободной ячейки будет указатель на следующий свободный)
* будем хранить много суперблоков, лежащих в двусвязном списке, и 2 переменные с указателями на заполненные суперблоки и суперблоки, в которых еще можно что-то выделить
* а как освобождать-то? Во `free` нам дается только адрес, зная адрес, мы не знаем, где начало суперблока :( Решение: выровняем суперблоки, например, пусть они будут размером со страницу, тогда сможем понять, в каком мы суперблоке лежим, после этого обновим head и прочее.
* что делать, если блок был полностью заполнен? Нужно переместить его из списка заполненных (full) в список частично заполненных (slabs)

Плюсы:

* быстро (почти все за О(1), когда нужно создать новый суперблок - подольше)
* маленький overhead по памяти (только немного служебной информации в каждом суперблоке)

Минусы:

* один размер выдаваемых кусков :(

**Free List Allocator** - более лучший аллокатор (хочу пикчу). Будем хранить список свободных кусков, опять суперблоки, но куски нефиксированного размера, при запросе будем хранить по указателю размер, который мы отдали, а вернем пользователю указатель на данные. При запросе может не подойти первый попавшийся кусок (может быть слишком маленьким), тогда бежим по списку и ищем блок хотя бы такого размера, который нам нужен. Найденый блок разбиваем, первую половину вернем пользователю, вторую сделаем новым пустым блоком

Плюсы:

* работает для любых размеров (!но меньше суперблока)

Минусы:

* работает долго
* фрагментация (пустые куски разбросаны по памяти), память есть, но разбросана по разным блокам, поэтому считаем, что ее нет. Решение - завести список последовательных кусков и выделять их как один в случае чего.

Бывают разные стратегии выбора кусков, которые отдаем:

* first-fit, первый пусток кусок, который подходит
* best-fit, кусок, который подходит лучше всего

Какая из стратегий лучше - вопрос дискуссионный ¯\\\_(ツ)_/¯

Еще про склеивание: а нужно ли оно нам вообще? Программы иногда просят куски одинаковых размеров, если склеим, будем тратить больше времени.

Прочие оптимизации: Можно использовать кучу или дерево поиска для поиска кусков, это будет побыстрее. Но память используется менее эффективно, ибо на каждый кусок еще хранится память на размер.

Соберем все в кучу, чтобы получить какой-то разумный аллокатор:

* SLAB для маленьких
* Free List для средних
* `mmap` для больших

Казалось бы, работает всегда, но нет. Как делать `free`, как узнать, из какого аллокатора пришел объект? Нет хорошего решения, придется танцевать с бубном, аля хранить для куска, какой аллокатор его породил, получаем еще больше оверхеда.

Другой хороший вариант:

* фиксируем X > 1 (2, например)
* создаем SLAB для объектов размера m, mX, mX<sup>2</sup>, mX<sup>3</sup>...
* округляем размер запроса вверх к ближайшему mX<sup>i</sup>
* `mmap` для очень больших запросов

Итог: проиграли по памяти еще сильнее, но реальные аллокаторы как-то так и выглядят. Для `free` тоже приходится использовать всякие трюки.

**Hoard**: к нам пришла многопоточность :] Несколько решений проблемы прихода:

* одна большая блокировка на все (ваша многопоточность не многопоточность)
* Проблема: False Sharing (Ваня рассказывал), 2 потока получили память рядом и тырят ее друг у друга одну и ту же линию кэша. Решение: выдавать каждому потоку свои суперблоки. Будут утечки памяти, поэтому будем выдавать каждому потоку свои суперблоки и иногда забирать их в общее хранилище, такое не должно особо тормозить, ибо обращение в общее хранилище с блокировкой на него операция достаточно редкая

**Thread-local Storage** (в билете нет, но пусть будет тут) // допилите плес

Сегметная адресация никуда не делась. Адрес = База + Смещение. У потоков память общая, а хотим не общую - при переключении потоков будем переключать сегмент, с которым работает.

**Virtual Memory Areas**: есть аллокатор в ядре, который отвечает за то, как будет работать `mmap`

* Ядру нужно найти X свободных страниц подряд
* Реализуется примерно как "средний" аллокатор (удобно завести дерево отрезок или дерево поисков).

[Статья на тему](http://www.makelinux.net/books/lkd2/ch14lev1sec2)
