// LabelPage.jsx - Complete with fixed read/unread functionality

import { FaTrash, FaArchive, FaTag } from "react-icons/fa";
import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { fetchWithAuth } from "../utils/api";
import { useSearch } from "../context/SearchContext";
import { useLocation } from "react-router-dom";
import Loading from "../components/Loading";

const LabelPage = () => {
  const { labelId } = useParams();
  const [mails, setMails] = useState([]);
  const [labelName, setLabelName] = useState("");
  const [allLabels, setAllLabels] = useState([]);
  const [error, setError] = useState("");
  const { searchQuery, setSearchQuery } = useSearch();
  const location = useLocation();
  const [loading, setLoading] = useState(true);

  // Read status tracking - with proper refresh mechanism
  const [readMails, setReadMails] = useState(new Set());

  // Label detection helpers
  const [draftLabelId, setDraftLabelId] = useState(null);
  const [labelsReady, setLabelsReady] = useState(false);

  // Mail management state
  const [currentMailLabels, setCurrentMailLabels] = useState({});
  const [openLabelManagement, setOpenLabelManagement] = useState({});
  const [pendingLabelChanges, setPendingLabelChanges] = useState({});
  
  // Gmail-style selection state
  const [selectedMails, setSelectedMails] = useState(new Set());
  const [selectAll, setSelectAll] = useState(false);

  const navigate = useNavigate();
  const token = localStorage.getItem("token");

  // Enhanced localStorage management with event listeners
  useEffect(() => {
    // Load read mails from localStorage
    const loadReadMails = () => {
      const stored = localStorage.getItem('readMails');
      if (stored) {
        try {
          const readArray = JSON.parse(stored);
          setReadMails(new Set(readArray));
          console.log('Loaded/Updated read mails:', readArray);
        } catch (e) {
          console.error('Error loading read mails:', e);
          setReadMails(new Set());
        }
      }
    };

    // Initial load
    loadReadMails();

    // Listen for storage changes (works across tabs)
    const handleStorageChange = (e) => {
      if (e.key === 'readMails') {
        console.log('Storage changed, refreshing read mails');
        loadReadMails();
      }
    };

    // Listen for focus events (when user returns to this tab)
    const handleFocus = () => {
      console.log('Page focused, refreshing read mails');
      loadReadMails();
    };

    // Custom event for same-page updates
    const handleReadMailUpdate = () => {
      console.log('Custom read mail event, refreshing');
      loadReadMails();
    };

    // Listen for page visibility changes
    const handleVisibilityChange = () => {
      if (!document.hidden) {
        console.log('Page visible, refreshing read mails');
        loadReadMails();
      }
    };

    window.addEventListener('storage', handleStorageChange);
    window.addEventListener('focus', handleFocus);
    window.addEventListener('readMailsUpdated', handleReadMailUpdate);
    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      window.removeEventListener('storage', handleStorageChange);
      window.removeEventListener('focus', handleFocus);
      window.removeEventListener('readMailsUpdated', handleReadMailUpdate);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, []);

  // Mark mail as read with event dispatch
  const markAsRead = (mailId) => {
    setReadMails(prev => {
      const newSet = new Set([...prev, mailId]);
      const newArray = Array.from(newSet);
      localStorage.setItem('readMails', JSON.stringify(newArray));
      
      // Dispatch custom event to notify other components
      window.dispatchEvent(new CustomEvent('readMailsUpdated'));
      console.log(`Marked mail ${mailId} as read`);
      
      return newSet;
    });
  };

  // Helper functions
  const normalize = (text) => (text || "").toString().trim().toLowerCase();

  const getDraftLabelId = (labelsList) => {
    const drafts = (labelsList || []).find(
      (l) => (l.name || "").toLowerCase() === "drafts"
    );
    return drafts ? drafts.id : null;
  };

  const hasNameDraft = (labelsOrNames) => {
    if (!Array.isArray(labelsOrNames)) return false;
    return labelsOrNames.some(
      (x) => typeof x === "string" && normalize(x) === "drafts"
    );
  };

  const includesId = (arr, id) =>
    Array.isArray(arr) && id != null ? arr.includes(id) : false;

  const isDraftMailRobust = (mail) => {
    if (mail?.isDraft === true) return true;
    if (Array.isArray(mail?.labels)) {
      if (draftLabelId && includesId(mail.labels, draftLabelId)) return true;
      if (hasNameDraft(mail.labels)) return true;
    }
    const idsForMail = currentMailLabels[mail?.id] || [];
    if (draftLabelId && includesId(idsForMail, draftLabelId)) return true;
    if (normalize(labelName) === "drafts" || normalize(labelId) === "drafts")
      return true;
    return false;
  };

  const ensureLabelIdByName = async (name) => {
    const wanted = String(name || "").trim().toLowerCase();
    if (!wanted) throw new Error("Invalid label name");

    const exists = (allLabels || []).find(
      (l) => (l.name || "").toLowerCase() === wanted
    );
    if (exists) return exists.id;

    const res = await fetch("/api/labels", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ name: wanted }),
    });

    if (!res.ok) {
      let msg = `Failed to create label '${wanted}' (${res.status})`;
      try {
        const ct = res.headers.get("content-type") || "";
        if (ct.includes("application/json")) {
          const j = await res.json();
          if (j?.error) msg = j.error;
        } else {
          const t = await res.text();
          if (t) msg = t;
        }
      } catch { }
      throw new Error(msg);
    }

    const created = await res.json();
    setAllLabels((prev) => [...prev, created]);
    return created.id;
  };

  const resolveSystemLabelId = async (nameLower) => {
    const nm = String(nameLower || "").toLowerCase();
    const found = (allLabels || []).find(
      (l) => (l.name || "").toLowerCase() === nm
    );
    if (found) return found.id;
    return await ensureLabelIdByName(nm);
  };

  // Enhanced safeOpenMail - mark as read when opening
  const safeOpenMail = (mail) => {
    if (!labelsReady && mail?.isDraft !== true) {
      return;
    }
    
    // Mark as read when opening
    markAsRead(mail.id);
    
    if (isDraftMailRobust(mail)) {
      navigate(`/draft/${mail.id}`);
      return;
    }
    navigate(`/mail/${mail.id}`);
  };

  // Gmail-style selection functions
  const handleSelectAll = () => {
    if (selectAll) {
      setSelectedMails(new Set());
    } else {
      setSelectedMails(new Set(filteredMails.map(mail => mail.id)));
    }
    setSelectAll(!selectAll);
  };

  const handleSelectMail = (mailId, event) => {
    event.stopPropagation();
    const newSelected = new Set(selectedMails);
    if (newSelected.has(mailId)) {
      newSelected.delete(mailId);
    } else {
      newSelected.add(mailId);
    }
    setSelectedMails(newSelected);
    setSelectAll(newSelected.size === filteredMails.length);
  };

  // Format date like Gmail
  const formatDate = (timestamp) => {
    if (!timestamp) return "";
    const date = new Date(timestamp);
    const now = new Date();
    const diffDays = Math.floor((now - date) / (1000 * 60 * 60 * 24));
    
    if (diffDays === 0) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } else if (diffDays < 7) {
      return date.toLocaleDateString([], { weekday: 'short' });
    } else if (date.getFullYear() === now.getFullYear()) {
      return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
    } else {
      return date.toLocaleDateString([], { year: '2-digit', month: 'short', day: 'numeric' });
    }
  };

  // Get mail labels for display
  const getMailLabels = (mail) => {
    const labelIds = currentMailLabels[mail?.id] || [];
    return labelIds.map(id => {
      const label = allLabels.find(l => l.id === id);
      return label || { id, name: `Label ${id}` };
    }).filter(label => label.name.toLowerCase() !== 'inbox');
  };

  // Update pending label changes when management opens
  useEffect(() => {
    const updates = {};
    let needUpdate = false;

    for (const [mid, isOpen] of Object.entries(openLabelManagement)) {
      if (isOpen && !pendingLabelChanges[mid]) {
        updates[mid] = currentMailLabels[mid] || [];
        needUpdate = true;
      }
    }
    if (needUpdate) {
      setPendingLabelChanges((prev) => ({ ...prev, ...updates }));
    }
  }, [openLabelManagement, currentMailLabels, pendingLabelChanges]);

  // Action handlers
  const handleMoveToLabels = async (mailId) => {
    try {
      const selectedLabels = pendingLabelChanges[mailId] || [];
      const currentLabels = currentMailLabels[mailId] || [];

      for (const labelId of selectedLabels) {
        if (!currentLabels.includes(labelId)) {
          await fetch("/api/labels/tag", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify({ mailId, labelId }),
          });
        }
      }

      for (const labelId of currentLabels) {
        if (!selectedLabels.includes(labelId)) {
          await fetch("/api/labels/untag", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify({ mailId, labelId }),
          });
        }
      }

      setPendingLabelChanges((prev) => {
        const next = { ...prev };
        delete next[mailId];
        return next;
      });

      window.location.reload();
    } catch (err) {
      console.error("Error applying label changes:", err);
      alert("Failed to update labels.");
    }
  };

  const discardDraft = async (mailId) => {
    try {
      const res = await fetch(`/api/mails/${mailId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error(`delete failed ${res.status}`);

      navigate("/label/drafts", { replace: true, state: { justDeletedId: mailId, ts: Date.now() } });
    } catch (e) {
      console.error(e);
      alert("Failed to discard draft.");
    }
  };

  const moveMailToTrashOnly = async (mailId) => {
    try {
      const trashId = await ensureLabelIdByName("trash");

      const res = await fetch(`/api/labels/mail/${mailId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error(`labels fetch failed ${res.status}`);
      const currentLabels = await res.json();
      const safeCurrent = Array.isArray(currentLabels) ? currentLabels : [];

      await Promise.all(
        safeCurrent.map((lblId) =>
          fetch(`/api/labels/untag`, {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify({ mailId, labelId: lblId }),
          })
        )
      );

      const tagRes = await fetch(`/api/labels/tag`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ mailId, labelId: trashId }),
      });
      if (!tagRes.ok) throw new Error(`tag trash failed ${tagRes.status}`);

      setMails((prev) => prev.filter((m) => m.id !== mailId));
      setCurrentMailLabels((prev) => ({ ...prev, [mailId]: [trashId] }));
    } catch (err) {
      console.error("Error moving mail to trash:", err);
      alert(err.message || "Failed to move mail to trash");
    }
  };

  // Handle deleted drafts from navigation state
  useEffect(() => {
    const deletedId = location.state?.justDeletedId;
    if (!deletedId) return;

    setMails((prev) => prev.filter((m) => m.id !== deletedId));
    setCurrentMailLabels((prev) => {
      const next = { ...prev };
      delete next[deletedId];
      return next;
    });

    navigate(location.pathname, { replace: true, state: null });
  }, [location.state?.justDeletedId, location.pathname, navigate]);

  // Main data loading effect
  useEffect(() => {
    setSearchQuery("");
    if (!token) return;

    const sysNames = new Set(["inbox", "sent", "trash", "spam", "drafts"]);
    if (sysNames.has(String(labelId).toLowerCase())) {
      setLabelName(String(labelId).toLowerCase());
    } else {
      fetchWithAuth(`/labels/${labelId}`, token)
        .then((data) => setLabelName(data?.name || labelId))
        .catch(() => setLabelName(labelId));
    }
    
    const fetchMails = async () => {
      try {
        let validMails = [];

        if (labelId === "inbox" || labelId === "sent") {
          const response = await fetch("/api/mails", {
            headers: { Authorization: `Bearer ${token}` },
          });
          const data = await response.json();

          const list =
            labelId === "inbox"
              ? Array.isArray(data?.inbox)
                ? data.inbox
                : []
              : Array.isArray(data?.sent)
                ? data.sent
                : [];

          const prelim = list.filter(
            (m) => m?.isDraft !== true && !hasNameDraft(m?.labels || [])
          );

          const final = [];
          for (const mail of prelim) {
            try {
              const res = await fetch(`/api/labels/mail/${mail.id}`, {
                headers: { Authorization: `Bearer ${token}` },
              });
              if (!res.ok) {
                final.push(mail);
                continue;
              }
              const labelIds = await res.json();
              const names = await Promise.all(
                (labelIds || []).map(async (lid) => {
                  try {
                    const ld = await fetchWithAuth(`/labels/${lid}`, token);
                    return ld?.name?.toLowerCase() || "";
                  } catch {
                    return "";
                  }
                })
              );
              if (!names.includes("trash")) final.push(mail);
            } catch {
              final.push(mail);
            }
          }

          validMails = final;

        } else if (String(labelId).toLowerCase() === "trash") {
          const trashId = await resolveSystemLabelId("trash");
          const ids = await fetchWithAuth(`/labels/by-label/${trashId}`, token);
          if (Array.isArray(ids) && ids.length > 0) {
            const full = await Promise.all(
              ids.map(async (id) => {
                try {
                  return await fetchWithAuth(`/mails/${id}`, token);
                } catch {
                  return null;
                }
              })
            );
            validMails = full.filter(Boolean);
          } else {
            validMails = [];
          }
        } else if (String(labelId).toLowerCase() === "spam") {
          const spamId = await resolveSystemLabelId("spam");
          const ids = await fetchWithAuth(`/labels/by-label/${spamId}`, token);
          if (Array.isArray(ids) && ids.length > 0) {
            const full = await Promise.all(
              ids.map(async (id) => {
                try {
                  return await fetchWithAuth(`/mails/${id}`, token);
                } catch {
                  return null;
                }
              })
            );
            validMails = full.filter(Boolean);
          } else {
            validMails = [];
          }
        } else {
          const ids = await fetchWithAuth(`/labels/by-label/${labelId}`, token);
          if (Array.isArray(ids) && ids.length > 0) {
            const full = await Promise.all(
              ids.map(async (id) => {
                try {
                  return await fetchWithAuth(`/mails/${id}`, token);
                } catch (err) {
                  console.warn(`Failed to fetch mail ${id}:`, err);
                  return null;
                }
              })
            );
            validMails = full.filter(Boolean);
          } else {
            validMails = [];
          }
        }

        setMails(validMails);

        const mailLabelsMap = {};
        for (const mail of validMails) {
          try {
            const res = await fetch(`/api/labels/mail/${mail.id}`, {
              headers: { Authorization: `Bearer ${token}` },
            });
            if (!res.ok) {
              mailLabelsMap[mail.id] = [];
              continue;
            }
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
    
    setLoading(true);
    fetchMails().finally(() => setLoading(false));

    fetchWithAuth("/labels", token)
      .then((list) => {
        setAllLabels(list);
        setDraftLabelId(getDraftLabelId(list));
        setLabelsReady(true);
      })
      .catch((e) => {
        console.error(e);
        setLabelsReady(true);
      });
  }, [labelId, token, setSearchQuery]);

  // Filter mails based on search query
  const filteredMails = mails.filter((mail) => {
    const search = normalize(searchQuery);

    const sender =
      typeof mail.sender === "string"
        ? normalize(mail.sender)
        : normalize(mail.sender?.email || "");

    const recipient =
      typeof mail.recipient === "string"
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

  // Debug logging
  console.log('Read mails debug:', {
    readMailsCount: readMails.size,
    totalMails: filteredMails.length,
    sampleReadStatus: filteredMails.slice(0, 3).map(m => ({
      id: m.id,
      subject: m.subject?.slice(0, 20),
      isRead: readMails.has(m.id)
    }))
  });

  // Render the component
  return (
    <div className="gmail-main-area">
      {loading ? (
        <Loading label="Loading Mails…" />
      ) : error ? (
        <p style={{ color: "red", padding: "1rem" }}>{error}</p>
      ) : (
        <>
          {/* Gmail-style toolbar */}
          <div className="gmail-toolbar">
            <div className="gmail-toolbar-left">
              <div className="gmail-checkbox-all">
                <input
                  type="checkbox"
                  checked={selectAll}
                  onChange={handleSelectAll}
                />
              </div>
              <button className="gmail-refresh-btn" onClick={() => window.location.reload()}>
                ↻
              </button>
            </div>
            <div className="gmail-toolbar-right">
              <div className="gmail-pagination">
                <span>
                  {filteredMails.length > 0 
                    ? `1-${Math.min(50, filteredMails.length)} of ${filteredMails.length}`
                    : "No mails"
                  }
                </span>
                <button disabled>‹</button>
                <button disabled>›</button>
              </div>
            </div>
          </div>

          {/* Mail list container */}
          <div className="gmail-mail-list-container">
            {filteredMails.length === 0 ? (
              <div className="gmail-no-mails">
                <h3>No conversations in {labelName}</h3>
                <p>When you get new mail it will appear here.</p>
              </div>
            ) : (
              <div className="gmail-mail-list">
                {filteredMails.map((mail) => {
                  const draft = isDraftMailRobust(mail);
                  const isSelected = selectedMails.has(mail.id);
                  const isRead = readMails.has(mail.id);
                  const mailLabels = getMailLabels(mail);
                  const senderInfo = (() => {
                    const sender = mail.sender || mail.otherParty;
                    if (!sender) return "(unknown)";
                    if (typeof sender === "string") return sender;
                    return sender.email || `${sender.firstName || ""} ${sender.lastName || ""}`.trim();
                  })();

                  // Debug log for each mail
                  if (mail.id && readMails.size > 0) {
                    console.log(`Mail ${mail.id}: isRead=${isRead}, subject="${mail.subject?.slice(0, 20)}"`);
                  }

                  return (
                    <div
                      key={mail.id}
                      className={`gmail-mail-item ${draft ? 'is-draft' : ''} ${isSelected ? 'selected' : ''} ${isRead ? 'read' : 'unread'}`}
                      style={{
                        // Force inline styles for debugging
                        backgroundColor: isRead ? '#f2f6fc !important' : '#ffffff !important',
                        fontWeight: isRead ? 'normal !important' : 'bold !important'
                      }}
                    >
                      <div 
                        className="gmail-mail-item-content"
                        onClick={() => safeOpenMail(mail)}
                      >
                        {/* Checkbox */}
                        <div className="gmail-mail-checkbox">
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={(e) => handleSelectMail(mail.id, e)}
                          />
                        </div>

                        {/* Star */}
                        <div className="gmail-mail-star">
                          ☆
                        </div>

                        {/* Sender */}
                        <div className="gmail-mail-sender" style={{
                          fontWeight: isRead ? 'normal !important' : 'bold !important',
                          color: isRead ? '#5f6368 !important' : '#202124 !important'
                        }}>
                          {senderInfo}
                          {draft && <span style={{ color: 'var(--muted)', fontSize: '12px' }}> Draft</span>}
                        </div>

                        {/* Subject and preview */}
                        <div className="gmail-mail-subject-content">
                          <span className="gmail-mail-subject" style={{
                            fontWeight: isRead ? 'normal !important' : 'bold !important',
                            color: isRead ? '#5f6368 !important' : '#202124 !important'
                          }}>
                            {mail.subject || "(no subject)"}
                          </span>
                          <span className="gmail-mail-preview">
                            {mail.preview || mail.content?.slice(0, 100) || "(no content)"}
                          </span>
                        </div>

                        {/* Labels */}
                        <div className="gmail-mail-labels">
                          {mailLabels.map((label) => (
                            <span 
                              key={label.id} 
                              className={`gmail-label-chip ${label.name.toLowerCase()}`}
                            >
                              {label.name}
                            </span>
                          ))}
                        </div>

                        {/* Date */}
                        <div className="gmail-mail-date" style={{
                          fontWeight: isRead ? 'normal !important' : 'bold !important',
                          color: isRead ? '#5f6368 !important' : '#202124 !important'
                        }}>
                          {formatDate(mail.timestamp)}
                        </div>
                      </div>

                      {/* Action buttons */}
                      <div className="gmail-mail-actions">
                        {!["trash", "drafts"].includes((labelName || "").toLowerCase()) && (
                          <>
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                setOpenLabelManagement((prev) => ({
                                  ...prev,
                                  [mail.id]: !prev[mail.id],
                                }));
                              }}
                              title="Labels"
                            >
                              <FaTag />
                            </button>
                            
                            {openLabelManagement[mail.id] && (
                              <div className="gmail-label-management-popup">
                                {allLabels
                                  .filter((label) => (label.name || "").toLowerCase() !== "trash")
                                  .map((label) => {
                                    const isPendingSelection = (
                                      pendingLabelChanges[mail.id] || []
                                    ).includes(label.id);

                                    return (
                                      <div 
                                          key={label.id} 
                                          className="gmail-label-checkbox-item"
                                          onClick={(e) => {
                                            e.stopPropagation();
                                            setPendingLabelChanges((prev) => {
                                              const base = prev[mail.id] || currentMailLabels[mail.id] || [];
                                              const pending = new Set(base);
                                              if (!isPendingSelection) {
                                                pending.add(label.id);
                                              } else {
                                                pending.delete(label.id);
                                              }
                                              return {
                                                ...prev,
                                                [mail.id]: Array.from(pending),
                                              };
                                            });
                                          }}
                                          style={{ cursor: 'pointer' }}
                                        >
                                          <input
                                            type="checkbox"
                                            onChange={(e) => {
                                              e.stopPropagation();
                                            }}
                                            checked={isPendingSelection}
                                            readOnly
                                          />
                                          <span>{label.name}</span>
                                        </div>
                                    );
                                  })}
                                
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleMoveToLabels(mail.id);
                                  }}
                                  style={{ 
                                    marginTop: '8px', 
                                    width: '100%',
                                    padding: '6px',
                                    background: 'var(--accent)',
                                    color: 'white',
                                    border: 'none',
                                    borderRadius: '4px'
                                  }}
                                >
                                  Apply
                                </button>
                              </div>
                            )}
                          </>
                        )}

                        {draft ? (
                          <>
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                navigate(`/draft/${mail.id}`, { state: { assumeDraft: true } });
                              }}
                              title="Edit Draft"
                            >
                              ✎
                            </button>
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                if (window.confirm("Discard this draft permanently?")) {
                                  discardDraft(mail.id);
                                }
                              }}
                              title="Discard Draft"
                            >
                              ✕
                            </button>
                          </>
                        ) : (
                          <>
                            {(labelName || "").toLowerCase() !== "trash" && (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  moveMailToTrashOnly(mail.id);
                                }}
                                title="Move to Trash"
                              >
                                <FaTrash />
                              </button>
                            )}

                            {(labelName || "").toLowerCase() === "trash" && (
                              <button
                                onClick={async (e) => {
                                  e.stopPropagation();
                                  try {
                                    await fetch(`/api/mails/${mail.id}`, {
                                      method: "DELETE",
                                      headers: {
                                        Authorization: `Bearer ${token}`,
                                      },
                                    });
                                    setMails((prev) => prev.filter((m) => m.id !== mail.id));
                                  } catch (err) {
                                    console.error("Failed to delete mail:", err);
                                    alert("Failed to delete mail.");
                                  }
                                }}
                                title="Delete Forever"
                              >
                                ✕
                              </button>
                            )}
                          </>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};

export default LabelPage;