# mini-agent-sameer
A simple and responsive AI chat UI built using React and TailwindCSS. The chat simulates a Mini-Agent behavior with memory, basic calculation, and typing delay using a mock backend function. Perfect demonstration of state management, UI components, and async logic handling.

# Mini-Agent Chat UI

This project is a simple, responsive chat interface built using **React** and **TailwindCSS**, designed to simulate the experience of interacting with a lightweight AI agent. It includes message history, typing indication, and a mock backend function that handles basic logic like remembering user input and performing simple calculations.

---

## 🚀 Features

- **Clean, responsive chat UI**
- **User and Agent message bubbles** with left/right alignment
- **Typing indicator** while the agent is "thinking"
- **Mock backend** to simulate agent responses
- **Short-term memory** (e.g., “Remember my cat's name is Fluffy”)
- **Simple calculator ability** (e.g., “What is 10 + 5?”)
- **Custom color theme** using TailwindCSS
- **Agent profile avatar** in header

---

## 🛠️ Tech Stack

| Tool / Library | Purpose |
|----------------|---------|
| React          | UI and component structure |
| TailwindCSS (CDN) | Styling with utility classes |
| TypeScript     | Type safety (optional use) |
| Vite           | Fast development server and build |

---

## 🧠 Mock Backend Logic

The function `mockAgentResponse(prompt)` simulates how a backend agent might respond.  
It includes:

- 1-second artificial delay (`setTimeout`) to mimic thinking
- Basic **calculator** tool
- **Memory save & recall**
- Simple conversational responses ("hello", "help")

Example:
```js
if (prompt.includes("Remember my cat's name is Fluffy")) {
  memory["cat's name"] = "Fluffy";
}
