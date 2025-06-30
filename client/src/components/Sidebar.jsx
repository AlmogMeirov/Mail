import React, { useEffect, useState } from "react";
import { NavLink } from "react-router-dom";
import './Sidebar.css';

const Sidebar = () => {
  const [labels, setLabels] = useState([]);
  const [newLabel, setNewLabel] = useState("");

  useEffect(() => {
    const fetchLabels = async () => {
      try {
        const res = await fetch("/api/labels", {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
        });
        const data = await res.json();
        setLabels(data);
      } catch (err) {
        console.error("Failed to fetch labels", err);
      }
    };

    fetchLabels();
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
      <h2>MyMail</h2>
      <ul>
        <li><NavLink to="/inbox">Inbox</NavLink></li>
        <li><NavLink to="/sent">Sent</NavLink></li>

        {/* Dynamic label links */}
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
