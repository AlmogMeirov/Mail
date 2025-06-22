import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

function MailViewPage() {
  const { id } = useParams();
  const [mail, setMail] = useState(null);
  const [error, setError] = useState(null);

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
        console.log("Fetched mail data:", data);
        console.log("Sender field:", data.sender);
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
      {mail.sender?.profileImage && (
        <img
          src={mail.sender.profileImage || "/user-svgrepo-com.svg"}
          alt="Sender avatar"
          style={{ width: 50, height: 50, borderRadius: "50%", marginRight: "1rem" }}
        />
      )}
      <div>
         <strong>From:</strong>{" "}
          {mail.sender?.firstName} {mail.sender?.lastName} ({mail.sender?.email})<br />
          <strong>To:</strong>{" "}
          {mail.recipient?.firstName} {mail.recipient?.lastName} ({mail.recipient?.email})<br />
          <strong>Date:</strong> {new Date(mail.timestamp).toLocaleString()}
      </div>
    </div>

    <div style={{ whiteSpace: "pre-wrap" }}>
      {mail.content || <em>(no content)</em>}
    </div>
  </div>
);
}

export default MailViewPage;
