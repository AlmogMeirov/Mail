import { FaTrash } from "react-icons/fa";
import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { fetchWithAuth, moveMailToLabel } from "../utils/api";
//import SearchBar from "../components/SearchBar";
//import LogoutButton from "../components/LogoutButton";
//import Topbar from "../components/Topbar";
import { useSearch } from "../context/SearchContext";

const LabelPage = () => {
  const { labelId } = useParams();
  const [mails, setMails] = useState([]);
  const [labelName, setLabelName] = useState("");
  //const [userEmail, setUserEmail] = useState("");
  const [allLabels, setAllLabels] = useState([]);
  const [error, setError] = useState("");
  //const [showComposer, setShowComposer] = useState(false);
  //const [searchQuery, setSearchQuery] = useState("");
  const { searchQuery, setSearchQuery } = useSearch();
  const [selectedLabelMap, setSelectedLabelMap] = useState({});
  const navigate = useNavigate();
  const token = localStorage.getItem("token");

  useEffect(() => {
    setSearchQuery(""); // Clear search query when entering label page
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

          let list = labelId === "inbox" ? inboxList : sentList;
          setMails(list);
        } else {
          // Defensive check in case response is null or empty
          const ids = await fetchWithAuth(`/labels/by-label/${labelId}`, token);
          if (!Array.isArray(ids) || ids.length === 0) {
            setMails([]);
          } else {
            const fullMails = await Promise.all(
              ids.map(async (id) => {
                try {
                  const mail = await fetchWithAuth(`/mails/${id}`, token);
                  return mail || null;
                } catch (err) {
                  console.warn(`Failed to fetch mail ${id}:`, err);
                  return null;
                }
              })
            );
            setMails(fullMails.filter(m => m !== null));
          }
        }
      } catch (err) {
        console.error("Fetch error:", err);
        setError("Failed to load mail data");
      }
    };

    fetchMails();
    fetchWithAuth("/labels", token).then(setAllLabels).catch(console.error);
  }, [labelId, token]);

  const handleMove = async (mailId, toLabelIds) => {
    try {
      for (const toLabelId of toLabelIds) {
        await moveMailToLabel(mailId, labelId, toLabelId, token);
      }
      window.location.reload();
    } catch (err) {
      alert("Failed to move mail.");
      console.error(err);
    }
  };

  const renderSender = (mail) => {
    const sender = mail.sender || mail.otherParty;
    if (!sender) return "(unknown)";
    if (typeof sender === "string") return sender;
    return sender.email || `${sender.firstName || ""} ${sender.lastName || ""}`.trim();
  };

  /*const handleSearch = (query) => {
    setSearchQuery(query.trim().toLowerCase());
  };*/

  /*const filteredMails = searchQuery
    ? mails.filter(mail =>
        (mail.subject || "").toLowerCase().includes(searchQuery) ||
        (mail.content || "").toLowerCase().includes(searchQuery) ||
        renderSender(mail).toLowerCase().includes(searchQuery)
      )
    : mails;*/

  const normalize = (text) => (text || "").toString().trim().toLowerCase();

  const filteredMails = mails.filter((mail) => {
    const search = normalize(searchQuery);

    const sender = typeof mail.sender === "string"
      ? normalize(mail.sender)
      : normalize(mail.sender?.email || "");

    const recipient = typeof mail.recipient === "string"
      ? normalize(mail.recipient)
      : normalize(mail.recipient?.email || "");

    const subject = normalize(mail.subject);
    const content = normalize(mail.content);

    if (search === "") return true;

    return (
      subject.includes(search) ||
      content.includes(search) ||
      sender.includes(search) ||
      recipient.includes(search)
    );
  });



  return (
    <div style={{ padding: "1rem" }}>
      {labelId === "inbox" && (
        <>
          {/*<Topbar onSearch={handleSearch} />
          <SearchBar onSearch={handleSearch} />*/}
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <h1>{labelName}</h1>
            {/*<LogoutButton />*/}
          </div>
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
              <strong>Date:</strong> {mail.timestamp ? new Date(mail.timestamp).toLocaleString() : "Invalid Date"}<br />
              <p style={{ color: "#666" }}>{mail.preview || mail.content?.slice(0, 100) || <em>(no content)</em>}</p>

              <details onClick={(e) => e.stopPropagation()} style={{ marginBottom: "0.5rem" }}>
                <summary style={{ cursor: "pointer", color: "#174ea6", fontWeight: "500", marginBottom: "0.3rem" }}>
                  Tag as:
                </summary>
                <div style={{ padding: "0.5rem 0", display: "flex", flexWrap: "wrap", gap: "0.5rem" }}>
                  {allLabels.filter(lbl => lbl.id !== labelId).map((label) => (
                    <label
                      key={label.id}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        backgroundColor: "#f1f3f4",
                        padding: "4px 10px",
                        borderRadius: "16px",
                        border: "1px solid #ccc",
                        fontSize: "0.85rem",
                        cursor: "pointer"
                      }}
                    >
                      <input
                        type="checkbox"
                        style={{ marginRight: "6px" }}
                        onChange={(e) => {
                          e.stopPropagation();
                          setSelectedLabelMap(prev => {
                            const mailLabels = new Set(prev[mail.id] || []);
                            e.target.checked ? mailLabels.add(label.id) : mailLabels.delete(label.id);
                            return { ...prev, [mail.id]: Array.from(mailLabels) };
                          });
                        }}
                        checked={selectedLabelMap[mail.id]?.includes(label.id) || false}
                      />
                      {label.name}
                    </label>
                  ))}
                </div>
              </details>
              
              <button
                className="trash-button"
                onClick={(e) => {
                  e.stopPropagation();
                  const trashLabel = allLabels.find(l => l.name.toLowerCase() === "trash");
                  if (trashLabel) {
                    handleMove(mail.id, [trashLabel.id]);
                  } else {
                    alert("Trash label not found");
                  }
                }}
                title="Move to Trash"
                style={{
                  marginLeft: "0.5rem",
                  backgroundColor: "#f8d7da",
                  color: "#721c24",
                  border: "1px solid #f5c6cb",
                  borderRadius: "4px",
                  padding: "4px 8px",
                  cursor: "pointer"
                }}
              >
                <FaTrash />
              </button>
              <button
                className="move-button"
                onClick={(e) => {
                  e.stopPropagation();
                  const selected = selectedLabelMap[mail.id] || [];
                  handleMove(mail.id, selected);
                }}
              >
                Move
              </button>

              {labelName.toLowerCase() === "trash" && (
                <button
                  className="delete-button"
                  onClick={async (e) => {
                    e.stopPropagation();
                    try {
                      await fetch(`/api/mails/${mail.id}`, {
                        method: "DELETE",
                        headers: {
                          Authorization: `Bearer ${token}`,
                        },
                      });
                      window.location.reload();
                    } catch (err) {
                      console.error("Failed to delete mail:", err);
                      alert("Failed to delete mail.");
                    }
                  }}
                  style={{
                    marginLeft: "0.5rem",
                    backgroundColor: "#dc3545",
                    color: "white",
                    border: "none",
                    borderRadius: "4px",
                    padding: "4px 8px",
                    cursor: "pointer"
                  }}
                >
                  Delete Forever
                </button>
              )}

              {Array.isArray(mail.labels) && mail.labels.length > 0 && (
                <div className="mail-labels">
                  {mail.labels.map((label) => (
                    <span key={label} className="mail-label">{label}</span>
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
