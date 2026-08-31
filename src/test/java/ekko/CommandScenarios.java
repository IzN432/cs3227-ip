package ekko;

import java.util.List;

/**
 * Independently maintained command scenarios for Ekko's JUnit integration tests.
 * These expectations are owned by the test suite, not loaded from documentation.
 */
final class CommandScenarios {
    private CommandScenarios() {
    }

    /**
     * Returns command inputs and their expected normalized responses.
     */
    static List<Scenario> all() {
        return List.of(
                new Scenario("AGENDA-01",
                        "agenda 2/12/2019\n"
                                + "bye\n",
                        "Your scheduled obligations on Dec 02 2019:\n"
                                + "1.[D][ ] return book (by: Dec 02 2019, 6:00 PM)\n"
                                + "2.[E][ ] conference (from: Dec 01 2019, 9:00 AM to: Dec 03 2019, 5:00 PM)\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        "T | 0 | undated task\n"
                                + "D | 0 | return book | 2019-12-02T18:00\n"
                                + "E | 0 | conference | 2019-12-01T09:00 | 2019-12-03T17:00"),
                new Scenario("AGENDA-02",
                        "agenda 2019-12-03\n"
                                + "bye\n",
                        "Nothing scheduled for Dec 03 2019. Try not to make a habit of it.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        "D | 0 | return book | 2019-12-02T18:00"),
                new Scenario("AGENDA-03",
                        "agenda\n"
                                + "bye\n",
                        "Please provide a date for the agenda.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("AGENDA-04",
                        "agenda 2019-02-29\n"
                                + "bye\n",
                        "Please use a valid date such as 2019-10-15 or 2/12/2019.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("BLANK-01",
                        "\n"
                                + "bye\n",
                        "Please enter a command.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("BYE-01",
                        "bye\n",
                        "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("DEADLINE-01",
                        "deadline return book /by 2/12/2019 1800\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [D][ ] return book (by: Dec 02 2019, 6:00 PM)\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("DEADLINE-02",
                        "deadline /by Sunday\n"
                                + "bye\n",
                        "The description of a deadline cannot be empty.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("DEADLINE-03",
                        "deadline return book\n"
                                + "bye\n",
                        "A deadline must have a non-empty /by argument.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("DEADLINE-04",
                        "deadline return book /by\n"
                                + "bye\n",
                        "A deadline must have a non-empty /by argument.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("DEADLINE-05",
                        "deadline read /about Java /by 2019-12-02\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [D][ ] read /about Java (by: Dec 02 2019)\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("DEADLINE-06",
                        "deadline return book /by 2019-02-29\n"
                                + "bye\n",
                        "Please use a valid date/time such as 2019-10-15 or 2/12/2019 1800.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("DELETE-01",
                        "todo first task\n"
                                + "todo second task\n"
                                + "todo third task\n"
                                + "delete 2\n"
                                + "list\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [T][ ] first task\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [T][ ] second task\n"
                                + "Now you have 2 tasks in the list.\n"
                                + "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [T][ ] third task\n"
                                + "Now you have 3 tasks in the list.\n"
                                + "Removed from your agenda. Your responsibilities may disagree:\n"
                                + "  [T][ ] second task\n"
                                + "Now you have 2 tasks in the list.\n"
                                + "Human memory is unreliable. Fortunately, I kept a list:\n"
                                + "1.[T][ ] first task\n"
                                + "2.[T][ ] third task\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("DELETE-02",
                        "todo borrow book\n"
                                + "delete 0\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [T][ ] borrow book\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "Please input a valid task number. "
                                + "You can send list to see how many tasks you have.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("DELETE-03",
                        "todo borrow book\n"
                                + "delete 2\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [T][ ] borrow book\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "Please input a valid task number. "
                                + "You can send list to see how many tasks you have.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("DELETE-04",
                        "delete first\n"
                                + "bye\n",
                        "Please provide a valid task number.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("DELETE-05",
                        "delete\n"
                                + "bye\n",
                        "Please provide a task number.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("EVENT-01",
                        "event project meeting /from 2019-12-02 1400 /to 2019-12-03 1600\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [E][ ] project meeting (from: Dec 02 2019, 2:00 PM to: Dec 03 2019, 4:00 PM)\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("EVENT-02",
                        "event /from 2pm /to 4pm\n"
                                + "bye\n",
                        "The description of an event cannot be empty.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("EVENT-03",
                        "event meeting /to 4pm\n"
                                + "bye\n",
                        "An event must have a non-empty /from argument.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("EVENT-04",
                        "event meeting /from /to 4pm\n"
                                + "bye\n",
                        "An event must have a non-empty /from argument.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("EVENT-05",
                        "event meeting /from 2pm\n"
                                + "bye\n",
                        "An event must have a non-empty /to argument.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("EVENT-06",
                        "event meeting /from 2pm /to\n"
                                + "bye\n",
                        "An event must have a non-empty /to argument.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("EVENT-07",
                        "event project /form discussion /from 2019-12-02 1400 /to 2019-12-02 1600\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [E][ ] project /form discussion "
                                + "(from: Dec 02 2019, 2:00 PM to: Dec 02 2019, 4:00 PM)\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("EVENT-08",
                        "event meeting /from 2019-12-03 1400 /to 2019-12-02 1600\n"
                                + "bye\n",
                        "An event's /to date/time cannot be before its /from date/time.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("LIST-01",
                        "list\n"
                                + "bye\n",
                        "Nothing on your agenda. I will assume this is an achievement.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("LIST-02",
                        "todo borrow book\n"
                                + "deadline return book /by 2019-12-02\n"
                                + "event meeting /from 2019-12-02 1400 /to 2019-12-02 1600\n"
                                + "list\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [T][ ] borrow book\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [D][ ] return book (by: Dec 02 2019)\n"
                                + "Now you have 2 tasks in the list.\n"
                                + "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [E][ ] meeting (from: Dec 02 2019, 2:00 PM to: Dec 02 2019, 4:00 PM)\n"
                                + "Now you have 3 tasks in the list.\n"
                                + "Human memory is unreliable. Fortunately, I kept a list:\n"
                                + "1.[T][ ] borrow book\n"
                                + "2.[D][ ] return book (by: Dec 02 2019)\n"
                                + "3.[E][ ] meeting (from: Dec 02 2019, 2:00 PM to: Dec 02 2019, 4:00 PM)\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("MARK-01",
                        "todo borrow book\n"
                                + "mark 1\n"
                                + "list\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [T][ ] borrow book\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "One puny task completed. Only your whole life left to go.\n"
                                + "  [T][X] borrow book\n"
                                + "Human memory is unreliable. Fortunately, I kept a list:\n"
                                + "1.[T][X] borrow book\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("MARK-02",
                        "todo borrow book\n"
                                + "mark 0\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [T][ ] borrow book\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "Please input a valid task number. "
                                + "You can send list to see how many tasks you have.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("MARK-03",
                        "todo borrow book\n"
                                + "mark 2\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [T][ ] borrow book\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "Please input a valid task number. "
                                + "You can send list to see how many tasks you have.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("MARK-04",
                        "mark first\n"
                                + "bye\n",
                        "Please provide a valid task number.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("MARK-05",
                        "mark\n"
                                + "bye\n",
                        "Please provide a task number.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("MARK-06",
                        "todo borrow book\n"
                                + "mark 1\n"
                                + "mark 1\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [T][ ] borrow book\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "One puny task completed. Only your whole life left to go.\n"
                                + "  [T][X] borrow book\n"
                                + "This task has already been marked as done:\n"
                                + "  [T][X] borrow book\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("LOAD-01",
                        "list\n"
                                + "bye\n",
                        "Human memory is unreliable. Fortunately, I kept a list:\n"
                                + "1.[T][X] read book\n"
                                + "2.[D][ ] return book (by: Dec 02 2019)\n"
                                + "3.[E][ ] project meeting (from: Dec 02 2019, 2:00 PM to: Dec 03 2019, 4:00 PM)\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        "T | 1 | read book\n"
                                + "D | 0 | return book | 2019-12-02T00:00\n"
                                + "E | 0 | project meeting | 2019-12-02T14:00 | 2019-12-03T16:00"),
                new Scenario("LOAD-02",
                        "yes\n"
                                + "bye\n",
                        "The stored task data is invalid. Delete the data file? (y/n)\n"
                                + "The invalid data file was deleted. Ekko will start with an empty task list.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        "D | 0 | return book | not-a-date"),
                new Scenario("LOAD-03",
                        "n\n",
                        "The stored task data is invalid. Delete the data file? (y/n)\n"
                                + "The data file was kept. Ekko will now exit.",
                        "invalid task record"),
                new Scenario("TODO-01",
                        "todo borrow book\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [T][ ] borrow book\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("TODO-02",
                        "todo\n"
                                + "bye\n",
                        "The description of a todo cannot be empty.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("TODO-03",
                        "todo read /about Java\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [T][ ] read /about Java\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("UNKNOWN-01",
                        "remember borrow book\n"
                                + "bye\n",
                        "Unknown command. A command reference has been provided. Use it.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("UNMARK-01",
                        "todo borrow book\n"
                                + "mark 1\n"
                                + "unmark 1\n"
                                + "list\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [T][ ] borrow book\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "One puny task completed. Only your whole life left to go.\n"
                                + "  [T][X] borrow book\n"
                                + "Premature optimism detected. Back on the agenda:\n"
                                + "  [T][ ] borrow book\n"
                                + "Human memory is unreliable. Fortunately, I kept a list:\n"
                                + "1.[T][ ] borrow book\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("UNMARK-02",
                        "todo borrow book\n"
                                + "unmark 1\n"
                                + "bye\n",
                        "Added to your agenda. Your memory has been relieved of duty:\n"
                                + "  [T][ ] borrow book\n"
                                + "Now you have 1 tasks in the list.\n"
                                + "This task has already been unmarked:\n"
                                + "  [T][ ] borrow book\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("UNMARK-03",
                        "unmark 1\n"
                                + "bye\n",
                        "Please input a valid task number. You can send list to see how many tasks you have.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("UNMARK-04",
                        "unmark first\n"
                                + "bye\n",
                        "Please provide a valid task number.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null),
                new Scenario("UNMARK-05",
                        "unmark\n"
                                + "bye\n",
                        "Please provide a task number.\n"
                                + "Ekko offline. You are briefly responsible for yourself.",
                        null)
        );
    }

    /**
     * Describes one isolated application run.
     *
     * @param name descriptive identifier shown by JUnit.
     * @param input lines sent to standard input, including the final newline.
     * @param expected expected command responses without startup chrome.
     * @param initialData initial data-file contents, or null for a fresh installation.
     */
    record Scenario(String name, String input, String expected, String initialData) {
    }
}
