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
  const [currentMailLabels, setCurrentMailLabels] = useState({});
  const [openLabelManagement, setOpenLabelManagement] = useState({});
  const [pendingLabelChanges, setPendingLabelChanges] = useState({});
  const navigate = useNavigate();
  const token = localStorage.getItem("token");

  useEffect(() => {
    setSearchQuery(""); // Clear search query when entering label page
    if (!token) return;

    // Set the display name
    if (labelId === "inbox" || labelId === "sent") {
      setLabelName(labelId);
    } else {
      fetchWithAuth(`/labels/${labelId}`, token)
        .then(data => setLabelName(data?.name || labelId))
        .catch(() => setLabelName(labelId));
    }

    const fetchMails = async () => {
      try {
        // Use the same label-based approach for all labels, including inbox and sent
        const ids = await fetchWithAuth(`/labels/by-label/${labelId}`, token);
        
        let validMails = [];
        
        if (!Array.isArray(ids) || ids.length === 0) {
          // For sent emails, fallback to /api/mails if no label-based results
          if (labelId === "sent") {
            try {
              const response = await fetch("/api/mails", {
                headers: { Authorization: `Bearer ${token}` },
              });
              const data = await response.json();
              const sentList = Array.isArray(data?.sent) ? data.sent : [];
              
              // Filter out emails that are in trash
              const filteredSentList = [];
              for (const mail of sentList) {
                try {
                  const res = await fetch(`/api/labels/mail/${mail.id}`, {
                    headers: { Authorization: `Bearer ${token}` }
                  });
                  const labels = await res.json();
                  const labelNames = await Promise.all(
                    labels.map(async (labelId) => {
                      try {
                        const labelData = await fetchWithAuth(`/labels/${labelId}`, token);
                        return labelData?.name?.toLowerCase() || '';
                      } catch (err) {
                        return '';
                      }
                    })
                  );
                  
                  // Only include if not in trash
                  if (!labelNames.includes('trash')) {
                    filteredSentList.push(mail);
                  }
                } catch (err) {
                  // If we can't fetch labels, include the mail (safer default)
                  filteredSentList.push(mail);
                }
              }
              
              validMails = filteredSentList;
            } catch (fallbackErr) {
              console.warn("Fallback fetch failed:", fallbackErr);
            }
          }
          
          if (validMails.length === 0) {
            setMails([]);
          }
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
          validMails = fullMails.filter(m => m !== null);
        }
        
        setMails(validMails);
        
        // Fetch current labels for each mail
        const mailLabelsMap = {};
        for (const mail of validMails) {
          try {
            const res = await fetch(`/api/labels/mail/${mail.id}`, {
              headers: { Authorization: `Bearer ${token}` }
            });
            const labels = await res.json();
            mailLabelsMap[mail.id] = Array.isArray(labels) ? labels : [];
          } catch (err) {
            console.warn(`Failed to fetch labels for mail ${mail.id}:`, err);
            mailLabelsMap[mail.id] = [];
          }
        }
        setCurrentMailLabels(mailLabelsMap);
      } catch (err) {
        console.error("Fetch error:", err);
        setError("Failed to load mail data");
      }
    };

    fetchMails();
    fetchWithAuth("/labels", token).then(setAllLabels).catch(console.error);
  }, [labelId, token, setSearchQuery]);

  // Apply multiple label changes for a mail
  const handleMoveToLabels = async (mailId) => {
    try {
      const selectedLabels = pendingLabelChanges[mailId] || [];
      const currentLabels = currentMailLabels[mailId] || [];
      
      // Add new labels
      for (const labelId of selectedLabels) {
        if (!currentLabels.includes(labelId)) {
          await fetch("/api/labels/tag", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`
            },
            body: JSON.stringify({ mailId, labelId })
          });
        }
      }

      // Remove labels that are no longer selected
      for (const labelId of currentLabels) {
        if (!selectedLabels.includes(labelId)) {
          await fetch("/api/labels/untag", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`
            },
            body: JSON.stringify({ mailId, labelId })
          });
        }
      }

      // Clear pending changes for this mail
      setPendingLabelChanges(prev => {
        const newPending = { ...prev };
        delete newPending[mailId];
        return newPending;
      });

      // Refresh the page to show updated results
      window.location.reload();
    } catch (err) {
      console.error("Error applying label changes:", err);
      alert("Failed to update labels.");
    }
  };

  // Remove all labels and set only 'Trash'
  const moveMailToTrashOnly = async (mailId) => {
    try {
      const trashLabel = allLabels.find(l => l.name.toLowerCase() === "trash");
      if (!trashLabel) {
        alert("Trash label not found");
        return;
      }

      // Get current labels for this mail
      const res = await fetch(`/api/labels/mail/${mailId}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      const currentLabels = await res.json();

      // Remove all current labels
      await Promise.all(
        currentLabels.map(labelId =>
          fetch(`/api/labels/untag`, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`
            },
            body: JSON.stringify({ mailId, labelId })
          })
        )
      );

      // Tag only Trash
      await fetch(`/api/labels/tag`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ mailId, labelId: trashLabel.id })
      });

      window.location.reload();
    } catch (err) {
      console.error("Error moving mail to trash:", err);
      alert("Failed to move mail to trash");
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
            {/*<h1>{labelName}</h1>
            {/*<LogoutButton />*/}
          </div>
        </>
      )}

      {labelId !== "inbox"}
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

              {/* Only show tagging options for non-trash labels */}
              {labelName.toLowerCase() !== "trash" && (
                <div style={{ marginBottom: "0.5rem" }}>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setOpenLabelManagement(prev => ({
                        ...prev,
                        [mail.id]: !prev[mail.id]
                      }));
                    }}
                    style={{
                      cursor: "pointer",
                      color: "#174ea6",
                      fontWeight: "500",
                      marginBottom: "0.3rem",
                      background: "none",
                      border: "none",
                      textDecoration: "underline"
                    }}
                  >
                    {openLabelManagement[mail.id] ? "Hide Labels" : "Manage Labels"}
                  </button>
                  
                  {openLabelManagement[mail.id] && (
                    <div 
                      onClick={(e) => e.stopPropagation()}
                      style={{ padding: "0.5rem 0", display: "flex", flexWrap: "wrap", gap: "0.5rem" }}
                    >
                      {allLabels
                        .filter(label => label.name.toLowerCase() !== "trash")
                        .map((label) => {
                          const isCurrentlyLabeled = (currentMailLabels[mail.id] || []).includes(label.id);
                          const isPendingSelection = (pendingLabelChanges[mail.id] || []).includes(label.id);
                          
                          // Initialize pending changes with current labels when first opening
                          if (openLabelManagement[mail.id] && !pendingLabelChanges[mail.id]) {
                            setPendingLabelChanges(prev => ({
                              ...prev,
                              [mail.id]: currentMailLabels[mail.id] || []
                            }));
                          }
                          
                          return (
                            <label
                              key={label.id}
                              onClick={(e) => e.stopPropagation()}
                              style={{
                                display: "flex",
                                alignItems: "center",
                                backgroundColor: isPendingSelection ? "#e8f0fe" : "#f1f3f4",
                                color: isPendingSelection ? "#174ea6" : "#202124",
                                padding: "4px 10px",
                                borderRadius: "16px",
                                border: isPendingSelection ? "1px solid #174ea6" : "1px solid #ccc",
                                fontSize: "0.85rem",
                                cursor: "pointer",
                                fontWeight: isPendingSelection ? "500" : "normal"
                              }}
                            >
                              <input
                                type="checkbox"
                                style={{ marginRight: "6px" }}
                                onClick={(e) => e.stopPropagation()}
                                onChange={(e) => {
                                  e.stopPropagation();
                                  setPendingLabelChanges(prev => {
                                    const pendingLabels = new Set(prev[mail.id] || currentMailLabels[mail.id] || []);
                                    if (e.target.checked) {
                                      pendingLabels.add(label.id);
                                    } else {
                                      pendingLabels.delete(label.id);
                                    }
                                    return { ...prev, [mail.id]: Array.from(pendingLabels) };
                                  });
                                }}
                                checked={isPendingSelection}
                              />
                              <span onClick={(e) => e.stopPropagation()}>
                                {label.name}
                              </span>
                            </label>
                          );
                        })}
                    </div>
                  )}
                </div>
              )}

              {/* Move to Trash only - available for all labels except trash itself */}
              {labelName.toLowerCase() !== "trash" && (
                <button
                  className="trash-button"
                  onClick={(e) => {
                    e.stopPropagation();
                    moveMailToTrashOnly(mail.id);
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
              )}

              {/* Move to selected labels - only for non-trash labels */}
              {labelName.toLowerCase() !== "trash" && openLabelManagement[mail.id] && (
                <button
                  className="move-button"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleMoveToLabels(mail.id);
                  }}
                  style={{
                    backgroundColor: "#e8f0fe",
                    color: "#174ea6",
                    border: "1px solid #c6dafc",
                    padding: "4px 8px",
                    fontSize: "0.8rem",
                    borderRadius: "4px",
                    cursor: "pointer",
                    marginRight: "0.5rem"
                  }}
                >
                  Apply Changes
                </button>
              )}

              {/* Delete Forever (only from Trash) */}
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
              
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default LabelPage;