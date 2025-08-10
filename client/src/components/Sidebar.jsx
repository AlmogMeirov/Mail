import React, { useEffect, useState } from "react";
import { NavLink } from "react-router-dom";
import { useSendMail } from "../context/SendMailContext";
import './Sidebar.css';

const Sidebar = () => {
  const [labels, setLabels] = useState([]);
  const [newLabel, setNewLabel] = useState("");
  const { setShow } = useSendMail();


  useEffect(() => {
    const token = localStorage.getItem("token");

    const fetchLabelsAndEnsureTrash = async () => {
      try {
        const res = await fetch("/api/labels", {
          headers: { Authorization: `Bearer ${token}` },
        });
        const data = await res.json();
        setLabels(data);

        const trashExists = data.some(
          label => label.name.toLowerCase() === "trash"
        );

        if (!trashExists) {
          const createRes = await fetch("/api/labels", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify({ name: "Trash" }),
          });

          if (!createRes.ok) throw new Error("Failed to create Trash label");

          const created = await createRes.json();
          setLabels(prev => [...prev, created]);
        }
      } catch (err) {
        console.error("Failed to fetch or create labels", err);
      }
    };

    fetchLabelsAndEnsureTrash();
  }, []);

  const handleCreateLabel = async () => {
    if (!newLabel.trim()) return;

    try {
      const res = await fetch("/api/labels", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
        body: JSON.stringify({ name: newLabel }),
      });

      if (!res.ok) throw new Error("Failed to create label");

      const created = await res.json();
      setLabels([...labels, created]);
      setNewLabel("");
    } catch (err) {
      console.error("Error creating label:", err);
    }
  };

  return (
    <nav className="sidebar">
      <h2>FooMail</h2>
      <div className="new-mail">
        <button onClick={() => setShow(true)}>Compose</button>
      </div>
      <ul>
        {/* Static system labels */}
        <li><NavLink to="/label/inbox">Inbox</NavLink></li>
        <li><NavLink to="/label/sent">Sent</NavLink></li>

        {/* Dynamic user-created labels */}
        {labels.map(label => (
          <li key={label.id}>
            <NavLink to={`/label/${label.id}`}>{label.name}</NavLink>
          </li>
        ))}
      </ul>

      <div className="new-label-form">
        <input
          type="text"
          placeholder="New label name"
          value={newLabel}
          onChange={(e) => setNewLabel(e.target.value)}
        />
        <button onClick={handleCreateLabel}>Create Label</button>
      </div>
    </nav>
  );
};

export default Sidebar;
