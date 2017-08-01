## Mathlogic homework

Code is in `src/main/java`. 

Tests are in `src/test`. Better use `-Xms4G` VM argument when running tests, 
otherwise it may refuse to parse deduction result of `correct11` test (120k lines are heavy
and may result in many calls to the garbage collector which will slow everything down a lot).

Input-output is done via `io/<task_name>/input.txt` and `io/<task_name>/output.txt`.

Hand-made proofs are in `proofs/<task_name>/`.