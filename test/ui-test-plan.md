# Ekko UI test plan

This plan verifies the command-line behaviour implemented in the working tree. Each test starts Ekko in a fresh process, so task state does not carry between tests.

The runner compiles Java sources recursively beneath `src/main/java` and launches the `ekko.Ekko` main class. Package organization does not change the command inputs, expected outputs, or test-design reasons below.

## Output comparison

The automated runner removes the fixed startup banner, greeting, blank lines, and 80-character separators. It compares all remaining lines exactly, including task numbering, status markers, descriptions, date/time text, and confirmation or error messages. The special input line `<blank>` represents pressing Enter without typing anything.

An optional **Initial data file** block provides the contents of `data/ekko.txt` before Ekko starts. When omitted, neither the data directory nor the data file exists for that test.

The cases are sorted alphabetically by command under test.

## `agenda`

### AGENDA-01 — Show dated tasks occurring on a date

**Reason:** Category-partition testing verifies deadline matching, multi-day event overlap, todo exclusion, and filtered numbering.

**Initial data file**

```text
T | 0 | undated task
D | 0 | return book | 2019-12-02T18:00
E | 0 | conference | 2019-12-01T09:00 | 2019-12-03T17:00
```

**Input**

```text
agenda 2/12/2019
bye
```

**Expected normalized output**

```text
Here are the deadlines and events on Dec 02 2019:
1.[D][ ] return book (by: Dec 02 2019, 6:00 PM)
2.[E][ ] conference (from: Dec 01 2019, 9:00 AM to: Dec 03 2019, 5:00 PM)
Bye. Hope to see you again soon!
```

### AGENDA-02 — Report no tasks on a date

**Reason:** Boundary testing covers an empty filtered result when dated tasks exist only on other dates.

**Initial data file**

```text
D | 0 | return book | 2019-12-02T18:00
```

**Input**

```text
agenda 2019-12-03
bye
```

**Expected normalized output**

```text
No deadlines or events found on Dec 03 2019.
Bye. Hope to see you again soon!
```

### AGENDA-03 — Reject a missing date

**Reason:** Boundary value analysis at the minimum agenda argument length of zero.

**Input**

```text
agenda
bye
```

**Expected normalized output**

```text
Please provide a date for the agenda.
Bye. Hope to see you again soon!
```

### AGENDA-04 — Reject an invalid date

**Reason:** Robustness testing verifies that an impossible calendar date does not run the query.

**Input**

```text
agenda 2019-02-29
bye
```

**Expected normalized output**

```text
Please use a valid date such as 2019-10-15 or 2/12/2019.
Bye. Hope to see you again soon!
```

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
deadline return book /by 2/12/2019 1800
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [D][ ] return book (by: Dec 02 2019, 6:00 PM)
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
deadline read /about Java /by 2019-12-02
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [D][ ] read /about Java (by: Dec 02 2019)
Now you have 1 tasks in the list.
Bye. Hope to see you again soon!
```

### DEADLINE-06 — Reject an invalid calendar date

**Reason:** Robustness testing verifies that syntactically plausible but impossible dates are rejected.

**Input**

```text
deadline return book /by 2019-02-29
bye
```

**Expected normalized output**

```text
Please use a valid date/time such as 2019-10-15 or 2/12/2019 1800.
Bye. Hope to see you again soon!
```

## `delete`

### DELETE-01 — Delete a task and renumber the remaining tasks

**Reason:** State-transition testing of removal from the middle of the list, including the resulting task count and numbering.

**Input**

```text
todo first task
todo second task
todo third task
delete 2
list
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [T][ ] first task
Now you have 1 tasks in the list.
Got it. I've added this task:
  [T][ ] second task
Now you have 2 tasks in the list.
Got it. I've added this task:
  [T][ ] third task
Now you have 3 tasks in the list.
Noted. I've removed this task:
  [T][ ] second task
Now you have 2 tasks in the list.
Here are the tasks in your list:
1.[T][ ] first task
2.[T][ ] third task
Bye. Hope to see you again soon!
```

### DELETE-02 — Reject task number zero

**Reason:** Boundary value analysis immediately below the lowest valid one-based task number.

**Input**

```text
todo borrow book
delete 0
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

