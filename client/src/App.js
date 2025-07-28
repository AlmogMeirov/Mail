import { BrowserRouter, Routes, Route } from "react-router-dom";
import Register from "./pages/Register";
import Login from "./pages/Login";
import MailViewPage from "./pages/MailViewPage";
import LabelPage from "./pages/LabelPage";
import Sidebar from "./components/Sidebar";
import Layout from "./components/Layout";
import "./App.css";
function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login />} /> {/* Redirect to login by default */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* Main layout with sidebar */}
        <Route element={<Layout />}>
          <Route path="/label/:labelId" element={<LabelPage />} />
          <Route path="/mail/:id" element={<MailViewPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
