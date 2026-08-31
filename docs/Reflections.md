# Reflections on agentic coding

This document contains my reflections on coding with a coding agent, developing Ekko
the chatbot.

---

# 1. Formatting matters

One of the first things we do in this project is update the chatbot's name from Duke. 
In doing so, we are also required to update the ASCII art from that of Duke to that
of the new chosen name, Ekko in my case. This was simply a matter of using an online
ASCII art generator, selecting a nice looking font, and pasting it into the Codex chat.

There was a lingering thought at the back of my mind of whether it was meaningful to
use Codex to update the ASCII art as opposed to doing it myself. Doing it through Codex
meant sacrificing token usage for ease of replacement, since I did not have to manually
format the ASCII art, which is provided as plaintext, into code with newlines and 
appropriate escaping of the special backslash character.

When first pasted into the Codex terminal, the ASCII art got malformed. This was because
Codex supports some markdown formatting such as using underscores as italics delimiters.
This caused some underscores to vanish from the art.

Interestingly, Codex actually caught that the ASCII art was malformed, and proceeded to 
generate its own, albeit not to my liking, ASCII art of the name Ekko.

The solution to the issue was to surround the input ASCII art text with ```, which treats
the contained text as plaintext, thus evading the markdown conversion attempts.

The main takeaway from this prompt is that the formatting of the prompt dictates how the
agent parses it and answers you.

# 2. For vaguer topics, demonstrate with clear examples

When trying to craft a personality for the chatbot, I first asked Codex for some ideas
on text for a unique personality. Then, I took aspects of each of Codex's output to
construct my chatbot's unique personality. While there are adjectives that could have
been used to describe the chatbot's eventual robotic sarcasm, I chose not to tell the
model what personality I wanted the chatbot to display, but rather I chose to demonstrate
with examples.

Personality as described in adjectives has a fuzzy connection to the eventual output
strings. What abstract adjectives cannot convey is **tone**, **rhythm**, and **degree**. 
"Sarcastic" covers a wide range, from mild irony to outright hostility. The example phrases 
pinned the exact register I had in mind. The AI then used those examples as anchors when 
generating the remaining responses, keeping the personality consistent across all ten commands.

The takeaway from this prompt is that when you have a specific vision in mind, don't be
afraid to just give the AI examples to guide its output better than a few adjectives can.

# 3. Be aware of AI's heedless compliance

One section of the assignment had us asking the agent for possible code quality improvements in
iterations, asking for the next iteration after each code update. For the first few iterations,
the agent proposed genuinely meaningful code quality updates, which I hence accepted easily.
However, after 6 rounds, Codex proposed naming the field-position constants used in task
deserialization (`fields[2], fields[3]`, etc.). This looked to me like it was grasping for straws,
and I took another look at the prompt.

At no point in the prompt did I give Codex a stopping condition. I was simply asking it to move on
to the next iteration.

Rather than just rejecting the proposal outright, I instead asked Codex directly: *"do you really
think that is a genuine improvement or should we just stop here?"* Codex agreed to stop, and
acknowledged that the change offered a small readability benefit but that the code was already
short, well-tested, and unlikely to be a source of future bugs.

This brings us to the takeaway that agents tend to comply mindlessly with what you prompt them, so
it is important to be clear in your specifications, or to keep an eye out for the agent straying
from your requirements when prompting. Asking the agent to justify its recommendation — or to
evaluate whether a recommendation is worth making at all — is a useful tool to have.