### DELETE-03 — Reject a task number above the current count

**Reason:** Boundary value analysis immediately above the highest valid task number.

**Input**

```text
todo borrow book
delete 2
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

### DELETE-04 — Reject a nonnumeric task number

**Reason:** Equivalence partitioning for arguments outside the integer input domain.

**Input**

```text
delete first
bye
```

**Expected normalized output**

```text
Please provide a valid task number.
Bye. Hope to see you again soon!
```

### DELETE-05 — Reject a missing task number

**Reason:** Boundary value analysis at an argument length of zero.

**Input**

```text
delete
bye
```

**Expected normalized output**

```text
Please provide a task number.
Bye. Hope to see you again soon!
```

## `event`

### EVENT-01 — Add a valid event with multiword values

**Reason:** Positive-path equivalence partition covering values that contain spaces.

**Input**

```text
event project meeting /from 2019-12-02 1400 /to 2019-12-03 1600
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [E][ ] project meeting (from: Dec 02 2019, 2:00 PM to: Dec 03 2019, 4:00 PM)
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
event project /form discussion /from 2019-12-02 1400 /to 2019-12-02 1600
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [E][ ] project /form discussion (from: Dec 02 2019, 2:00 PM to: Dec 02 2019, 4:00 PM)
Now you have 1 tasks in the list.
Bye. Hope to see you again soon!
```

### EVENT-08 — Reject an end before the start

**Reason:** Boundary-order testing checks the invalid interval where the end precedes the start.

**Input**

```text
event meeting /from 2019-12-03 1400 /to 2019-12-02 1600
bye
```

**Expected normalized output**

```text
An event's /to date/time cannot be before its /from date/time.
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
deadline return book /by 2019-12-02
event meeting /from 2019-12-02 1400 /to 2019-12-02 1600
list
bye
```

**Expected normalized output**

```text
Got it. I've added this task:
  [T][ ] borrow book
Now you have 1 tasks in the list.
Got it. I've added this task:
  [D][ ] return book (by: Dec 02 2019)
Now you have 2 tasks in the list.
Got it. I've added this task:
  [E][ ] meeting (from: Dec 02 2019, 2:00 PM to: Dec 02 2019, 4:00 PM)
Now you have 3 tasks in the list.
Here are the tasks in your list:
1.[T][ ] borrow book
2.[D][ ] return book (by: Dec 02 2019)
3.[E][ ] meeting (from: Dec 02 2019, 2:00 PM to: Dec 02 2019, 4:00 PM)
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
Here are the tasks in your list:
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

## Startup loading

### LOAD-01 — Load saved tasks when Ekko starts

**Reason:** State-restoration testing across all task types and both completion states.

**Initial data file**

```text
T | 1 | read book
D | 0 | return book | 2019-12-02T00:00
E | 0 | project meeting | 2019-12-02T14:00 | 2019-12-03T16:00
```

**Input**

```text
list
bye
```

**Expected normalized output**

```text
Here are the tasks in your list:
1.[T][X] read book
2.[D][ ] return book (by: Dec 02 2019)
3.[E][ ] project meeting (from: Dec 02 2019, 2:00 PM to: Dec 03 2019, 4:00 PM)
Bye. Hope to see you again soon!
```

### LOAD-02 — Delete malformed stored data after confirmation

**Reason:** Decision-table testing covers the affirmative recovery path for an unreadable task record.

**Initial data file**

```text
D | 0 | return book | not-a-date
```

**Input**

```text
yes
bye
```

**Expected normalized output**

```text
The stored task data is invalid. Delete the data file? (y/n)
The invalid data file was deleted. Ekko will start with an empty task list.
Bye. Hope to see you again soon!
```

### LOAD-03 — Keep malformed stored data after declining

**Reason:** Decision-table testing covers a non-affirmative response and verifies the safe, non-destructive path.

**Initial data file**

```text
invalid task record
```

**Input**

```text
n
```

**Expected normalized output**

```text
The stored task data is invalid. Delete the data file? (y/n)
The data file was kept. Ekko will now exit.
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
Here are the tasks in your list:
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
