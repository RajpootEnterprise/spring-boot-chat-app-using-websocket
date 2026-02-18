<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${pageTitle}</title>
    <style>
        /* ─── CSS Reset & Variables ─────────────────────────────────── */
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

        :root {
            --bg-dark: #0f1117;
            --bg-card: #1a1d2e;
            --bg-input: #252842;
            --accent: #6c63ff;
            --accent-hover: #5a52e0;
            --text-primary: #e8eaf6;
            --text-secondary: #9e9ea8;
            --border: #2d3053;
            --error: #ff6b6b;
            --success: #51cf66;
            --radius: 16px;
            --shadow: 0 24px 64px rgba(0,0,0,0.5);
        }

        body {
            font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
            background: var(--bg-dark);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
            background-image:
                radial-gradient(circle at 20% 50%, rgba(108,99,255,0.08) 0%, transparent 50%),
                radial-gradient(circle at 80% 20%, rgba(82,196,255,0.06) 0%, transparent 50%);
        }

        /* ─── Card ───────────────────────────────────────────────────── */
        .login-card {
            background: var(--bg-card);
            border: 1px solid var(--border);
            border-radius: var(--radius);
            padding: 48px 40px;
            width: 100%;
            max-width: 440px;
            box-shadow: var(--shadow);
            animation: slideUp 0.5s ease;
        }

        @keyframes slideUp {
            from { opacity: 0; transform: translateY(24px); }
            to   { opacity: 1; transform: translateY(0); }
        }

        /* ─── Logo / Header ──────────────────────────────────────────── */
        .logo {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 8px;
        }

        .logo-icon {
            width: 48px;
            height: 48px;
            background: linear-gradient(135deg, var(--accent), #52c4ff);
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
        }

        .logo-text {
            font-size: 24px;
            font-weight: 700;
            color: var(--text-primary);
            letter-spacing: -0.5px;
        }

        .tagline {
            color: var(--text-secondary);
            font-size: 14px;
            margin-bottom: 36px;
            margin-left: 60px;
        }

        /* ─── Form ────────────────────────────────────────────────────── */
        .form-group {
            margin-bottom: 20px;
        }

        label {
            display: block;
            font-size: 13px;
            font-weight: 600;
            color: var(--text-secondary);
            margin-bottom: 8px;
            text-transform: uppercase;
            letter-spacing: 0.8px;
        }

        input[type="text"] {
            width: 100%;
            padding: 14px 16px;
            background: var(--bg-input);
            border: 1px solid var(--border);
            border-radius: 10px;
            color: var(--text-primary);
            font-size: 15px;
            transition: border-color 0.2s, box-shadow 0.2s;
            outline: none;
        }

        input[type="text"]:focus {
            border-color: var(--accent);
            box-shadow: 0 0 0 3px rgba(108,99,255,0.15);
        }

        input[type="text"].error {
            border-color: var(--error);
        }

        .field-error {
            color: var(--error);
            font-size: 12px;
            margin-top: 6px;
            display: none;
        }

        /* ─── Button ─────────────────────────────────────────────────── */
        .btn-join {
            width: 100%;
            padding: 15px;
            background: linear-gradient(135deg, var(--accent), #52c4ff);
            border: none;
            border-radius: 10px;
            color: white;
            font-size: 15px;
            font-weight: 700;
            cursor: pointer;
            transition: opacity 0.2s, transform 0.1s;
            letter-spacing: 0.5px;
            margin-top: 8px;
            position: relative;
        }

        .btn-join:hover { opacity: 0.9; }
        .btn-join:active { transform: scale(0.98); }
        .btn-join:disabled { opacity: 0.6; cursor: not-allowed; }

        .btn-loading::after {
            content: '';
            display: inline-block;
            width: 16px;
            height: 16px;
            border: 2px solid rgba(255,255,255,0.4);
            border-top-color: white;
            border-radius: 50%;
            animation: spin 0.6s linear infinite;
            vertical-align: middle;
            margin-left: 8px;
        }

        @keyframes spin { to { transform: rotate(360deg); } }

        /* ─── Features list ─────────────────────────────────────────── */
        .features {
            margin-top: 32px;
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;
        }

        .feature {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 13px;
            color: var(--text-secondary);
        }

        .feature-icon {
            width: 28px;
            height: 28px;
            background: rgba(108,99,255,0.15);
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 14px;
            flex-shrink: 0;
        }
    </style>
</head>
<body>

<div class="login-card">
    <!-- Logo -->
    <div class="logo">
        <div class="logo-icon">💬</div>
        <span class="logo-text">ChatApp</span>
    </div>
    <p class="tagline">Real-time messaging with WebSocket</p>

    <!-- Login Form -->
    <form id="loginForm" onsubmit="handleJoin(event)">
        <div class="form-group">
            <label for="username">Your Username</label>
            <input type="text"
                   id="username"
                   name="username"
                   placeholder="e.g. john_doe"
                   maxlength="50"
                   autocomplete="off"
                   autofocus
                   required />
            <div class="field-error" id="usernameError"></div>
        </div>

        <div class="form-group">
            <label for="displayName">Display Name <span style="color:#666">(optional)</span></label>
            <input type="text"
                   id="displayName"
                   name="displayName"
                   placeholder="e.g. John Doe"
                   maxlength="100" />
        </div>

        <button type="submit" class="btn-join" id="joinBtn">
            🚀 Join Chat Room
        </button>
    </form>

    <!-- Features -->
    <div class="features">
        <div class="feature">
            <div class="feature-icon">👥</div>
            <span>Group Chat</span>
        </div>
        <div class="feature">
            <div class="feature-icon">🔒</div>
            <span>Private DMs</span>
        </div>
        <div class="feature">
            <div class="feature-icon">🖼️</div>
            <span>Image Share</span>
        </div>
        <div class="feature">
            <div class="feature-icon">⚡</div>
            <span>Real-time</span>
        </div>
    </div>
</div>

<script>
    // Validate username: lowercase letters, numbers, underscores only
    function validateUsername(value) {
        if (!value || value.trim().length < 2) return "Username must be at least 2 characters";
        if (value.trim().length > 50) return "Username must be under 50 characters";
        if (!/^[a-zA-Z0-9_]+$/.test(value.trim())) return "Only letters, numbers, and underscores allowed";
        return null;
    }

    // Show validation error
    function showError(fieldId, errorId, message) {
        document.getElementById(fieldId).classList.add('error');
        const el = document.getElementById(errorId);
        el.textContent = message;
        el.style.display = 'block';
    }

    // Clear validation error
    function clearError(fieldId, errorId) {
        document.getElementById(fieldId).classList.remove('error');
        document.getElementById(errorId).style.display = 'none';
    }

    // Live validation on input
    document.getElementById('username').addEventListener('input', function() {
        const err = validateUsername(this.value);
        if (err) showError('username', 'usernameError', err);
        else clearError('username', 'usernameError');
    });

    // Handle join form submission
    async function handleJoin(event) {
        event.preventDefault();

        const username = document.getElementById('username').value.trim().toLowerCase();
        const displayName = document.getElementById('displayName').value.trim();
        const btn = document.getElementById('joinBtn');

        // Client-side validation
        const usernameError = validateUsername(username);
        if (usernameError) {
            showError('username', 'usernameError', usernameError);
            return;
        }
        clearError('username', 'usernameError');

        // Show loading state
        btn.disabled = true;
        btn.classList.add('btn-loading');
        btn.textContent = 'Joining';

        try {
            // Call REST API to register/get user
            const response = await fetch('/api/users/join', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, displayName: displayName || username })
            });

            const data = await response.json();

            if (data.success) {
                // Redirect to chat room with username
                window.location.href = '/chat?username=' + encodeURIComponent(username);
            } else {
                alert('Error: ' + (data.error || 'Could not join chat'));
                btn.disabled = false;
                btn.classList.remove('btn-loading');
                btn.textContent = '🚀 Join Chat Room';
            }
        } catch (error) {
            console.error('Join error:', error);
            alert('Connection error. Please check if the server is running.');
            btn.disabled = false;
            btn.classList.remove('btn-loading');
            btn.textContent = '🚀 Join Chat Room';
        }
    }
</script>

</body>
</html>
