// client/src/pages/SearchResultsPage.jsx

import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { fetchWithAuth } from "../utils/api";
import Loading from "../components/Loading";

const SearchResultsPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const query = new URLSearchParams(location.search).get("q") || "";
  const [mails, setMails] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  //const [error, setError] = useState("");

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token || !query) return;

    setIsLoading(true);
    fetchWithAuth(`/mails/search?q=${encodeURIComponent(query)}`, token)
      .then((results) => {
        setMails(results);
        console.log("Search results from server:", results);
      })
      .catch((err) => {
        console.error(err);
        setMails([]);
        //setError("Failed to load search results");
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [query]);

  return (
    <div style={{ padding: "1rem" }}>
      <h1>Search Results for: "{query}"</h1>
      {isLoading ? (
        <Loading label="Searching…" />
      ) : mails.length === 0 ? (
        <p>No matching mails found.</p>
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
              <strong>From:</strong>{" "}
              {typeof mail.sender === "string"
                ? mail.sender
                : mail.sender?.email || "(unknown)"}{" "}
              <br />
              <strong>Subject:</strong>{" "}
              {mail.subject || <em>(no subject)</em>} <br />
              <strong>Date:</strong>{" "}
              {mail.timestamp
                ? new Date(mail.timestamp).toLocaleString()
                : "Invalid Date"}
              <p style={{ color: "#666" }}>
                {mail.preview || mail.content?.slice(0, 100) || <em>(no content)</em>}
              </p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default SearchResultsPage;
