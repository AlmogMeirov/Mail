import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { fetchWithAuth, moveMailToLabel } from '../utils/api';

const LabelPage = () => {
  const { labelId } = useParams();
  const [mails, setMails] = useState([]);
  const [labelName, setLabelName] = useState('');
  const [error, setError] = useState('');
  const [allLabels, setAllLabels] = useState([]);
  const [selectedTargets, setSelectedTargets] = useState({}); // ✅ בשביל הכפתור

  const token = localStorage.getItem('token');

  useEffect(() => {
    if (!token) return;

    // Fetch label name
    fetchWithAuth(`/labels/${labelId}`, token)
      .then(data => setLabelName(data?.name || labelId))
      .catch(() => setLabelName(labelId));

    // Fetch mails under this label
    fetchWithAuth(`/labels/by-label/${labelId}`, token)
      .then(data => setMails(data || []))
      .catch(err => setError(err.message));

    // Fetch all labels
    fetchWithAuth(`/labels`, token)
      .then(setAllLabels)
      .catch(console.error);

  }, [labelId, token]);

  // Move mail to another label
  const handleMove = async (mailId) => {
    const toLabelId = selectedTargets[mailId];
    if (!toLabelId) return;

    try {
      await moveMailToLabel(mailId, labelId, toLabelId, token);
      const updated = await fetchWithAuth(`/labels/by-label/${labelId}`, token);
      setMails(updated || []);
      setSelectedTargets(prev => ({ ...prev, [mailId]: '' }));
    } catch (err) {
      alert("Failed to move mail.");
      console.error(err);
    }
  };

  return (
    <div>
      <h1>{labelName}</h1>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      <ul>
        {mails.map(mailId => (
          <li key={mailId}>
            Mail ID: {mailId}
            <br />
            <label>Move to:</label>{" "}
            <select
              value={selectedTargets[mailId] || ""}
              onChange={(e) => {
                setSelectedTargets(prev => ({
                  ...prev,
                  [mailId]: e.target.value
                }));
              }}
            >
              <option value="" disabled>Select label</option>
              {allLabels
                .filter(lbl => lbl.id !== labelId)
                .map(lbl => (
                  <option key={lbl.id} value={lbl.id}>
                    {lbl.name}
                  </option>
                ))}
            </select>
            <button onClick={() => handleMove(mailId)}>Move</button>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default LabelPage;
