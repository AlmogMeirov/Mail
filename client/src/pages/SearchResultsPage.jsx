// client/src/pages/SearchResultsPage.jsx
// Enhanced search results page reusing existing mail row components

import React, { useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { fetchWithAuth } from "../utils/api";
import Loading from "../components/Loading";

const normalize = (s) => (s || "").toString().trim().toLowerCase();

// Decode email from JWT payload
const emailFromToken = (jwt) => {
  try {
    const b64 = (jwt || "").split(".")[1];
    if (!b64) return "";
    const json = atob(b64.replace(/-/g, "+").replace(/_/g, "/"));
    const p = JSON.parse(json);
    return (
      p.email ||
      p.user?.email ||
      p.preferred_username ||
      (typeof p.sub === "string" && p.sub.includes("@") ? p.sub : "") ||
      ""
    );
  } catch {
    return "";
  }
};

// Extract a single email from string/object/array
const extractEmail = (v) => {
  if (!v) return "";
  if (typeof v === "string") {
    const m = v.match(/<([^>]+)>/);
    const addr = m ? m[1] : v;
    return addr.toLowerCase().trim();
  }
  if (typeof v === "object" && v.email) {
    return v.email.toLowerCase().trim();
  }
  if (Array.isArray(v) && v.length > 0) {
    return extractEmail(v[0]);
  }
  return "";
};

// Format date like Gmail - shows time for today, date for older emails
const formatDate = (timestamp) => {
  if (!timestamp) return "";
  const date = new Date(timestamp);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const mailDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());

  // If today - show time (e.g., "2:30 PM")
  if (mailDate.getTime() === today.getTime()) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
  
  // If yesterday - show "Yesterday"
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);
  if (mailDate.getTime() === yesterday.getTime()) {
    return "Yesterday";
  }
  
  // If this week - show day name (e.g., "Mon")
  const dayDiff = Math.floor((today.getTime() - mailDate.getTime()) / (1000 * 60 * 60 * 24));
  if (dayDiff < 7) {
    return date.toLocaleDateString([], { weekday: 'short' });
  }
  
  // If this year - show month and day (e.g., "Aug 15")
  if (date.getFullYear() === now.getFullYear()) {
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  }
  
  // If older - show full date (e.g., "8/15/23")
  return date.toLocaleDateString([], { year: '2-digit', month: 'numeric', day: 'numeric' });
};

const SearchResultsPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const query = searchParams.get("q") || "";
  
  const [results, setResults] = useState([]);
  const [selectedMails, setSelectedMails] = useState(new Set());
  const [isLoading, setLoading] = useState(false);
  const [err, setErr] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [draftsId, setDraftsId] = useState(null);
  
  const token = localStorage.getItem("token");
  const myEmail = emailFromToken(token);
  
  const mailsPerPage = 50;
  
  const draftLabelCacheRef = useRef(new Map());
  const inflightRef = useRef(new Map());

  // Get drafts label ID - same as existing LabelPage logic
  useEffect(() => {
    if (!token) return;
    let dead = false;
    const run = async () => {
      try {
        const labels = await fetchWithAuth("/labels", token);
        const drafts = Array.isArray(labels)
          ? labels.find((l) => normalize(l.name) === "drafts")
          : null;
        if (!dead) setDraftsId(drafts?.id || drafts?.name || drafts?.id || null);
      } catch (e) {
        if (!dead) setDraftsId(null);
      }
    };
    run();
    return () => { dead = true; };
  }, [token]);

  // Check if mail is draft by server labels - same as existing logic
  const checkIsDraftByServerLabels = async (mailId) => {
    if (!mailId || !token) return false;
    const cache = draftLabelCacheRef.current;
    if (cache.has(mailId)) return cache.get(mailId);

    const inflight = inflightRef.current;
    if (inflight.has(mailId)) return inflight.get(mailId);

    const p = (async () => {
      try {
        const res = await fetch(`/api/labels/mail/${mailId}`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (!res.ok) return false;

        const arr = await res.json();
        if (!Array.isArray(arr)) return false;

        let result = false;
        if (arr.length && typeof arr[0] === "object") {
          result = arr.some(
            (l) =>
              normalize(l?.name) === "drafts" ||
              (draftsId != null && String(l?.id) === String(draftsId))
          );
        } else if (arr.length && typeof arr[0] === "string") {
          const names = arr.map(normalize);
          if (names.includes("drafts")) result = true;
          else if (draftsId != null) {
            result = arr.map(String).includes(String(draftsId));
          }
        }

        cache.set(mailId, result);
        return result;
      } catch {
        return false;
      } finally {
        inflight.delete(mailId);
      }
    })();

    inflight.set(mailId, p);
    return p;
  };

  // Load search results - improved error handling
  useEffect(() => {
    let dead = false;

    const run = async () => {
      if (!token || !query.trim()) {
        setResults([]);
        return;
      }
      setLoading(true);
      setErr("");
      try {
        const tryFetch = async (path) => {
          return await fetchWithAuth(
            `${path}?q=${encodeURIComponent(query)}&ts=${Date.now()}`,
            token
          );
        };

        let data;
        try {
          data = await tryFetch("/mails/search");
        } catch (e1) {
          try {
            data = await tryFetch("/search");
          } catch (e2) {
            // If both endpoints fail, still try to handle empty results gracefully
            console.warn("[Search] Both search endpoints failed:", e1, e2);
            data = [];
          }
        }

        const raw = Array.isArray(data)
          ? data
          : Array.isArray(data?.results)
            ? data.results
            : [];

        // Enrich with draft detection - same as existing logic
        const enriched = await Promise.all(
          raw.map(async (m) => ({
            ...m,
            isDraft: m?.isDraft === true,
            hasLabels: Array.isArray(m.labels),
            hasLabelIds: Array.isArray(m.labelIds),
            __isDraft: await checkIsDraftByServerLabels(m?.id),
          }))
        );

        if (!dead) {
          // Sort results by timestamp - newest first (like Gmail)
          const sortedResults = enriched.sort((a, b) => {
            const dateA = new Date(a.timestamp || 0);
            const dateB = new Date(b.timestamp || 0);
            return dateB.getTime() - dateA.getTime(); // Newest first
          });
          
          setResults(sortedResults);
          setCurrentPage(1);
        }
      } catch (e) {
        console.error("[Search] error:", e);
        if (!dead) {
          // Only set error for actual technical failures, not empty results
          setErr("Connection error. Please try again.");
          setResults([]);
        }
      } finally {
        if (!dead) setLoading(false);
      }
    };

    run();
  }, [query, token, draftsId, myEmail]);

  // Open mail item - same as existing logic
  const openItem = (m) => {
    if (!m?.id) return;
    if (m.__isDraft) {
      navigate(`/draft/${m.id}`, { state: { assumeDraft: true } });
    } else {
      navigate(`/mail/${m.id}`);
    }
  };

  // Pagination calculations - same as existing
  const totalPages = Math.ceil(results.length / mailsPerPage);
  const startIndex = (currentPage - 1) * mailsPerPage;
  const endIndex = startIndex + mailsPerPage;
  const paginatedResults = results.slice(startIndex, endIndex);

  const goToPage = (page) => {
    if (page >= 1 && page <= totalPages) {
      setCurrentPage(page);
      setSelectedMails(new Set()); // Clear selections when changing pages
    }
  };

  const goToPreviousPage = () => {
    if (currentPage > 1) {
      goToPage(currentPage - 1);
    }
  };

  const goToNextPage = () => {
    if (currentPage < totalPages) {
      goToPage(currentPage + 1);
    }
  };

  // Transform search results to mail format for consistency with existing components
  const transformedMails = paginatedResults.map(mail => ({
    ...mail,
    // Ensure compatibility with existing mail display logic
    sender: mail.sender || mail.from,
    direction: extractEmail(mail.sender || mail.from) === myEmail ? "sent" : "received",
    preview: mail.preview || mail.content?.slice(0, 100) || "",
    labels: ["search-result"], // Add search result indicator
    isRead: false, // Search results should appear unread for visibility
  }));

  return (
    <div className="gmail-main-area">
      {/* Reuse existing Gmail toolbar structure from LabelPage */}
      <div className="gmail-toolbar">
        <div className="gmail-toolbar-left">
          <div className="gmail-select-all-container">
            <input
              type="checkbox"
              onChange={(e) => {
                const allCurrentIds = transformedMails.map(m => m.id);
                if (e.target.checked) {
                  setSelectedMails(new Set([...selectedMails, ...allCurrentIds]));
                } else {
                  const newSelected = new Set(selectedMails);
                  allCurrentIds.forEach(id => newSelected.delete(id));
                  setSelectedMails(newSelected);
                }
              }}
              checked={transformedMails.length > 0 && transformedMails.every(mail => selectedMails.has(mail.id))}
              title="Select all on this page"
            />
          </div>
          
          {selectedMails.size > 0 && (
            <span className="gmail-selection-count">
              {selectedMails.size} selected
            </span>
          )}
        </div>

        <div className="gmail-toolbar-right">
          <button 
            className="gmail-refresh-btn"
            onClick={() => window.location.reload()}
            title="Refresh"
          >
            ↻
          </button>
          
          {/* Reuse existing pagination structure */}
          {results.length > 0 && (
            <div className="gmail-pagination">
              <span className="gmail-results-info">
                {`${startIndex + 1}-${Math.min(endIndex, results.length)} of ${results.length}`}
              </span>
              <button 
                onClick={goToPreviousPage}
                disabled={currentPage <= 1}
                title="Previous page"
              >
                ‹
              </button>
              <button 
                onClick={goToNextPage}
                disabled={currentPage >= totalPages}
                title="Next page"
              >
                ›
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Search header with same styling as existing pages */}
      <div style={{ 
        padding: '16px 20px', 
        borderBottom: '1px solid var(--border, #e0e4e7)',
        background: 'var(--surface, #ffffff)'
      }}>
        <h1 style={{ 
          fontSize: '20px', 
          fontWeight: '400', 
          margin: '0', 
          color: 'var(--text, #202124)' 
        }}>
          Search results for: "<strong>{query}</strong>"
        </h1>
        {results.length > 0 && (
          <p style={{ 
            color: 'var(--muted, #5f6368)', 
            fontSize: '14px', 
            margin: '4px 0 0 0' 
          }}>
            {results.length} result{results.length !== 1 ? 's' : ''} found
          </p>
        )}
      </div>

      {/* Mail list container with same structure as LabelPage */}
      <div className="gmail-mail-list-container">
        {isLoading ? (
          <div style={{ padding: '40px', textAlign: 'center' }}>
            <Loading label="Searching…" />
          </div>
        ) : err ? (
          <div style={{ 
            padding: '60px 20px', 
            textAlign: 'center'
          }}>
            <div style={{ fontSize: '48px', marginBottom: '16px', opacity: 0.5 }}>
              ⚠️
            </div>
            <h3 style={{ 
              fontSize: '18px', 
              fontWeight: '400', 
              margin: '0 0 8px 0', 
              color: 'var(--error, #ea4335)' 
            }}>
              {err}
            </h3>
            <button 
              onClick={() => window.location.reload()} 
              style={{
                background: 'var(--accent, #1a73e8)',
                color: 'white',
                border: 'none',
                padding: '8px 16px',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '14px',
                marginTop: '8px'
              }}
            >
              Try Again
            </button>
          </div>
        ) : transformedMails.length === 0 ? (
          <div style={{ 
            padding: '60px 20px', 
            textAlign: 'center', 
            color: 'var(--muted, #5f6368)',
            fontSize: '16px',
            lineHeight: '1.5'
          }}>
            <div style={{ fontSize: '48px', marginBottom: '16px', opacity: 0.5 }}>
              🔍
            </div>
            <h3 style={{ 
              fontSize: '18px', 
              fontWeight: '400', 
              margin: '0 0 8px 0', 
              color: 'var(--text, #202124)' 
            }}>
              No results found
            </h3>
            <p style={{ margin: '0', color: 'var(--muted, #5f6368)' }}>
              Try different keywords or check your spelling
            </p>
          </div>
        ) : (
          // Reuse existing mail list structure - this will automatically use existing CSS
          <div className="gmail-mail-list">
            {transformedMails.map((mail) => {
              const isSelected = selectedMails.has(mail.id);
              const isDraft = mail.__isDraft || mail.isDraft;
              const isRead = false; // Search results appear unread
              
              // Use same sender info logic as LabelPage
              let senderInfo = "";
              if (isDraft) {
                senderInfo = "Draft";
              } else {
                const senderEmail = extractEmail(mail.sender);
                senderInfo = senderEmail === myEmail ? "me" : senderEmail || "(unknown)";
              }

              return (
                <div
                  key={mail.id}
                  className={`gmail-mail-item ${isSelected ? 'selected' : ''} ${isRead ? 'read' : 'unread'} ${isDraft ? 'is-draft' : ''}`}
                  onClick={() => openItem(mail)}
                >
                  <div className="gmail-mail-item-content">
                    {/* Checkbox - same as LabelPage */}
                    <div className="gmail-mail-checkbox">
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={(e) => {
                          e.stopPropagation();
                          const newSelected = new Set(selectedMails);
                          if (newSelected.has(mail.id)) {
                            newSelected.delete(mail.id);
                          } else {
                            newSelected.add(mail.id);
                          }
                          setSelectedMails(newSelected);
                        }}
                        onClick={(e) => e.stopPropagation()}
                      />
                    </div>

                    {/* Star - same as LabelPage */}
                    <div className="gmail-mail-star">
                      ☆
                    </div>

                    {/* Sender - same styling as LabelPage */}
                    <div className="gmail-mail-sender">
                      {senderInfo}
                      {isDraft && <span style={{ color: 'var(--muted)', fontSize: '12px' }}> Draft</span>}
                    </div>

                    {/* Subject and preview - same as LabelPage */}
                    <div className="gmail-mail-subject-content">
                      <span className="gmail-mail-subject">
                        {mail.subject || "(no subject)"}
                      </span>
                      <span className="gmail-mail-preview">
                        {mail.preview || mail.content?.slice(0, 100) || "(no content)"}
                      </span>
                    </div>

                    {/* Labels - show search result indicator */}
                    <div className="gmail-mail-labels">
                      <span className="gmail-label-chip search-result">
                        Search Result
                      </span>
                    </div>

                    {/* Date - Gmail-style formatting */}
                    <div className="gmail-mail-date">
                      {formatDate(mail.timestamp)}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Bottom pagination - same as LabelPage */}
        {results.length > mailsPerPage && (
          <div className="gmail-pagination" style={{ 
            margin: '24px 0 0 0', 
            justifyContent: 'center',
            padding: '16px',
            borderTop: '1px solid var(--border, #e0e4e7)'
          }}>
            <button 
              onClick={goToPreviousPage}
              disabled={currentPage <= 1}
              title="Previous page"
            >
              ‹ Previous
            </button>
            <span style={{ margin: '0 16px', color: 'var(--text, #202124)' }}>
              Page {currentPage} of {totalPages}
            </span>
            <button 
              onClick={goToNextPage}
              disabled={currentPage >= totalPages}
              title="Next page"
            >
              Next ›
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default SearchResultsPage;