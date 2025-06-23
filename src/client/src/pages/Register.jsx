import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const Register = () => {
    // Local state for each field
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [profilePicture, setProfilePicture] = useState(null);
    const [errorMessage, setErrorMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const navigate = useNavigate();

    // Validate inputs before sending
    const validateForm = () => {
        if (!firstName || !lastName || !email || !password || !confirmPassword) {
            setErrorMessage("All fields must be filled (except profile picture)");
            return false;
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            setErrorMessage("Invalid email format");
            return false;
        }

        if (password.length < 6) {
            setErrorMessage("Password must be at least 6 characters");
            return false;
        }

        if (password !== confirmPassword) {
            setErrorMessage("Passwords do not match");
            return false;
        }

        setErrorMessage('');
        return true;
    };

    // Handle profile picture as base64
    const handleImageChange = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onloadend = () => {
            setProfilePicture(reader.result);
        };
        reader.readAsDataURL(file);
    };

    // Submit registration form
    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validateForm()) return;

        setIsLoading(true);
        setErrorMessage('');

        try {
            const response = await fetch("http://localhost:3000/api/users/register", {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    firstName,
                    lastName,
                    email,
                    password,
                    profilePicture,
                }),
            });

            if (response.status === 201) {
                navigate('/login');
            } else {
                const data = await response.json();
                setErrorMessage(data?.error || "Registration failed");
            }
        } catch (err) {
            console.error("Network or unexpected error:", err);
            setErrorMessage("Could not connect to server");
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div>
            <h2>Register</h2>

            {errorMessage && (
                <div style={{
                    color: 'white',
                    backgroundColor: 'red',
                    padding: '10px',
                    borderRadius: '5px',
                    marginBottom: '15px'
                }}>
                    {errorMessage}
                </div>
            )}

            <form onSubmit={handleSubmit}>
                <input
                    type="text"
                    placeholder="First Name"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    required
                />

                <input
                    type="text"
                    placeholder="Last Name"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    required
                />

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

                <input
                    type="password"
                    placeholder="Confirm Password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                />

                <input
                    type="file"
                    accept="image/*"
                    onChange={handleImageChange}
                />

                {isLoading && <p>Registering...</p>}

                <button type="submit">Register</button>
            </form>
        </div>
    );
};

export default Register;
