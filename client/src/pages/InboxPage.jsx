// src/pages/InboxPage.jsx
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { fetchWithAuth, moveMailToLabel } from "../utils/api"; // Add in exercises 4 - for moving mails to labels

function InboxPage() {
  const [mails, setMails] = useState([]);
  const [loading, setLoading] = useState(true);
  const [allLabels, setAllLabels] = useState([]);// Add in exercises 4 - for moving mails to labels
  const navigate = useNavigate();
   const token = localStorage.getItem("token");// Add in exercises 4 - for moving mails to labels

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
    // Add in exercises 4 - fetch mails and labels
    fetchMails();
    fetchWithAuth("/labels", token)
      .then(setAllLabels)
      .catch(console.error);

  }, []);

  
// Add in exercises 4 - move mail to label
  const handleMove = async (mailId, toLabelId) => {
    try {
      await moveMailToLabel(mailId, "inbox", toLabelId, token); // from inbox
      setMails((prev) => prev.filter((m) => m.id !== mailId)); // remove mail from view
    } catch (err) {
      console.error("Failed to move mail:", err);
      alert("Failed to move mail.");
    }
  };

  if (loading) return <p>Loading...</p>;
  if (mails.length === 0) return <p>No mails in your inbox yet.</p>;

  return (
    <div className="mail-content-area">
      <h1>Inbox</h1>
      <ul className="mail-list">
        {mails.map((mail) => (
          <li
            key={mail.id}
            className="mail-item"
            onClick={() => navigate(`/mail/${mail.id}`)}
            onMouseEnter={(e) => e.currentTarget.style.backgroundColor = "#eef"}
            onMouseLeave={(e) => e.currentTarget.style.backgroundColor = "#f9f9f9"}
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
            <p className="mail-preview">
              {mail.preview || <em>(no content)</em>}
            </p>

            {/* ★ dropdown to move mail to another label */}
            <label>Move to:</label>{" "}
            <select
              defaultValue=""
              onClick={(e) => e.stopPropagation()}
              onChange={(e) => {
                if (e.target.value) {
                  handleMove(mail.id, e.target.value);
                }
              }}
            >
              <option value="" disabled>Select label</option>
              {allLabels.map((label) => (
                <option key={label.id} value={label.id}>
                  {label.name}
                </option>
              ))}
            </select>

            {Array.isArray(mail.labels) && mail.labels.length > 0 && (
              <div className="mail-labels">
                {mail.labels.map((label) => (
                  <span
                    key={label}
                    className="mail-label"
                  >
                    {label}
                  </span>
                ))}
              </div>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
};

export default InboxPage;
