import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const Register = () => {
    // Local state for each field
    // Required fields
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    // Optional fields
    const [profilePicture, setProfilePicture] = useState(null);
    const [birthDate, setBirthDate] = useState('');
    const [phone, setPhone] = useState('');
    const [gender, setGender] = useState('');

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
        const payload = {
            firstName,
            lastName,
            email,
            password,
        };

        if (profilePicture) payload.profilePicture = profilePicture;
        if (birthDate) payload.birthDate = birthDate;
        if (phone) payload.phone = phone;
        if (gender) payload.gender = gender;


        try {
            const response = await fetch("http://localhost:3000/api/users/register", {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
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
                    type="date"
                    placeholder="Birth Date"
                    value={birthDate}
                    onChange={(e) => setBirthDate(e.target.value)}
                />

                <input
                    type="text"
                    placeholder="Phone Number"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                />

                <select
                    value={gender}
                    onChange={(e) => setGender(e.target.value)}
                >
                    <option value="">Select Gender</option>
                    <option value="male">Male</option>
                    <option value="female">Female</option>
                    <option value="other">Other</option>
                </select>


                <input
                    type="file"
                    accept="image/*"
                    onChange={handleImageChange}
                />

                {isLoading && <p>Registering...</p>}

                <button type="submit">Register</button>
                <p>
                    Already have an account?{" "}
                    <span onClick={() => navigate("/login")} style={{ color: "blue", cursor: "pointer" }}>
                        Login here
                    </span>
                </p>

            </form>
        </div>
    );
};

export default Register;
