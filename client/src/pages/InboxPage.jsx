// src/pages/InboxPage.jsx
import React, { useEffect, useState } from "react";
import SendMailComponent from "../components/SendMailComponent";
import { useNavigate, useSearchParams } from "react-router-dom";
import SearchBar from "../components/SearchBar";

function InboxPage() {
  const [mails, setMails] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showComponent, setShowComponent] = useState(false);
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // Helper: fetch mails (all or by query)
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

  // On first load
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

    // Optional: validate query
    // const isValid = /^[\p{L}\p{N}\s@.?!'"-]+$/u.test(trimmed);
    // if (!isValid) {
    //   alert("Search query contains invalid characters.");
    //   return;
    // }

    if (searchParams.get("q") !== trimmed) {
      setSearchParams({ q: trimmed });
    }

    await fetchMails(trimmed);
  };

  return (
    <div style={{ padding: "1rem" }}>
      <SearchBar onSearch={handleSearch} />
      <h1>Inbox</h1>

      <button onClick={() => setShowComponent(true)}>Send Mail</button>
      {showComponent && (
        <>
          <div
            style={{
              position: "fixed",
              top: 0,
              left: 0,
              width: "100vw",
              height: "100vh",
              backgroundColor: "rgba(0, 0, 0, 0.5)",
              zIndex: 998,
            }}
            onClick={() => setShowComponent(false)}
          />
          <SendMailComponent onClose={() => setShowComponent(false)} />
        </>
      )}

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
                : mail.otherParty?.email ||
                (mail.direction === "sent" ? mail.recipient : mail.sender) ||
                "(unknown)"}
              <br />
              <strong>Subject:</strong>{" "}
              {mail.subject || <em>(no subject)</em>} <br />
              <strong>Date:</strong>{" "}
              {new Date(mail.timestamp).toLocaleString()} <br />
              <p style={{ color: "#666" }}>
                {mail.preview || mail.content?.slice(0, 100) || (
                  <em>(no content)</em>
                )}
              </p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default InboxPage;
