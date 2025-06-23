import { BrowserRouter, Routes, Route } from "react-router-dom";
import InboxPage from "./pages/InboxPage";
import MailViewPage from "./pages/MailViewPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/inbox" element={<InboxPage />} />
        <Route path="/mail/:id" element={<MailViewPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;