import { useState } from "react";

export default function SendMailComponent({ onClose }) {
  const [recipient, setRecipient] = useState("");
  const [subject, setSubject] = useState("");
  const [content, setContent] = useState("");

  const handleSend = async () => {
    const token = localStorage.getItem("token");
    const sender = localStorage.getItem("email"); // Assuming email is stored in localStorage

    const res = await fetch("http://localhost:3000/api/mails", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`
      },
      body: JSON.stringify({ recipient, subject, content, sender })
    });

    if (res.ok) {
      alert("Mail sent!");
      onClose();
    } else {
      alert("Failed to send mail.");
    }
  };

  return (
    <div style={{
      position: "fixed", top: "20%", left: "50%", transform: "translateX(-50%)",
      backgroundColor: "white", padding: "1rem", border: "1px solid gray", zIndex: 999
    }}>
      <h3>Send Mail</h3>
      <input placeholder="To" value={recipient} onChange={e => setRecipient(e.target.value)} /><br />
      <input placeholder="Subject" value={subject} onChange={e => setSubject(e.target.value)} /><br />
      <textarea placeholder="Message" value={content} onChange={e => setContent(e.target.value)} /><br />
      <button onClick={handleSend}>Send</button>
      <button onClick={onClose}>Cancel</button>
    </div>
  );
}
