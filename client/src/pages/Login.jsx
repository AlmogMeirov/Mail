// src/pages/Login.jsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const Login = () => {
    // --- state ---
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [errorMessage, setError] = useState('');
    const [isLoading, setLoading] = useState(false);

    const navigate = useNavigate();

    // --- simple client-side validation ---
    const validate = () => {
        if (!email || !password) {
            setError('Both fields are required');
            return false;
        }
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            setError('Invalid email format');
            return false;
        }
        setError('');
        return true;
    };

    // --- handle form submit ---
    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!validate()) return;

        setLoading(true);
        setError('');

        try {
            const res = await fetch('http://localhost:3000/api/users/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password }),
            });

            if (res.status === 200) {
                const { token } = await res.json();
                localStorage.setItem('token', token);
                navigate('/inbox');
            } else {
                let errorMsg = 'Login failed';
                try {
                    const data = await res.json();
                    errorMsg = data?.error || errorMsg;
                } catch (jsonErr) {
                    console.warn('Failed to parse error JSON:', jsonErr);
                }
                setError(errorMsg);
            }
        } catch (err) {
            console.error('Network error:', err);
            setError('Could not connect to server');
        } finally {
            setLoading(false);
        }
    }

    // --- render ---
    return (
        <div>
            <h2>Login</h2>

            {errorMessage && (
                <div style={{
                    backgroundColor: 'red',
                    color: 'white',
                    padding: '10px',
                    borderRadius: '5px',
                    marginBottom: '15px'
                }}>
                    {errorMessage}
                </div>
            )}

            <form onSubmit={handleSubmit}>
                <input
                    type="email"
                    placeholder="Email Address"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                />

                <input
                    type="password"
                    placeholder="Password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                />

                {isLoading && <p>Logging in...</p>}

                <button type="submit">Login</button>
            </form>
        </div>
    );
};

export default Login;
