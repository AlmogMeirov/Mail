import { BrowserRouter, Routes, Route } from "react-router-dom";
import InboxPage from "./pages/InboxPage";
import MailViewPage from "./pages/MailViewPage";
import Register from "./pages/Register";
import Login from "./pages/Login";


// Add in exercises 4 for layout and navigation
import SentPage from "./pages/SentPage"; 
import LabelPage from "./pages/LabelPage";
import Sidebar from "./components/Sidebar"; 
import './App.css'; 
import Layout from "./components/Layout"; 


function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        {/* Add in exercises 4 - Protected routes under Layout */}
        <Route element={<Layout />}>
          <Route path="/inbox" element={<InboxPage />} />
          <Route path="/sent" element={<SentPage />} />
          <Route path="/mail/:id" element={<MailViewPage />} />
          <Route path="/label/:labelId" element={<LabelPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;