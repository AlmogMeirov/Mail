import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import SendMailComponent from "../components/SendMailComponent";

function MailViewPage() {
  const { id } = useParams();
  const [mail, setMail] = useState(null);
  const [error, setError] = useState(null);
  const [showReply, setShowReply] = useState(false);
  const [showForward, setShowForward] = useState(false);

  useEffect(() => {
    const fetchMail = async () => {
      try {
        const response = await fetch(`/api/mails/${id}`, {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
        });

        if (!response.ok) throw new Error("Failed to fetch mail");
        const data = await response.json();
        setMail(data);
      } catch (err) {
        console.error(err);
        setError("Could not load mail");
      }
    };

    fetchMail();
  }, [id]);

  if (error) return <p>{error}</p>;
  if (!mail) return <p>Loading mail...</p>;

  return (
  <div style={{ padding: "1rem" }}>
    <h1>{mail.subject || <em>(no subject)</em>}</h1>

    <div style={{ display: "flex", alignItems: "center", marginBottom: "1rem" }}>
      <img
        src={mail.sender?.profileImage || "/user-svgrepo-com.svg"}
        alt="Sender avatar"
        style={{
            width: 50,
            height: 50,
            borderRadius: "50%",
            marginRight: "1rem"
        }}
     />
      <div>
         <strong>From:</strong>{" "}
          {mail.sender?.firstName} {mail.sender?.lastName} ({mail.sender?.email})<br />
          <strong>To:</strong>{" "}
          {mail.recipient?.firstName} {mail.recipient?.lastName} ({mail.recipient?.email})<br />
          <strong>Date:</strong> {new Date(mail.timestamp).toLocaleString()}
      </div>
    </div>

    <div style={{ whiteSpace: "pre-wrap", marginBottom: "1rem" }}>
      {mail.content || <em>(no content)</em>}
    </div>

    <div style={{ display: "flex", gap: "1rem" }}>
        <button onClick={() => setShowReply(true)}>Reply</button>
        <button onClick={() => setShowForward(true)}>Forward</button>
    </div>

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
                zIndex: 998
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
                zIndex: 998
            }}
            onClick={() => setShowForward(false)}
        />
        <SendMailComponent
          onClose={() => setShowForward(false)}
          initialRecipient=""
          initialSubject={
            mail.subject?.startsWith("Fwd:") ? mail.subject : `Fwd: ${mail.subject}`
          }
          initialContent={`\n\n--- Forwarded Message ---\nFrom: ${mail.sender?.email}\nTo: ${mail.recipient?.email}\nDate: ${new Date(mail.timestamp).toLocaleString()}\n\n${mail.content}`}
        />
        </>
    )}

  </div>
);
}

export default MailViewPage;
