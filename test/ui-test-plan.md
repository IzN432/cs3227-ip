# Ekko UI test plan

This plan verifies the command-line behaviour implemented in the working tree. Each test starts Ekko in a fresh process, so task state does not carry between tests.

## Output comparison

The automated runner removes the fixed startup banner, greeting, blank lines, and 80-character separators. It compares all remaining lines exactly, including task numbering, status markers, descriptions, date/time text, and confirmation or error messages. The special input line `<blank>` represents pressing Enter without typing anything.

The cases are sorted alphabetically by command under test.

## Blank input

### BLANK-01 — Reject an empty command

**Reason:** Equivalence partitioning for the invalid empty-input partition.

**Input**

```text
<blank>
bye
```

**Expected normalized output**

```text
Please enter a command.
Bye. Hope to see you again soon!
```

## `bye`

### BYE-01 — Exit immediately

**Reason:** State-transition testing of the application's terminating command from its initial state.

**Input**

```text
bye
```

**Expected normalized output**

```text
Bye. Hope to see you again soon!
```

## `deadline`

### DEADLINE-01 — Add a valid deadline

**Reason:** Positive-path equivalence partition for a description and a non-empty `/by` value.

**Input**

```text
deadline return book /by Sunday
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [D][ ] return book (by: Sunday)
Now you have 1 tasks in the list.
Bye. Hope to see you again soon!
```

### DEADLINE-02 — Reject a missing description

**Reason:** Boundary value analysis at the minimum description length of zero.

**Input**

```text
deadline /by Sunday
bye
```

**Expected normalized output**

```text
The description of a deadline cannot be empty.
Bye. Hope to see you again soon!
```

### DEADLINE-03 — Reject a missing `/by` argument

**Reason:** Equivalence partitioning for syntactically absent required arguments.

**Input**

```text
deadline return book
bye
```

**Expected normalized output**

```text
A deadline must have a non-empty /by argument.
Bye. Hope to see you again soon!
```

### DEADLINE-04 — Reject an empty `/by` value

**Reason:** Boundary value analysis at the minimum `/by` value length of zero.

**Input**

```text
deadline return book /by
bye
```

**Expected normalized output**

```text
A deadline must have a non-empty /by argument.
Bye. Hope to see you again soon!
```

### DEADLINE-05 — Preserve an unrecognised slash word

**Reason:** Syntax discrimination testing to ensure only explicitly allowed slash arguments are parsed.

**Input**

```text
deadline read /about Java /by Sunday
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [D][ ] read /about Java (by: Sunday)
Now you have 1 tasks in the list.
Bye. Hope to see you again soon!
```

## `event`

### EVENT-01 — Add a valid event with multiword values

**Reason:** Positive-path equivalence partition covering values that contain spaces.

**Input**

```text
event project meeting /from Mon 2pm /to Tue 4pm
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [E][ ] project meeting (from: Mon 2pm to: Tue 4pm)
Now you have 1 tasks in the list.
Bye. Hope to see you again soon!
```

### EVENT-02 — Reject a missing description

**Reason:** Boundary value analysis at the minimum description length of zero.

**Input**

```text
event /from 2pm /to 4pm
bye
```

**Expected normalized output**

```text
The description of an event cannot be empty.
Bye. Hope to see you again soon!
```

### EVENT-03 — Reject a missing `/from` argument

**Reason:** Equivalence partitioning for an absent required start value.

**Input**

```text
event meeting /to 4pm
bye
```

**Expected normalized output**

```text
An event must have a non-empty /from argument.
Bye. Hope to see you again soon!
```

### EVENT-04 — Reject an empty `/from` value

**Reason:** Boundary value analysis at the minimum `/from` value length of zero.

**Input**

```text
event meeting /from /to 4pm
bye
```

**Expected normalized output**

```text
An event must have a non-empty /from argument.
Bye. Hope to see you again soon!
```

### EVENT-05 — Reject a missing `/to` argument

**Reason:** Equivalence partitioning for an absent required end value.

**Input**

```text
event meeting /from 2pm
bye
```

**Expected normalized output**

```text
An event must have a non-empty /to argument.
Bye. Hope to see you again soon!
```

### EVENT-06 — Reject an empty `/to` value

**Reason:** Boundary value analysis at the minimum `/to` value length of zero.

**Input**

```text
event meeting /from 2pm /to
bye
```

**Expected normalized output**

```text
An event must have a non-empty /to argument.
Bye. Hope to see you again soon!
```

### EVENT-07 — Preserve an unrecognised slash word

**Reason:** Syntax discrimination testing of the allowed-argument set for events.

**Input**

```text
event project /form discussion /from 2pm /to 4pm
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [E][ ] project /form discussion (from: 2pm to: 4pm)
Now you have 1 tasks in the list.
Bye. Hope to see you again soon!
```

## `list`

### LIST-01 — List an empty task collection

**Reason:** Boundary value analysis at the minimum task count of zero.

**Input**

```text
list
bye
```

**Expected normalized output**

```text
No tasks found!
Bye. Hope to see you again soon!
```

### LIST-02 — List every task type in insertion order

**Reason:** Category partitioning across Todo, Deadline, and Event, including their type and status formatting.

**Input**

