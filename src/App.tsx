import { Routes, Route } from "react-router-dom";
import Nav from "./components/Nav";
import ChatPage from "./pages/ChatPage";
import BookingPage from "./pages/BookingPage";

export default function App() {
  return (
    <div className="min-h-full flex flex-col">
      <Nav />
      <main className="flex-1">
        <Routes>
          <Route path="/" element={<ChatPage />} />
          <Route path="/booking" element={<BookingPage />} />
        </Routes>
      </main>
    </div>
  );
}
