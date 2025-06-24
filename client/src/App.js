import { BrowserRouter, Routes, Route } from "react-router-dom";
import InboxPage from "./pages/InboxPage";
import MailViewPage from "./pages/MailViewPage";
import Register from "./pages/Register";
import Login from "./pages/Login";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/inbox" element={<InboxPage />} />
        <Route path="/mail/:id" element={<MailViewPage />} />
        <Route path="/register" element={<Register />} />
        <Route path="/login" element={<Login />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;