// client/src/components/Layout.jsx

import React from "react";
import Sidebar from "./Sidebar"; // Sidebar component for navigation
import { Outlet } from "react-router-dom";
import "../App.css"; // Importing styles for the layout

const Layout = ({ children }) => {
  return (
    <div className="app-layout">
      <Sidebar />
      <main className="main-content">
        {children}
        <Outlet />
      </main>
    </div>
  );
};

export default Layout;
