import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { fetchWithAuth, moveMailToLabel } from "../utils/api";
import SearchBar from "../components/SearchBar";
import LogoutButton from "../components/LogoutButton";
import SendMailComponent from "../components/SendMailComponent";

const LabelPage = () => {
  const { labelId } = useParams();
  const [mails, setMails] = useState([]);
  const [labelName, setLabelName] = useState("");
  const [allLabels, setAllLabels] = useState([]);
  const [error, setError] = useState("");
  const [showComposer, setShowComposer] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const navigate = useNavigate();
  const token = localStorage.getItem("token");

  useEffect(() => {
    if (!token) return;

    if (labelId === "inbox" || labelId === "sent") {
      setLabelName(labelId);
    } else {
      fetchWithAuth(`/labels/${labelId}`, token)
        .then(data => setLabelName(data?.name || labelId))
        .catch(() => setLabelName(labelId));
    }

    const fetchMails = async () => {
      try {
        if (labelId === "inbox" || labelId === "sent") {
          const response = await fetch("/api/mails", {
            headers: { Authorization: `Bearer ${token}` },
          });
          const data = await response.json();

          const inboxList = Array.isArray(data?.inbox)
            ? data.inbox
            : Array.isArray(data?.recent_mails)
              ? data.recent_mails
              : [];

          const sentList = Array.isArray(data?.sent) ? data.sent : [];

          let list = [];
          if (labelId === "inbox") {
            list = inboxList;
          } else if (labelId === "sent") {
            list = sentList;
          } else {
            setError("Unsupported label ID");
            return;
          }

          setMails(list);
        } else {
          const ids = await fetchWithAuth(`/labels/by-label/${labelId}`, token);
          const fullMails = await Promise.all(
            ids.map(id => fetchWithAuth(`/mails/${id}`, token))
          );
          setMails(fullMails);
        }
      } catch (err) {
        console.error("Fetch error:", err);
        setError("Failed to load mail data");
      }
    };

    fetchMails();

    fetchWithAuth("/labels", token)
      .then(setAllLabels)
      .catch(console.error);
  }, [labelId, token]);

  const handleMove = async (mailId, toLabelId) => {
    try {
      await moveMailToLabel(mailId, labelId, toLabelId, token);
      window.location.reload();
    } catch (err) {
      alert("Failed to move mail.");
      console.error(err);
    }
  };

  const renderSender = (mail) => {
    const sender = mail.sender;

    if (sender) {
      if (typeof sender === "string") return sender;
      if (sender.email) return sender.email;
      if (sender.firstName || sender.lastName)
        return `${sender.firstName || ""} ${sender.lastName || ""}`.trim();
    }

    const other = mail.otherParty;
    if (other) {
      if (typeof other === "string") return other;
      if (other.email) return other.email;
      if (other.firstName || other.lastName)
        return `${other.firstName || ""} ${other.lastName || ""}`.trim();
    }

    return "(unknown)";
  };

  const handleSearch = (query) => {
    setSearchQuery(query.trim().toLowerCase());
  };

  const filteredMails = searchQuery
    ? mails.filter(mail =>
        (mail.subject || "").toLowerCase().includes(searchQuery) ||
        (mail.content || "").toLowerCase().includes(searchQuery) ||
        renderSender(mail).toLowerCase().includes(searchQuery)
      )
    : mails;

  return (
    <div style={{ padding: "1rem" }}>
      {labelId === "inbox" && (
        <>
          <SearchBar onSearch={handleSearch} />
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <h1>{labelName}</h1>
            <LogoutButton />
          </div>
          <button onClick={() => setShowComposer(true)}>Send Mail</button>
          {showComposer && (
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
                onClick={() => setShowComposer(false)}
              />
              <SendMailComponent onClose={() => setShowComposer(false)} />
            </>
          )}
        </>
      )}

      {labelId !== "inbox" && <h1>{labelName}</h1>}
      {error && <p style={{ color: "red" }}>{error}</p>}

      {filteredMails.length === 0 ? (
        <p>No mails found.</p>
      ) : (
        <ul style={{ listStyle: "none", padding: 0 }}>
          {filteredMails.map((mail) => (
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
              <strong>From:</strong> {renderSender(mail)} <br />
              <strong>Subject:</strong> {mail.subject || <em>(no subject)</em>} <br />
              <strong>Date:</strong>{" "}
              {mail.timestamp
                ? new Date(mail.timestamp).toLocaleString()
                : "Invalid Date"}{" "}
              <br />
              <p style={{ color: "#666" }}>
                {mail.preview || mail.content?.slice(0, 100) || (
                  <em>(no content)</em>
                )}
              </p>

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
                {allLabels
                  .filter((lbl) => lbl.id !== labelId)
                  .map((label) => (
                    <option key={label.id} value={label.id}>
                      {label.name}
                    </option>
                  ))}
              </select>

              {Array.isArray(mail.labels) && mail.labels.length > 0 && (
                <div className="mail-labels">
                  {mail.labels.map((label) => (
                    <span key={label} className="mail-label">
                      {label}
                    </span>
                  ))}
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default LabelPage;
