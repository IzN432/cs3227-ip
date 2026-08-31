# Ekko User Guide

Ekko is your task-management chatbot. Keep track of to-dos,
deadlines, and events by typing short commands. Your memory can take a break.

## Quick start

1. Install **Java 25** and check it with `java -version` in a terminal.
2. Place the bundled application, `duke.jar`, in a folder where you want to keep your tasks.
3. Open a terminal in that folder and run `java -jar duke.jar`.
4. In the Ekko window, type `todo read a book` into the bottom field and press **Enter**
   or click **Send**. Wait for the reply, then try `list`.

Input is briefly disabled while Ekko processes a command; wait for the reply before sending another.

## Command basics

- Use lowercase command words and type one command at a time.
- Replace placeholders such as `DESCRIPTION`, `DATE`, and `NUMBER` with your own values.
  Do not add quotation marks around descriptions.
- Descriptions are required and cannot contain `|` or embedded newlines.
- Put descriptions before slash arguments. Supply each required `/by`, `/from`, or `/to` exactly once.
- `list` and `bye` take no arguments.

### Dates and times

| Input | Example |
| --- | --- |
| Date: `YYYY-MM-DD` | `2026-09-02` |
| Day-first date: `D/M/YYYY` | `2/9/2026` |
| Either date followed by a 24-hour time, `HHmm` | `2026-09-02 1800` or `2/9/2026 1800` |
| ISO date and time | `2026-09-02T18:00` |

Dates must exist in the calendar. A date without a time means **midnight at the start
of that day**, not the end of the day. Ekko displays dates like `Sep 02 2026` and
non-midnight times like `Sep 02 2026, 6:00 PM`.

## Features

### Add a to-do: `todo`

Adds a task without a date.

**Format:** `todo DESCRIPTION`

**Example:** `todo read a book`

Ekko confirms the new task and shows the updated task count.

### Add a deadline: `deadline`

Adds a task with a due date and optional time.

**Format:** `deadline DESCRIPTION /by DATE_TIME`

**Example:** `deadline submit report /by 2026-09-02 1800`

The report is due on September 2, 2026 at 6:00 PM. You can omit the time if you only need a date.

### Add an event: `event`

Adds a task with a start and end. The end must be strictly after the start.

**Format:** `event DESCRIPTION /from DATE_TIME /to DATE_TIME`

**Example:** `event team meeting /from 2026-09-02 1400 /to 2026-09-02 1500`

This schedules a meeting from 2:00 PM to 3:00 PM. Events can span multiple days;
include a date in both endpoints.

> **Duplicates:** Ekko rejects tasks with the same type, description, and schedule,
> even if the existing task is completed. Description matching is case-sensitive and
> ignores spaces at the edges. A different type or schedule is allowed.

### View all tasks: `list`

**Format:** `list`

Shows completed and incomplete tasks in the order they were added, numbered from 1.
For example, after adding the three tasks above:

```text
1.[T][ ] read a book
2.[D][ ] submit report (by: Sep 02 2026, 6:00 PM)
3.[E][ ] team meeting (from: Sep 02 2026, 2:00 PM to: Sep 02 2026, 3:00 PM)
```

`[T]`, `[D]`, and `[E]` mean to-do, deadline, and event. `[ ]` means incomplete;
`[X]` means completed. An empty list produces a message instead.

### Mark or unmark a task: `mark`, `unmark`

**Formats:** `mark NUMBER` and `unmark NUMBER`

**Examples:** `mark 1` marks the first task as completed; `unmark 1` makes it incomplete again.
Ekko shows the updated task. Marking does not remove it from the list.

### Find tasks: `find`

Searches descriptions for an exact, **case-sensitive substring or phrase**.

**Format:** `find KEYWORD_OR_PHRASE`

**Examples:** `find book` matches `read a book` but not `Book tickets`;
`find team meeting` searches for that whole phrase.

Dates, task types, and completion markers are not searched. Both completed and incomplete
matches appear in their original order. Ekko tells you if nothing matches.

### View a day's schedule: `agenda`

**Format:** `agenda DATE`

**Example:** `agenda 2026-09-02`

Shows deadlines due that day and events spanning that day, including both the start
and end dates. Completed tasks are included; to-dos are not. Results retain task-list
order rather than being sorted by time. Use a date without a time.

> **Task numbers:** `find` and `agenda` number their results from 1 but do not change
> the full list. Always run `list` to get the correct number before using `mark`,
> `unmark`, or `delete`. Use a whole number from 1 to the number of tasks in that list.

### Delete a task: `delete`

**Format:** `delete NUMBER`

**Example:** `delete 2` removes the second task in the full list and shows the remaining count.
Deletion is immediate, with no undo command. Later task numbers shift, so run `list`
again before deleting another task.

### End the session: `bye`

**Format:** `bye`

Ekko displays a farewell and stops accepting commands. Close the window to exit.

## Saving and troubleshooting

Tasks are saved automatically after changes and loaded when Ekko starts. There is no
save command. The file is `data/ekko.txt`, relative to the folder you launch Ekko from.
Launch from the same folder each time to keep using the same task list. To back up your
tasks, close Ekko and copy this file somewhere safe.

- **Invalid command:** Read Ekko's error, check the format above, and try again.
- **Invalid saved data:** Ekko asks whether to delete the file. Choose **No** (or dismiss
  the dialog) to preserve it and stop the session. Back it up before attempting repairs.
  Choosing **Yes** deletes the stored tasks and starts an empty list.
- **File-access or save error:** Commands are disabled and changes may not have been saved.
  Check that the data folder is writable, resolve the reported problem, and restart Ekko.
