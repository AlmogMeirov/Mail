// src/components/Topbar.jsx
import { useState, useEffect } from "react";
import LogoutButton from "./LogoutButton";
import { useSearch } from "../context/SearchContext";
import { useNavigate } from "react-router-dom";
import "./Topbar.css";

export default function Topbar() {
    const { setSearchQuery } = useSearch();
    const [query, setQuery] = useState("");
    const navigate = useNavigate();

    const [user, setUser] = useState(null);

    // --- Dark Mode state start ---
    const [theme, setTheme] = useState(() => localStorage.getItem("theme") || "light"); // Added for Dark Mode: keep current theme in state
    useEffect(() => {
        // Added for Dark Mode: reflect theme to <html data-theme> and persist
        document.documentElement.setAttribute("data-theme", theme);
        localStorage.setItem("theme", theme);
    }, [theme]); // Added for Dark Mode
    const toggleTheme = () => setTheme((t) => (t === "light" ? "dark" : "light")); // Added for Dark Mode: toggle handler
    // --- Dark Mode state end ---

    const token = localStorage.getItem("token");

    useEffect(() => {
        const fetchUser = async () => {
        try {
            const res = await fetch("/api/users/me", {
            headers: { Authorization: `Bearer ${token}` },
            });
            if (res.ok) {
            const data = await res.json();
            setUser(data);
            } else {
            console.error("Failed to fetch user");
            }
        } catch (err) {
            console.error("Error fetching user:", err);
        }
        };

        if (token) fetchUser();
    }, [token]);

    console.log("user in Topbar:", user);

    const handleKeyDown = (e) => {
        if (e.key === "Enter" && query.trim()) {
        const q = query.trim().toLowerCase();
        setSearchQuery(q);
        navigate(`/search?q=${encodeURIComponent(q)}`);
        }
    };

    return (
        <div className="topbar">
        <div className="topbar-left">
            <input
            type="text"
            placeholder="Search mail"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            className="search-bar"
            />
        </div>

        <div className="topbar-right">
            {/* Added for Dark Mode: theme toggle button in top bar */}
            <button
              className="theme-toggle" // Added for Dark Mode: styled in Topbar.css using CSS variables
              onClick={toggleTheme}     // Added for Dark Mode
              title={theme === "light" ? "Enable dark theme" : "Disable dark theme"} // Added for Dark Mode
              aria-label="Toggle theme" // Added for Dark Mode
            >
              {theme === "light" ? "🌙" : "☀️" /* Added for Dark Mode: simple Gmail-like icon */ }
            </button>

            <span className="user-name">
            {user?.firstName} {user?.lastName}
            </span>
            <img
            src={
                user?.profileImage?.startsWith("data:image")
                ? user.profileImage
                : user?.profileImage || "/user-svgrepo-com.svg"
            }
            alt="User avatar"
            className="avatar"
            />
            <LogoutButton />
        </div>
        </div>
    );
}
