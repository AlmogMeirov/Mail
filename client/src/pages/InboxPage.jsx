// src/pages/InboxPage.jsx
import React, { useEffect, useState } from "react";
import SendMailComponent from "../components/SendMailComponent";
import { useNavigate } from "react-router-dom";

function InboxPage() {
  const [mails, setMails] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const [showComponent, setShowComponent] = useState(false);
  
  useEffect(() => {
    const fetchMails = async () => {
      try {
        const response = await fetch("/api/mails", {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
        });

        if (!response.ok) {
          throw new Error("Failed to fetch mails");
        }

        const data = await response.json();
        setMails(Array.isArray(data.recent_mails) ? data.recent_mails : []);
        console.log("Mail object example:", data.recent_mails[0]);
      } catch (err) {
        console.error("Error fetching mails:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchMails();
  }, []);

  if (loading) return <p>Loading...</p>;

  if (mails.length === 0) return <p>No mails in your inbox yet.</p>;

  return (
    <div style={{ padding: "1rem" }}>
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
                zIndex: 998
            }}
            onClick={() => setShowComponent(false)}
            />
            <SendMailComponent onClose={() => setShowComponent(false)} />
        </>
     )}
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
        : mail.otherParty?.email || "(unknown)"} <br />
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
    </div>
  );
}

export default InboxPage;
