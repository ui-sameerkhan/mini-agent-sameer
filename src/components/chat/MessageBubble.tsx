export interface ChatMessage {
  id: string;
  role: "user" | "agent";
  text: string;
}

export default function MessageBubble({ message }: { message: ChatMessage }) {
  const isUser = message.role === "user";
  return (
    <div className={`flex ${isUser ? "justify-end" : "justify-start"}`}>
      <div
        className={`max-w-[75%] whitespace-pre-wrap rounded-2xl px-4 py-2 text-sm shadow-sm ${
          isUser
            ? "bg-brand-600 text-white rounded-br-sm"
            : "bg-white text-slate-800 border border-slate-200 rounded-bl-sm"
        }`}
      >
        {message.text}
      </div>
    </div>
  );
}
