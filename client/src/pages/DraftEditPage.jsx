// src/pages/DraftEditPage.jsx
// NOTE: comments in English only

import React, { useEffect, useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { fetchWithAuth } from "../utils/api";


const getDraftLabelId = (labelsList) => {
    const drafts = (labelsList || []).find(l => (l.name || "").toLowerCase() === "drafts");
    return drafts ? drafts.id : null;
};

const hasNameDraft = (labelsOrNames) => {
    if (!Array.isArray(labelsOrNames)) return false;
    return labelsOrNames.some(x => typeof x === "string" && x.toLowerCase() === "drafts");
};

const DraftEditPage = () => {
    const { id } = useParams();          // draft id
    const navigate = useNavigate();
    const location = useLocation();
    const token = localStorage.getItem("token");

    const [loading, setLoading] = useState(true);
    const [sending, setSending] = useState(false);
    const [error, setError] = useState("");
    const [mail, setMail] = useState(null);

    // editable fields
    const [fromEmail, setFromEmail] = useState("");   // NEW: sender
    const [to, setTo] = useState("");
    const [subject, setSubject] = useState("");
    const [body, setBody] = useState("");

    const assumeDraft = location.state?.assumeDraft === true;

    // try to derive sender from several sources

    // NOTE: comments in English only
 
    const resolveSender = (m) => {
        // 1) from loaded mail
        const fromMail =
            (typeof m?.sender === "string" && m.sender) ||
            m?.sender?.email ||
            m?.from ||
            "";

        // 2) from localStorage
        const fromStorage = localStorage.getItem("userEmail") || "";

        // 3) from JWT payload (best-effort)
        let fromJwt = "";
        try {
            const b64 = (token || "").split(".")[1];
            if (b64) {
                const json = atob(b64.replace(/-/g, "+").replace(/_/g, "/"));
                const p = JSON.parse(json);
                fromJwt =
                    p.email ||
                    p.user?.email ||
                    p.preferred_username ||
                    (typeof p.sub === "string" && p.sub.includes("@") ? p.sub : "") ||
                    "";
            }
        } catch { }

        const winner = fromMail || fromStorage || fromJwt || "";
        if (winner) localStorage.setItem("userEmail", winner); // cache for next time
        return winner;
    };

    useEffect(() => {
        let killed = false;

        (async () => {
            try {
                setLoading(true);
                setError("");

                // Load labels, mail, and this mail's label IDs in parallel
                const [labelsList, m, perMailLabelIds] = await Promise.all([
                    fetchWithAuth("/labels", token).catch(() => []),
                    fetchWithAuth(`/mails/${id}`, token),
                    fetch(`/api/labels/mail/${id}`, {
                        headers: { Authorization: `Bearer ${token}` },
                    }).then(r => (r.ok ? r.json() : [])).catch(() => []),
                ]);
                if (killed) return;

                const draftLabelId = getDraftLabelId(labelsList) || null;

                // Robust draft detection:
                let isDraft = false;
                if (m?.isDraft === true) {
                    isDraft = true;
                } else if (draftLabelId && Array.isArray(perMailLabelIds) && perMailLabelIds.includes(draftLabelId)) {
                    isDraft = true;
                } else if (Array.isArray(m?.labels) && hasNameDraft(m.labels)) {
                    isDraft = true;
                }

                if (!isDraft && !assumeDraft) {
                    navigate(`/mail/${id}`, { replace: true });
                    return;
                }

                setMail(m);
                setFromEmail(resolveSender(m)); // <-- set sender
                setTo(m?.to || m?.recipient?.email || m?.recipient || "");
                setSubject(m?.subject || "");
                setBody(m?.body || m?.content || "");
            } catch (e) {
                if (!killed) setError("Failed to load draft");
            } finally {
                if (!killed) setLoading(false);
            }
        })();

        return () => { killed = true; };
    }, [id, token, navigate, assumeDraft]);
    // NOTE: comments in English only
    // NOTE: comments in English only


    const sendDraft = async () => {
        if (sending) return;
        setSending(true);

        const token = localStorage.getItem("token");
        const sender = (fromEmail || "").trim();
        const recipient = (to || "").trim();

        if (!sender || !sender.includes("@")) {
            alert("Sender is required (a valid email known to the system).");
            setSending(false);
            return;
        }
        if (!recipient || !recipient.includes("@")) {
            alert("Invalid recipient address.");
            setSending(false);
            return;
        }

        // map UI fields to server schema
        const payload = {
            sender,                  // REQUIRED by your API
            recipient,               // server expects 'recipient'
            subject: subject || "",
            content: body || "",     // server expects 'content'
        };

        try {
            const res = await fetch(`/api/mails`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify(payload),
            });

            // read body for helpful error
            let detail = "";
            try {
                const ct = res.headers.get("content-type") || "";
                if (ct.includes("application/json")) {
                    const j = await res.json();
                    detail = j?.message || j?.error || JSON.stringify(j);
                } else {
                    detail = await res.text();
                }
            } catch { }

            if (!(res.ok || res.status === 201 || res.status === 202)) {
                alert(`Send failed (${res.status}) ${detail || ""}`);
                setSending(false);
                return;
            }

            // optional: delete the draft now that it was sent
            if (id) {
                fetch(`/api/mails/${id}`, {
                    method: "DELETE",
                    headers: { Authorization: `Bearer ${token}` },
                }).catch(() => { });
            }

            navigate("/label/sent", { replace: true });
        } catch (e) {
            console.error(e);
            alert("Network error.");
        } finally {
            setSending(false);
        }
    };

    if (loading) return <div style={{ padding: 16 }}>Loading…</div>;
    if (error) return <div style={{ padding: 16, color: "red" }}>{error}</div>;

    return (
        <div style={{ padding: 16, maxWidth: 800, margin: "0 auto" }}>
            <h2>Draft Editor</h2>

            <div style={{ display: "grid", gap: 8, marginTop: 12 }}>
                <label style={{ fontWeight: 600 }}>
                    From
                    <input
                        value={fromEmail}
                        onChange={(e) => setFromEmail(e.target.value)}
                        style={{ width: "100%", padding: 8, marginTop: 4 }}
                        placeholder="your-verified-email@example.com"
                    />
                </label>

                <label style={{ fontWeight: 600 }}>
                    To
                    <input
                        value={to}
                        onChange={(e) => setTo(e.target.value)}
                        style={{ width: "100%", padding: 8, marginTop: 4 }}
                        placeholder="recipient@example.com"
                    />
                </label>

                <label style={{ fontWeight: 600 }}>
                    Subject
                    <input
                        value={subject}
                        onChange={(e) => setSubject(e.target.value)}
                        style={{ width: "100%", padding: 8, marginTop: 4 }}
                        placeholder="Subject"
                    />
                </label>

                <label style={{ fontWeight: 600 }}>
                    Body
                    <textarea
                        value={body}
                        onChange={(e) => setBody(e.target.value)}
                        rows={10}
                        style={{ width: "100%", padding: 8, marginTop: 4, fontFamily: "monospace" }}
                        placeholder="Write your message…"
                    />
                </label>
            </div>

            <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
                <button
                    onClick={sendDraft}
                    disabled={sending}
                    style={{
                        padding: "6px 12px",
                        border: "1px solid #28a745",
                        background: "#d4edda",
                        cursor: sending ? "not-allowed" : "pointer",
                    }}
                >
                    {sending ? "Sending..." : "Send"}
                </button>
                <button
                    onClick={() => navigate(-1)}
                    style={{ padding: "6px 12px", border: "1px solid #ccc", background: "#f1f3f4", cursor: "pointer" }}
                >
                    Back
                </button>
            </div>
        </div>
    );
};

export default DraftEditPage;
