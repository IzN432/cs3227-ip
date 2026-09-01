# Reflections on agentic coding

This document contains my reflections on coding with a coding agent, developing Ekko
the chatbot.

---

# 1. Ensuring the agent has the full context before proceeding

1. "okay sure let's do that. I'm thinking a shopping interface where you can
  log on as different users and perform either listing things to sell or buying
   things."

  2. "simple username, all stored in a data file similar to the existing one.
  the login wont be done via command, it will be done via button press. the app
   loads into a login screen and only once you are logged in will you see the
  command interface. on top of all this, users have money in their accounts,
  and buyer can do topup number to add money and sellers can do withdraw number
   to withdraw (all it does is add / remove money) cant go below 0 obviously.
  and all sellers are buyers but not all buyers are sellers. in order to become
   a seller, the buyer has to apply for seller status with a function like
  applytobecomeseller (we need to workshop that name). It should be
  automatically approved immediately. this time, the conversation history for
  each user is stored in the data files (we can have a file per user)"

  3. "there are two different sell types, BIN and AUCTION (I got these names
  from hypixel skyblock but im not sure if BIN means anything in the real
  world), so the commands can be bin <item name> /desc <item description>
  /price <price> and auction <item name> /desc <item description> /price
  <starting price> /end <date time> note that the time is mandatory. so once
  the time passes, or if the application is launched with the time already
  passed, the auction entry is processed to be finished, and the top bid goes
  to the seller and everyone else gets refunded their money except the top
  bidder. the command would be bid <uuid> /price <price>. Another thing to note
   is we will use a short UUID instead of the exact list number because I think
   it will look cooler. For bin, it is just direct purchase so it..."

  4. "the problem im facing right now is we cant reliably implement
  notifications when the application is not guaranteed to be running at all
  times and we dont have a central server so I can't think of any reasonable
  way to inform the winner that they have won... or perhaps on startup, the app
   combs through all auctions, checks if they are done, if they are done, it
  appends to the user's chat history the information of the thing. If not, it
  polls every second while the app is running for the same thing? then I guess
  that is the auction expiry process. help me to think about the bid holding
  thing. we deal with whole numbers only, the minimum bid increment can just be
   1. mylistings can show both and we can have more parameters to deal with
  whether to show active or not. uuid format can be 4 hex..."

  5. "just dont let people outbid themselves and we dont even have to think
  about that yeah. mylistings parameters looks good if it fits our code.
  auction display definitely has to display the top bid, end date time, number
  of bidders yeah. a non seller should see an error message suggesting
  becomeseller yes. username rules are just alphabet capital and lowercase"

  6. "add a new folder in ekko, call it listing. Listing should have item name
  and item description, both taken in on construction, and a state enum that
  goes between ACTIVE, SOLD, UNACTIVE. it should be inherited by Bin and
  Auction. Bin should have a price. Auction should have a base price and a
  highest Bid object. Bid should be another class that contains a User and a
  bidding price... While we are at it, add a users/ folder in ekko/ and it just
   contains the User class that should have a username, hashed password,
  isSeller status."

When I realised I read the specification wrong and had to pivot the project from the CS2103T ip to a new idea, I wanted to leave it mostly to the agent to perform the
pivot. So, I came up with the idea for the pivot, which was to move over to a marketplace app, then I discussed the functionalities with the agent.

I find that it is quite important to let the agent know the big picture before you begin any ground work, so that it can identify what needs to be put in each class in order to construct the entire functionality we are looking for.

Providing the agent with the full context also reduces the likelihood of it making locally reasonable changes that conflict with the overall design. In this case, requirements such as every seller also being a buyer, bids temporarily holding a user’s money, and expired auctions being processed when the application starts affect several parts of the system at once. They influence the user model, listing hierarchy, data-storage format, command design, and user interface. Discussing these interactions before implementing individual classes allowed the agent to understand not only what each class should contain, but also how the classes would eventually work together.

The conversation was also useful for refining ideas that were initially incomplete. For example, the lack of a continuously running server made real-time auction notifications impractical. By explaining this constraint, I could work with the agent on a more suitable approach: processing expired auctions at startup and periodically while the application is running, then recording the outcome in each affected user’s conversation history. Similarly, discussing edge cases such as preventing users from outbidding themselves, refunding unsuccessful bidders, enforcing a minimum bid increment, and preventing account balances from becoming negative helped establish clearer rules before coding began.