```text
todo borrow book
deadline return book /by Sunday
event meeting /from 2pm /to 4pm
list
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [T][ ] borrow book
Now you have 1 tasks in the list.
Got it. I've added this task:
  [D][ ] return book (by: Sunday)
Now you have 2 tasks in the list.
Got it. I've added this task:
  [E][ ] meeting (from: 2pm to: 4pm)
Now you have 3 tasks in the list.
1.[T][ ] borrow book
2.[D][ ] return book (by: Sunday)
3.[E][ ] meeting (from: 2pm to: 4pm)
Bye. Hope to see you again soon!
```

## `mark`

### MARK-01 — Mark the first task

**Reason:** Boundary value analysis at the lowest valid one-based task number.

**Input**

```text
todo borrow book
mark 1
list
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [T][ ] borrow book
Now you have 1 tasks in the list.
Nice! I've marked this task as done:
  [T][X] borrow book
1.[T][X] borrow book
Bye. Hope to see you again soon!
```

### MARK-02 — Reject task number zero

**Reason:** Boundary value analysis immediately below the lowest valid task number.

**Input**

```text
todo borrow book
mark 0
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [T][ ] borrow book
Now you have 1 tasks in the list.
Please input a valid task number. You can send list to see how many tasks you have.
Bye. Hope to see you again soon!
```

### MARK-03 — Reject a task number above the current count

**Reason:** Boundary value analysis immediately above the highest valid task number.

**Input**

```text
todo borrow book
mark 2
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [T][ ] borrow book
Now you have 1 tasks in the list.
Please input a valid task number. You can send list to see how many tasks you have.
Bye. Hope to see you again soon!
```

### MARK-04 — Reject a nonnumeric task number

**Reason:** Equivalence partitioning for arguments outside the integer input domain.

**Input**

```text
mark first
bye
```

**Expected normalized output**

```text
Please provide a valid task number.
Bye. Hope to see you again soon!
```

### MARK-05 — Reject a missing task number

**Reason:** Boundary value analysis at an argument length of zero.

**Input**

```text
mark
bye
```

**Expected normalized output**

```text
Please provide a task number.
Bye. Hope to see you again soon!
```

### MARK-06 — Report an already marked task

**Reason:** State-transition testing of the repeated mark operation in the marked state.

**Input**

```text
todo borrow book
mark 1
mark 1
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [T][ ] borrow book
Now you have 1 tasks in the list.
Nice! I've marked this task as done:
  [T][X] borrow book
This task has already been marked as done:
  [T][X] borrow book
Bye. Hope to see you again soon!
```

## `todo`

### TODO-01 — Add a valid todo

**Reason:** Positive-path equivalence partition at the simplest valid task type.

**Input**

```text
todo borrow book
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [T][ ] borrow book
Now you have 1 tasks in the list.
Bye. Hope to see you again soon!
```

### TODO-02 — Reject a missing description

**Reason:** Boundary value analysis at the minimum description length of zero.

**Input**

```text
todo
bye
```

**Expected normalized output**

```text
The description of a todo cannot be empty.
Bye. Hope to see you again soon!
```

### TODO-03 — Preserve slash-prefixed text

**Reason:** Syntax discrimination testing because todos define no slash arguments.

**Input**

```text
todo read /about Java
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [T][ ] read /about Java
Now you have 1 tasks in the list.
Bye. Hope to see you again soon!
```

## `unknown`

### UNKNOWN-01 — Reject an unknown command

**Reason:** Equivalence partitioning for command words outside the supported command set.

**Input**

```text
remember borrow book
bye
```

**Expected normalized output**

```text
I don't recognise that command.
Bye. Hope to see you again soon!
```

## `unmark`

### UNMARK-01 — Unmark a marked task

**Reason:** State-transition testing from unmarked to marked and back to unmarked.

**Input**

```text
todo borrow book
mark 1
unmark 1
list
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [T][ ] borrow book
Now you have 1 tasks in the list.
Nice! I've marked this task as done:
  [T][X] borrow book
Okay, I've unmarked this task as not done yet:
  [T][ ] borrow book
1.[T][ ] borrow book
Bye. Hope to see you again soon!
```

### UNMARK-02 — Report an already unmarked task

**Reason:** State-transition testing of the unmark operation in the initial unmarked state.

**Input**

```text
todo borrow book
unmark 1
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [T][ ] borrow book
Now you have 1 tasks in the list.
This task has already been unmarked:
  [T][ ] borrow book
Bye. Hope to see you again soon!
```

### UNMARK-03 — Reject an out-of-range task number

**Reason:** Boundary value analysis when no task number is valid because the task count is zero.

**Input**

```text
unmark 1
bye
```

**Expected normalized output**

```text
Please input a valid task number. You can send list to see how many tasks you have.
Bye. Hope to see you again soon!
```

### UNMARK-04 — Reject a nonnumeric task number

**Reason:** Equivalence partitioning for arguments outside the integer input domain.

**Input**

```text
unmark first
bye
```

**Expected normalized output**

```text
Please provide a valid task number.
Bye. Hope to see you again soon!
```

### UNMARK-05 — Reject a missing task number

**Reason:** Boundary value analysis at an argument length of zero.

**Input**

```text
unmark
bye
```

**Expected normalized output**

```text
Please provide a task number.
Bye. Hope to see you again soon!
```
