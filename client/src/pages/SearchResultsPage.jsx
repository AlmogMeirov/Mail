// client/src/pages/SearchResultsPage.jsx
// NOTE: comments in English only

import React, { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { fetchWithAuth } from "../utils/api";

const normalize = (s) => (s || "").toString().trim().toLowerCase();

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

const senderEmailOf = (mail) => {
  if (!mail) return "";
  if (typeof mail.sender === "string") return mail.sender;
  if (mail.sender?.email) return mail.sender.email;
  if (mail.from) return mail.from;
  return "";
};

const recipientEmailOf = (mail) => {
  if (!mail) return "";
  if (typeof mail.recipient === "string") return mail.recipient;
  if (mail.recipient?.email) return mail.recipient.email;
  if (mail.to) return mail.to;
  return "";
};

const hasNameDraft = (labelsOrNames) => {
  if (!Array.isArray(labelsOrNames)) return false;
  return labelsOrNames.some(
    (x) => typeof x === "string" && normalize(x) === "drafts"
  );
};

const SearchResultsPage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const q = new URLSearchParams(location.search).get("q") || "";

  const token = localStorage.getItem("token") || "";
  const myEmail = useMemo(() => normalize(emailFromToken(token)), [token]);

  const [draftsId, setDraftsId] = useState(null);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  // 1) fetch labels to get the real "drafts" label id
  useEffect(() => {
    let dead = false;
    const run = async () => {
      if (!token) return;
      try {
        const list = await fetchWithAuth("/labels", token);
        const drafts = (list || []).find(
          (l) => (l.name || "").toLowerCase() === "drafts"
        );
        if (!dead) setDraftsId(drafts ? drafts.id : null);
      } catch (e) {
        if (!dead) setDraftsId(null);
      }
    };
    run();
    return () => { dead = true; };
  }, [token]);

  // helper: check via /api/labels/mail/:id if a mail has the drafts label id
  const checkIsDraftByServerLabels = async (mailId) => {
    if (!mailId || !token) return false;
    try {
      const res = await fetch(`/api/labels/mail/${mailId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) return false;
      const ids = await res.json();
      if (!Array.isArray(ids)) return false;
      return draftsId ? ids.includes(draftsId) : false;
    } catch {
      return false;
    }
  };

  // 2) run search and hard-filter
  useEffect(() => {
    let dead = false;

    const run = async () => {
      if (!token || !q.trim()) {
        setResults([]);
        return;
      }
      setLoading(true);
      setErr("");
      try {
        // try /api/mails/search first
        const tryFetch = async (path) => {
          try {
            return await fetchWithAuth(`${path}?q=${encodeURIComponent(q)}&ts=${Date.now()}`, token);
          } catch (e) {
            // fetchWithAuth throws on non-ok; rethrow to try fallback
            throw e;
          }
        };

        let data;
        try {
          data = await tryFetch("/mails/search"); // resolves to /api/mails/search inside fetchWithAuth
        } catch (e1) {
          // fallback to /api/search if the first path doesn't exist
          try {
            data = await tryFetch("/search");
          } catch (e2) {
            throw e2;
          }
        }

        const raw = Array.isArray(data) ? data : Array.isArray(data?.results) ? data.results : [];

        // enrich with precise draft flag by querying label ids when needed
        const enriched = await Promise.all(
          raw.map(async (m) => {
            const sender = normalize(senderEmailOf(m));
            const recipient = normalize(recipientEmailOf(m));

            // quick signals
            let isDraft =
              m?.isDraft === true ||
              hasNameDraft(m?.labels || []);

            // if still unknown, ask the server for the mail's label ids
            if (!isDraft && draftsId) {
              isDraft = await checkIsDraftByServerLabels(m.id);
            }

            return {
              ...m,
              __sender: sender,
              __recipient: recipient,
              __isDraft: !!isDraft,
            };
          })
        );

        // HARD FILTER:
        //  a) Only mails that involve me (sender or recipient)
        //  b) If draft -> only if I am the sender (owner)
        const safe = enriched.filter((m) => {
          const involvesMe =
            m.__sender === myEmail || m.__recipient === myEmail;

          if (!involvesMe) return false;
          if (!m.__isDraft) return true;
          return m.__sender === myEmail; // only my drafts
        });

        // debug to console to see exactly what got filtered and why
        console.table(
          enriched.map((m) => ({
            id: m.id,
            subj: m.subject,
            sender: m.__sender,
            recipient: m.__recipient,
            isDraft: m.__isDraft,
            kept: safe.some((x) => x.id === m.id),
          }))
        );

        if (!dead) setResults(safe);
      } catch (e) {
        console.error("[Search] error:", e);
        if (!dead) {
          setErr("Failed to load search results");
          setResults([]);
        }
      } finally {
        if (!dead) setLoading(false);
      }
    };

    run();
    // re-run when query, token, or draftsId changes
  }, [q, token, draftsId, myEmail]);

  const openItem = (m) => {
    if (!m?.id) return;
    if (m.__isDraft) {
      navigate(`/draft/${m.id}`, { state: { assumeDraft: true } });
    } else {
      navigate(`/mail/${m.id}`);
    }
  };

  return (
    <div style={{ padding: "1rem" }}>
      <h1>Search Results for: "{q}"</h1>

      {loading && <p>Searching…</p>}
      {err && <p style={{ color: "red" }}>{err}</p>}

      {!loading && results.length === 0 ? (
        <p>No matching mails found.</p>
      ) : (
        <ul style={{ listStyle: "none", padding: 0 }}>
          {results.map((m) => (
            <li
              key={m.id}
              onClick={() => openItem(m)}
              style={{
                cursor: "pointer",
                border: "1px solid #ccc",
                padding: "1rem",
                marginBottom: "1rem",
                borderRadius: "8px",
                backgroundColor: m.__isDraft ? "#fff7e6" : "#f9f9f9",
              }}
            >
              <strong>
                {m.subject || <em>(no subject)</em>}
                {m.__isDraft ? " [Draft]" : ""}
              </strong>
              <br />
              <strong>From:</strong>{" "}
              {senderEmailOf(m) || "(unknown)"}
              <br />
              <strong>Date:</strong>{" "}
              {m.timestamp ? new Date(m.timestamp).toLocaleString() : "Invalid Date"}
              <p style={{ color: "#666" }}>
                {m.preview || (m.content || "").slice(0, 100) || <em>(no content)</em>}
              </p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default SearchResultsPage;
