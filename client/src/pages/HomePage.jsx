// src/pages/HomePage.jsx
import { useNavigate } from "react-router-dom";
import "../styles/HomePage.css";
import { useEffect, useState } from "react";


const HomePage = () => {
    const navigate = useNavigate();

    return (
        <div className="home-container">
            <div className="logo-area">
                <img src="/logo-light.png" alt="Gmail Logo" className="logo-image" />
            </div>
            <div className="home-card">
                <h1 className="home-title">Gmail Mail</h1>
                <p className="home-subtitle">
                    Secure, minimalist email system built for developers.
                </p>

                <div className="home-buttons">
                    <button onClick={() => navigate("/login")} className="btn primary">
                        Login
                    </button>
                    <button onClick={() => navigate("/register")} className="btn secondary">
                        Register
                    </button>
                </div>

                <p className="home-motto">
                    "Designed for developers. Inspired by simplicity."
                </p>
            </div>
        </div>
    );
};

export default HomePage;
