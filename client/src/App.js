import { BrowserRouter, Routes, Route } from "react-router-dom";
import Register from "./pages/Register";
import Login from "./pages/Login";
import HomePage from "./pages/HomePage";
import MailViewPage from "./pages/MailViewPage";
import { SendMailProvider } from "./context/SendMailContext";
import LabelPage from "./pages/LabelPage";
//import Sidebar from "./components/Sidebar";
import SearchResultsPage from "./pages/SearchResultsPage";
import DraftEditPage from "./pages/DraftEditPage";
import Layout from "./components/Layout";
import "./App.css";

function App() {
  return (
    <SendMailProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* Main layout with sidebar */}
          <Route element={<Layout />}>
            <Route path="/label/:labelId" element={<LabelPage />} />
            <Route path="/mail/:id" element={<MailViewPage />} />
            <Route path="/search" element={<SearchResultsPage />} />
            <Route path="/draft/:id" element={<DraftEditPage />} />

          </Route>
        </Routes>
      </BrowserRouter>
    </SendMailProvider>
  );
}

export default App;
