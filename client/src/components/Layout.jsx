// client/src/components/Layout.jsx

import React from "react";
import Sidebar from "./Sidebar"; // Sidebar component for navigation
import Topbar from "./Topbar"; // Added by Meir in exercise 4
import { SearchProvider } from "../context/SearchContext"; // Context provider for search functionality, added by Meir in exercise 4
import { Outlet } from "react-router-dom";
import "../App.css"; // Importing styles for the layout

/*const Layout = ({ children }) => {
  return (
    <div className="app-layout">
      <Sidebar />
      <main className="main-content">
        {children}
        <Outlet />
      </main>
    </div>
  );
};*/

const Layout = () => {
  return (
    <SearchProvider>
      <div className="app-layout">
        <Sidebar />
        <div className="main-content">
          <Topbar />
          <Outlet />
        </div>
      </div>
    </SearchProvider>
  );
};

export default Layout;
