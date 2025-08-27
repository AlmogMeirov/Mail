import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { FaStar, FaRegStar } from "react-icons/fa"; // Add star icons
import SendMailComponent from "../components/SendMailComponent";
import Loading from "../components/Loading";

function MailViewPage() {
  const { id } = useParams();
  const [mail, setMail] = useState(null);
  const [error, setError] = useState(null);
  const [showReply, setShowReply] = useState(false);
  const [showForward, setShowForward] = useState(false);
  const [showReplyAll, setShowReplyAll] = useState(false);
  const [labels, setLabels] = useState([]); // Add in exercises 4 - state to hold label names
  const [allLabels, setAllLabels] = useState([]); // All labels for starred detection
  const [starredLabelId, setStarredLabelId] = useState(null); // Starred label ID
  const [isStarred, setIsStarred] = useState(false); // Current starred status

  const token = localStorage.getItem("token");

  // Toggle starred status
  const toggleStarred = async () => {
    if (!starredLabelId || !mail?.id) return;

    try {
      if (isStarred) {
        // Remove from starred
        await fetch("/api/labels/untag", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ mailId: mail.id, labelId: starredLabelId }),
        });
        setIsStarred(false);
        setLabels(prev => prev.filter(label => label.id !== starredLabelId));
      } else {
        // Add to starred
        await fetch("/api/labels/tag", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ mailId: mail.id, labelId: starredLabelId }),
        });
        setIsStarred(true);
        const starredLabel = allLabels.find(l => l.id === starredLabelId);
        if (starredLabel) {
          setLabels(prev => [...prev, starredLabel]);
        }
      }
    } catch (error) {
      console.error('Error toggling starred status:', error);
      alert('Failed to update starred status');
    }
  };

  useEffect(() => {
    const fetchMail = async () => {
      try {
        // Fetch all labels first
        const labelsResponse = await fetch("/api/labels", {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
        const allLabelsData = await labelsResponse.json();
        setAllLabels(allLabelsData);

        // Find starred label ID
        const starred = allLabelsData.find(l => l.name.toLowerCase() === "starred");
        if (starred) {
          setStarredLabelId(starred.id);
        } else {
          // Create starred label if it doesn't exist
          const createResponse = await fetch("/api/labels", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify({ name: "Starred" }),
          });
          if (createResponse.ok) {
            const created = await createResponse.json();
            setStarredLabelId(created.id);
            setAllLabels(prev => [...prev, created]);
          }
        }

        // Fetch mail data
        const response = await fetch(`/api/mails/${id}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (!response.ok) throw new Error("Failed to fetch mail");
        const data = await response.json();
        console.log("Mail received:", data);
        setMail(data);

        // Fetch labels for this mail
        const labelsResponse2 = await fetch(`/api/labels/mail/${id}`, {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (labelsResponse2.ok) {
          const labelIds = await labelsResponse2.json();
          
          // Map label IDs to label objects
          const mailLabels = labelIds.map(labelId => 
            allLabelsData.find(l => l.id === labelId)
          ).filter(Boolean);
          
          setLabels(mailLabels);
          
          // Check if mail is starred
          if (starred) {
            setIsStarred(labelIds.includes(starred.id));
          }
        }

      } catch (err) {
        console.error(err);
        setError("Could not load mail");
      }
    };

    fetchMail();
  }, [id, token]);

  if (error) return <p>{error}</p>;
  if (!mail) return <Loading label="Loading mail…" />;

  return (
    <div style={{ padding: "1rem" }}>
      {/* Wrap the message in a "card" so background becomes gray in dark mode */}
      <div className="card mail-view"> {/* Added for Dark Mode: card container */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
          <h1 style={{ marginTop: 0, marginBottom: 0, flex: 1 }}>
            {mail.subject === null || mail.subject === undefined ? (
              <em>(no subject)</em>
            ) : (
              mail.subject
            )}
          </h1>
          
          {/* Star button */}
          {starredLabelId && (
            <button
              onClick={toggleStarred}
              style={{
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                fontSize: '24px',
                color: isStarred ? '#fbbc04' : '#5f6368',
                padding: '8px',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                transition: 'all 0.2s ease'
              }}
              onMouseOver={(e) => e.target.style.background = 'rgba(251, 188, 4, 0.1)'}
              onMouseOut={(e) => e.target.style.background = 'none'}
              title={isStarred ? "Remove star" : "Add star"}
            >
              {isStarred ? <FaStar /> : <FaRegStar />}
            </button>
          )}
        </div>

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
              border: "1px solid var(--border)" /* Added for Dark Mode: token border */,
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
              <span key={label.id} className="label-chip"> {/* Added for Dark Mode: tokenized chip */}
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
              ...mail.recipients
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
            initialContent={`\n\n--- Forwarded Message ---\nFrom: ${mail.sender?.email}\nTo: ${mail.recipient?.email}\nDate: ${new Date(
              mail.timestamp
            ).toLocaleString()}\n\n${mail.content}`}
          />
        </>
      )}
    </div>
  );
}

export default MailViewPage;