This experience showed me that effective use of an agent involves more than issuing isolated implementation instructions. The agent produces more coherent results when it is given the project’s goals, constraints, domain rules, and expected user experience upfront. Once that shared understanding was established, I could break the pivot into smaller implementation tasks such as creating the Listing, Bin, Auction, Bid, and User classes, while remaining confident that these components were being developed as parts of the same overall system rather than as disconnected features.

# 2. Initial suggestions can constrain the agent's thinking

One of my initial requirements was that the application should have a "simple username,
all stored in a data file similar to the existing one" and that "the conversation history
for each user is stored in the data files (we can have a file per user)." This prompt
established that users, listings, bids, and conversations all needed to persist between
sessions. The eventual design used shared files for users and listings because these are
marketplace-wide data, while each user had a separate conversation file so that their
history and notifications could be loaded and recovered independently.

An interesting issue arose when deciding how to name the conversation files. I initially
suggested using the username, and the agent followed that suggestion by proposing that
usernames should be restricted to characters that are safe in file names. This was a
locally reasonable solution, but it allowed a storage implementation detail to restrict
what users could call themselves. It would also have made the storage design depend on
operating-system-specific file-name rules. I caught this issue and suggested using each
user's UUID as the conversation file name instead. A UUID is already unique, does not
change, and can be made safe for use as a file name, while the username remains a
user-facing value rather than a storage identifier.

What stood out to me was that the agent did not initially challenge my suggestion to use
the username. Because I had proposed it, the agent treated it as a requirement and tried
to make it work instead of comparing it with better alternatives. This showed me that
giving an agent detailed direction can sometimes narrow its thinking too much. My own
suggestions should still be treated as design proposals, not automatically as the best
solution. In future prompts, I would explicitly ask the agent to identify drawbacks and
compare alternatives before implementing a design that affects persisted data.

I did not rely only on the apparent reasonableness of the generated persistence code. I
verified it with tests that serialized and deserialized users, listings, bids, and
conversation messages and checked that the original data was preserved. The tests also
covered malformed records, unsupported versions, missing files, Unicode text, delimiter
characters inside user-controlled fields, and replacing an existing saved file. Separate
conversation-storage tests checked that a damaged conversation could be replaced with a
recovery message without preventing the rest of the marketplace from loading.

These tests were important because persistence bugs can remain unnoticed until an
application restarts, at which point they may corrupt or lose data that appeared to save
successfully. The use of encoded fields and versioned records also made the format less
fragile, but those design choices only became trustworthy after round-trip and invalid-data
tests exercised them. Accepting the agent's implementation without this verification would
have risked tying usernames to the file system, mishandling special characters, or failing
the entire application because one user's conversation file was damaged.

# 3. Formatting matters

One of the first things we did in this project was update the chatbot's name from Duke.
In doing so, we were also required to update the ASCII art from that of Duke to that of
the newly chosen name, Ekko. This was simply a matter of using an online ASCII-art
generator, selecting a suitable font, and pasting it into the Codex chat.

There was a lingering thought at the back of my mind about whether it was meaningful to
use Codex to update the ASCII art instead of doing it myself. Doing it through Codex
meant sacrificing token usage for the convenience of not having to manually format the
plain-text art as Java code with newline characters and escaped backslashes.

When I first pasted the ASCII art into the Codex chat, it became malformed. Codex supports
Markdown formatting, where characters such as underscores can be interpreted as formatting
delimiters. This caused some of the underscores in the art to disappear. Interestingly,
Codex noticed that the ASCII art was malformed and generated its own replacement, although
the result was not to my liking.

The solution was to surround the ASCII art with triple backticks so that it was treated as
a plain-text code block rather than Markdown. Codex could then preserve the characters and
translate the art into a correctly escaped Java string.

The main takeaway from this prompt was that formatting is part of the prompt itself. Even
when the intended instruction is clear to a human, the interface may transform or interpret
the supplied text before the agent acts on it. For structured or character-sensitive input,
I should use an explicit code block and tell the agent that the contents must be preserved
exactly. In this case, manually editing the string may also have been faster, but the failed
attempt still taught me how prompt formatting affects the agent's understanding of input.
