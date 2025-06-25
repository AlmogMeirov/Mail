// src/pages/InboxPage.jsx
import React, { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import SearchBar from "../components/SearchBar"; // Search bar component branch309

function InboxPage() {
  const [mails, setMails] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();// Search parameters for query branch309

  // Helper: fetch mails (all or by query) branch309
  const fetchMails = async (query = null) => {
    setLoading(true);
    try {
      const url = query
        ? `/api/mails/search?q=${encodeURIComponent(query)}`
        : "/api/mails";

      const response = await fetch(url, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
      });

      if (!response.ok) throw new Error("Failed to fetch mails");

      const data = await response.json();
      if (Array.isArray(data.recent_mails)) {
        setMails(data.recent_mails);
      } else if (Array.isArray(data)) {
        setMails(data);
      } else {
        setMails([]);
      }
    } catch (err) {
      console.error("Fetch error:", err);
      setMails([]);
    } finally {
      setLoading(false);
    }
  };

  // On page load, fetch inbox or perform search
  useEffect(() => {
    const query = searchParams.get("q");
    fetchMails(query);
  }, []);

  // When user triggers search
  const handleSearch = async (query) => {
    const trimmed = query.trim();
    if (!trimmed) {
      alert("Empty search query – skipping request.");
      return;
    }

    /*
    const isValid = /^[\p{L}\p{N}\s@.?!'"-]+$/u.test(trimmed);
    if (!isValid) {
      alert("Search query contains invalid characters.");
      return;
    }
    */

    // Update the URL with query
    if (searchParams.get("q") !== trimmed) {
      setSearchParams({ q: trimmed });
    }

    await fetchMails(trimmed);
  };

  return (
    <div style={{ padding: "1rem" }}>
      <SearchBar onSearch={handleSearch} /> {/*  Search bar component branch309 */}
      <h1>Inbox</h1>
      {loading ? (
        <p>Loading...</p>
      ) : mails.length === 0 ? (
        <p>No mails found.</p>
      ) : (
        <ul style={{ listStyle: "none", padding: 0 }}>
          {mails.map((mail) => (
            <li
              key={mail.id}
              onClick={() => navigate(`/mail/${mail.id}`)}
              style={{
                cursor: "pointer",
                border: "1px solid #ccc",
                padding: "1rem",
                marginBottom: "1rem",
                borderRadius: "8px",
                backgroundColor: "#f9f9f9",
              }}
            >
              <strong>{mail.direction === "sent" ? "To" : "From"}:</strong>{" "}
              {mail.otherParty?.firstName
                ? `${mail.otherParty.firstName} ${mail.otherParty.lastName}`
                : mail.otherParty?.email || "(unknown)"}{" "}
              <br />
              <strong>Subject:</strong>{" "}
              {mail.subject || <em>(no subject)</em>} <br />
              <strong>Date:</strong>{" "}
              {new Date(mail.timestamp).toLocaleString()} <br />
              <p style={{ color: "#666" }}>
                {mail.preview || <em>(no content)</em>}
              </p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default InboxPage;
