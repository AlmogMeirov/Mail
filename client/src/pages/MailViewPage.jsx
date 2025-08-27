// MailViewPage.jsx - Complete with read status tracking and event dispatch

import React, { useEffect, useState } from "react";
import { useParams, useLocation } from "react-router-dom";
import SendMailComponent from "../components/SendMailComponent";
import Loading from "../components/Loading";

function MailViewPage() {
  const { id } = useParams();
  const location = useLocation();
  const [mail, setMail] = useState(null);
  const [error, setError] = useState(null);
  const [showReply, setShowReply] = useState(false);
  const [showForward, setShowForward] = useState(false);
  const [showReplyAll, setShowReplyAll] = useState(false);
  const [labels, setLabels] = useState([]);

  // Enhanced function to mark mail as read with event dispatch
  const markMailAsRead = (mailId) => {
    try {
      const stored = localStorage.getItem('readMails');
      let readMails = stored ? JSON.parse(stored) : [];
      
      if (!readMails.includes(mailId)) {
        readMails.push(mailId);
        localStorage.setItem('readMails', JSON.stringify(readMails));
        
        // Dispatch event to notify other components about the change
        window.dispatchEvent(new CustomEvent('readMailsUpdated'));
        
        console.log(`✅ Marked mail ${mailId} as read`, readMails);
      } else {
        console.log(`📧 Mail ${mailId} was already marked as read`);
      }
    } catch (error) {
      console.error("❌ Error updating read status:", error);
    }
  };

  useEffect(() => {
    const fetchMail = async () => {
      try {
        console.log(`🔍 Fetching mail ${id}...`);
        
        const response = await fetch(`/api/mails/${id}`, {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
        });

        if (!response.ok) throw new Error("Failed to fetch mail");
        const data = await response.json();
        
        console.log("📬 Mail received:", data);
        setMail(data);
        setLabels(data.labels || []);
        
        // Mark this mail as read when it's opened
        markMailAsRead(id);
        
      } catch (err) {
        console.error("❌ Error fetching mail:", err);
        setError("Could not load mail");
      }
    };

    fetchMail();
  }, [id, location]);

  if (error) return <p style={{ color: 'red', padding: '1rem' }}>{error}</p>;
  if (!mail) return <Loading label="Loading mail…" />;

  return (
    <div style={{ padding: "1rem" }}>
      {/* Wrap the message in a "card" so background becomes gray in dark mode */}
      <div className="card mail-view">
        <h1 style={{ marginTop: 0 }}>
          {mail.subject === null || mail.subject === undefined ? (
            <em>(no subject)</em>
          ) : (
            mail.subject
          )}
        </h1>

        <div style={{ display: "flex", alignItems: "center", marginBottom: "1rem" }}>
          <img
            src={
              mail.sender?.profileImage?.startsWith("data:image")
                ? mail.sender.profileImage
                : mail.sender?.profileImage || "/user-svgrepo-com.svg"
            }
            alt="Sender avatar"
            style={{
              width: 50,
              height: 50,
              borderRadius: "50%",
              marginRight: "1rem",
              border: "1px solid var(--border)",
            }}
          />
          <div>
            <strong>From:</strong>{" "}
            {mail.sender?.firstName} {mail.sender?.lastName} ({mail.sender?.email})<br />
            <strong>To:</strong>{" "}
            <span>
              {Array.isArray(mail.recipients)
                ? mail.recipients
                    .map((r) => `${r.firstName || ""} ${r.lastName || ""} (${r.email})`)
                    .join(", ")
                : `${mail.recipient?.firstName || ""} ${mail.recipient?.lastName || ""} (${mail.recipient?.email})`}
              <br />
            </span>
            <strong>Date:</strong> {new Date(mail.timestamp).toLocaleString()}
          </div>
        </div>

        {/* Labels */}
        {labels.length > 0 && (
          <div style={{ marginBottom: "1rem" }}>
            <strong>Labels:</strong>{" "}
            {labels.map((label) => (
              <span key={label.id} className="label-chip">
                {label.name}
              </span>
            ))}
          </div>
        )}

        <div style={{ whiteSpace: "pre-wrap", marginBottom: "1rem" }}>
          {mail.content === null || mail.content === undefined ? (
            <em>(no content)</em>
          ) : (
            mail.content
          )}
        </div>

        <div style={{ display: "flex", gap: "1rem" }}>
          <button onClick={() => setShowReply(true)}>Reply</button>
          <button onClick={() => setShowReplyAll(true)}>Reply All</button>
          <button onClick={() => setShowForward(true)}>Forward</button>
        </div>
      </div>

      {/* Reply modal */}
      {showReply && (
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
            onClick={() => setShowReply(false)}
          />
          <SendMailComponent
            onClose={() => setShowReply(false)}
            initialRecipient={mail.sender?.email}
            initialSubject={
              mail.subject?.startsWith("Re:") ? mail.subject : `Re: ${mail.subject}`
            }
            initialContent={`\n\n--- Original Message ---\n${mail.content}`}
          />
        </>
      )}

      {/* Reply-all modal */}
      {showReplyAll && (
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
            onClick={() => setShowReplyAll(false)}
          />
          <SendMailComponent
            onClose={() => setShowReplyAll(false)}
            initialRecipient={[
              mail.sender?.email,
              ...(Array.isArray(mail.recipients) ? mail.recipients : [])
                .filter((r) => r.email && r.email !== localStorage.getItem("email"))
                .map((r) => r.email),
            ]
              .filter((email) => email && email !== localStorage.getItem("email"))
              .join(", ")}
            initialSubject={
              mail.subject?.startsWith("Re:") ? mail.subject : `Re: ${mail.subject}`
            }
            initialContent={`\n\n--- Original Message ---\n${mail.content}`}
          />
        </>
      )}

      {/* Forward modal */}
      {showForward && (
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
            onClick={() => setShowForward(false)}
          />
          <SendMailComponent
            onClose={() => setShowForward(false)}
            initialRecipient=""
            initialSubject={
              mail.subject?.startsWith("Fwd:") ? mail.subject : `Fwd: ${mail.subject}`
            }
            initialContent={`\n\n--- Forwarded Message ---\nFrom: ${mail.sender?.email}\nTo: ${
              Array.isArray(mail.recipients) 
                ? mail.recipients.map(r => r.email).join(", ")
                : mail.recipient?.email
            }\nDate: ${new Date(mail.timestamp).toLocaleString()}\n\n${mail.content}`}
          />
        </>
      )}
    </div>
  );
}

export default MailViewPage;