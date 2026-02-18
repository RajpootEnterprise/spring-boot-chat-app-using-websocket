<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${pageTitle}</title>

    <!-- SockJS + STOMP from CDN -->
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/bundles/stomp.umd.min.js"></script>

    <style>
        /* ─── Variables ─────────────────────────────────────────────── */
        :root {
            --bg: #0f1117;
            --sidebar: #141621;
            --chat-bg: #1a1d2e;
            --header: #1f2235;
            --bubble-self: #4f46e5;
            --bubble-other: #252842;
            --bubble-system: transparent;
            --text: #e8eaf6;
            --text-muted: #787890;
            --text-dim: #4a4a6a;
            --accent: #6c63ff;
            --accent2: #52c4ff;
            --border: #2a2d4a;
            --online: #51cf66;
            --danger: #ff6b6b;
            --radius: 18px;
            --input-h: 56px;
        }

        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

        body {
            font-family: 'Segoe UI', system-ui, sans-serif;
            background: var(--bg);
            color: var(--text);
            height: 100vh;
            display: flex;
            overflow: hidden;
        }

        /* ─── Layout ─────────────────────────────────────────────────── */
        #sidebar {
            width: 280px;
            min-width: 260px;
            background: var(--sidebar);
            border-right: 1px solid var(--border);
            display: flex;
            flex-direction: column;
            height: 100vh;
        }

        #mainArea {
            flex: 1;
            display: flex;
            flex-direction: column;
            height: 100vh;
            position: relative;
        }

        /* ─── Sidebar Header ─────────────────────────────────────────── */
        .sidebar-header {
            padding: 20px 16px 16px;
            border-bottom: 1px solid var(--border);
        }

        .app-logo {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 16px;
        }

        .app-logo-icon {
            font-size: 22px;
            background: linear-gradient(135deg, var(--accent), var(--accent2));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        .app-logo-text {
            font-size: 16px;
            font-weight: 700;
            letter-spacing: -0.3px;
        }

        .current-user {
            display: flex;
            align-items: center;
            gap: 10px;
            background: rgba(108,99,255,0.1);
            border: 1px solid rgba(108,99,255,0.2);
            border-radius: 12px;
            padding: 10px 12px;
        }

        .avatar {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 14px;
            font-weight: 700;
            flex-shrink: 0;
        }

        .avatar-self { background: linear-gradient(135deg, var(--accent), var(--accent2)); color: white; }
        .avatar-other { color: white; }

        .user-info { flex: 1; min-width: 0; }
        .user-name { font-size: 13px; font-weight: 600; color: var(--text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .user-status { font-size: 11px; color: var(--online); display: flex; align-items: center; gap: 4px; }
        .online-dot { width: 6px; height: 6px; background: var(--online); border-radius: 50%; }

        /* ─── Sidebar Sections ───────────────────────────────────────── */
        .sidebar-section {
            padding: 12px 16px 4px;
        }

        .section-label {
            font-size: 10px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 1.2px;
            color: var(--text-dim);
            margin-bottom: 8px;
        }

        /* Group Chat Button */
        .group-chat-btn {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 10px 12px;
            border-radius: 10px;
            cursor: pointer;
            transition: background 0.15s;
            background: rgba(108,99,255,0.15);
            border: 1px solid rgba(108,99,255,0.3);
        }

        .group-chat-btn:hover { background: rgba(108,99,255,0.25); }

        .group-chat-btn.active {
            background: rgba(108,99,255,0.3);
            border-color: rgba(108,99,255,0.5);
        }

        /* Online Users List */
        #onlineUsersList {
            flex: 1;
            overflow-y: auto;
            padding: 8px 16px;
        }

        #onlineUsersList::-webkit-scrollbar { width: 4px; }
        #onlineUsersList::-webkit-scrollbar-track { background: transparent; }
        #onlineUsersList::-webkit-scrollbar-thumb { background: var(--border); border-radius: 2px; }

        .user-item {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 8px 10px;
            border-radius: 10px;
            cursor: pointer;
            transition: background 0.15s;
            margin-bottom: 2px;
            position: relative;
        }

        .user-item:hover { background: rgba(255,255,255,0.05); }
        .user-item.active { background: rgba(108,99,255,0.15); }

        .user-item .online-indicator {
            width: 8px;
            height: 8px;
            background: var(--online);
            border-radius: 50%;
            position: absolute;
            bottom: 8px;
            left: 34px;
            border: 2px solid var(--sidebar);
        }

        .unread-badge {
            margin-left: auto;
            background: var(--danger);
            color: white;
            font-size: 10px;
            font-weight: 700;
            padding: 2px 6px;
            border-radius: 10px;
            min-width: 18px;
            text-align: center;
        }

        /* ─── Chat Header ────────────────────────────────────────────── */
        #chatHeader {
            background: var(--header);
            border-bottom: 1px solid var(--border);
            padding: 16px 20px;
            display: flex;
            align-items: center;
            gap: 12px;
        }

        #chatTitle { font-size: 16px; font-weight: 600; }
        #chatSubtitle { font-size: 12px; color: var(--text-muted); }

        #connectionStatus {
            margin-left: auto;
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 12px;
            font-weight: 600;
        }

        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            background: var(--text-muted);
        }

        .status-dot.connected { background: var(--online); }
        .status-dot.disconnected { background: var(--danger); }
        .status-dot.connecting { background: #ffa500; animation: pulse 1s infinite; }

        @keyframes pulse { 0%,100% { opacity:1; } 50% { opacity:0.4; } }

        /* ─── Messages Area ──────────────────────────────────────────── */
        #messagesArea {
            flex: 1;
            overflow-y: auto;
            padding: 20px;
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        #messagesArea::-webkit-scrollbar { width: 6px; }
        #messagesArea::-webkit-scrollbar-track { background: transparent; }
        #messagesArea::-webkit-scrollbar-thumb { background: var(--border); border-radius: 3px; }

        /* ─── Message Bubbles ────────────────────────────────────────── */
        .message-row {
            display: flex;
            flex-direction: column;
            max-width: 75%;
            gap: 2px;
        }

        .message-row.self { align-self: flex-end; align-items: flex-end; }
        .message-row.other { align-self: flex-start; align-items: flex-start; }
        .message-row.system { align-self: center; align-items: center; max-width: 90%; }

        .message-sender {
            font-size: 11px;
            font-weight: 600;
            color: var(--text-muted);
            margin-bottom: 2px;
            padding: 0 6px;
        }

        .bubble {
            padding: 10px 14px;
            border-radius: 16px;
            font-size: 14px;
            line-height: 1.5;
            word-wrap: break-word;
            position: relative;
        }

        .bubble.self {
            background: var(--bubble-self);
            color: white;
            border-bottom-right-radius: 4px;
        }

        .bubble.other {
            background: var(--bubble-other);
            color: var(--text);
            border-bottom-left-radius: 4px;
            border: 1px solid var(--border);
        }

        .bubble.system {
            background: rgba(255,255,255,0.04);
            color: var(--text-muted);
            font-size: 12px;
            padding: 6px 14px;
            border-radius: 20px;
            font-style: italic;
        }

        .message-time {
            font-size: 10px;
            color: var(--text-dim);
            padding: 0 6px;
        }

        /* Image in chat */
        .chat-image {
            max-width: 280px;
            max-height: 200px;
            border-radius: 10px;
            cursor: pointer;
            transition: opacity 0.2s;
            display: block;
        }

        .chat-image:hover { opacity: 0.9; }

        /* Image caption */
        .image-caption {
            font-size: 12px;
            color: rgba(255,255,255,0.7);
            margin-top: 4px;
        }

        /* ─── Date Separator ────────────────────────────────────────── */
        .date-separator {
            text-align: center;
            font-size: 11px;
            color: var(--text-dim);
            margin: 12px 0;
            position: relative;
        }

        .date-separator::before {
            content: '';
            position: absolute;
            top: 50%;
            left: 0;
            right: 0;
            height: 1px;
            background: var(--border);
        }

        .date-separator span {
            background: var(--chat-bg);
            padding: 0 12px;
            position: relative;
        }

        /* ─── Input Area ─────────────────────────────────────────────── */
        #inputArea {
            background: var(--header);
            border-top: 1px solid var(--border);
            padding: 12px 20px;
        }

        #imagePreviewBar {
            display: none;
            align-items: center;
            gap: 10px;
            margin-bottom: 10px;
            background: rgba(108,99,255,0.1);
            border-radius: 10px;
            padding: 8px 12px;
        }

        #imagePreviewBar img {
            width: 50px;
            height: 50px;
            object-fit: cover;
            border-radius: 8px;
        }

        .preview-info { flex: 1; font-size: 12px; color: var(--text-muted); }
        .remove-preview { cursor: pointer; color: var(--danger); font-size: 18px; }

        .input-row {
            display: flex;
            align-items: flex-end;
            gap: 10px;
        }

        #messageInput {
            flex: 1;
            background: var(--bg-input, #252842);
            border: 1px solid var(--border);
            border-radius: 12px;
            padding: 14px 16px;
            color: var(--text);
            font-size: 14px;
            resize: none;
            max-height: 120px;
            min-height: 50px;
            outline: none;
            transition: border-color 0.2s;
            line-height: 1.4;
        }

        #messageInput:focus { border-color: var(--accent); }
        #messageInput::placeholder { color: var(--text-dim); }

        .icon-btn {
            width: 44px;
            height: 44px;
            border: none;
            border-radius: 10px;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            transition: background 0.15s, transform 0.1s;
            flex-shrink: 0;
        }

        .icon-btn:active { transform: scale(0.93); }
        .icon-btn.image-btn { background: rgba(82,196,255,0.15); color: var(--accent2); }
        .icon-btn.image-btn:hover { background: rgba(82,196,255,0.25); }
        .icon-btn.send-btn { background: var(--accent); color: white; }
        .icon-btn.send-btn:hover { background: #5a52e0; }
        .icon-btn.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }

        #imageFileInput { display: none; }

        /* ─── Lightbox ───────────────────────────────────────────────── */
        #lightbox {
            display: none;
            position: fixed;
            inset: 0;
            background: rgba(0,0,0,0.92);
            z-index: 1000;
            align-items: center;
            justify-content: center;
            cursor: pointer;
        }

        #lightbox.open { display: flex; }
        #lightbox img { max-width: 90vw; max-height: 90vh; border-radius: 8px; }

        /* ─── Toast Notifications ────────────────────────────────────── */
        #toastContainer {
            position: fixed;
            bottom: 80px;
            right: 20px;
            z-index: 999;
            display: flex;
            flex-direction: column;
            gap: 8px;
        }

        .toast {
            background: #252842;
            border: 1px solid var(--border);
            border-radius: 10px;
            padding: 12px 16px;
            font-size: 13px;
            color: var(--text);
            animation: toastIn 0.3s ease;
            max-width: 280px;
        }

        @keyframes toastIn {
            from { opacity:0; transform: translateX(20px); }
            to   { opacity:1; transform: translateX(0); }
        }

        /* ─── Responsive ─────────────────────────────────────────────── */
        @media (max-width: 700px) {
            #sidebar { width: 240px; min-width: 200px; }
        }
    </style>
</head>
<body>

<!-- Sidebar -->
<div id="sidebar">
    <div class="sidebar-header">
        <div class="app-logo">
            <span class="app-logo-icon">💬</span>
            <span class="app-logo-text">ChatApp</span>
        </div>
        <div class="current-user">
            <div class="avatar avatar-self" id="selfAvatar">?</div>
            <div class="user-info">
                <div class="user-name" id="selfName">${username}</div>
                <div class="user-status">
                    <div class="online-dot"></div>
                    <span>You</span>
                </div>
            </div>
        </div>
    </div>

    <!-- Group Chat -->
    <div class="sidebar-section">
        <div class="section-label">Channels</div>
        <div class="group-chat-btn active" id="groupChatBtn" onclick="switchToGroup()">
            <div class="avatar" style="background:linear-gradient(135deg,#4f46e5,#818cf8);">👥</div>
            <div class="user-info">
                <div class="user-name">Group Chat</div>
                <div style="font-size:11px;color:var(--text-muted)">Everyone</div>
            </div>
        </div>
    </div>

    <!-- Online Users -->
    <div class="sidebar-section">
        <div class="section-label">Online — <span id="onlineCount">0</span></div>
    </div>
    <div id="onlineUsersList">
        <div style="color:var(--text-dim);font-size:13px;padding:8px 10px">Connecting...</div>
    </div>
</div>

<!-- Main Chat Area -->
<div id="mainArea">
    <!-- Header -->
    <div id="chatHeader">
        <div style="font-size:24px">👥</div>
        <div>
            <div id="chatTitle">Group Chat</div>
            <div id="chatSubtitle">Welcome to the group!</div>
        </div>
        <div id="connectionStatus">
            <div class="status-dot connecting" id="statusDot"></div>
            <span id="statusText">Connecting...</span>
        </div>
    </div>

    <!-- Messages -->
    <div id="messagesArea"></div>

    <!-- Input Area -->
    <div id="inputArea">
        <div id="imagePreviewBar">
            <img id="previewImg" src="" alt="preview">
            <div class="preview-info" id="previewInfo">Image ready</div>
            <span class="remove-preview" onclick="clearImagePreview()" title="Remove">✕</span>
        </div>
        <div class="input-row">
            <button class="icon-btn image-btn" onclick="triggerImageUpload()" title="Share Image">🖼️</button>
            <input type="file" id="imageFileInput" accept="image/*" onchange="handleImageSelected(event)">
            <textarea id="messageInput"
                      placeholder="Type a message... (Enter to send)"
                      rows="1"
                      onkeydown="handleKeyDown(event)"
                      oninput="autoResize(this)"></textarea>
            <button class="icon-btn send-btn" id="sendBtn" onclick="sendMessage()" title="Send">➤</button>
        </div>
    </div>
</div>

<!-- Lightbox for full-size images -->
<div id="lightbox" onclick="closeLightbox()">
    <img id="lightboxImg" src="" alt="">
</div>

<!-- Toast notifications -->
<div id="toastContainer"></div>

<!-- Hidden: current user data from server -->
<script>
    // ─── App State ────────────────────────────────────────────────────────────
    const CURRENT_USER = "${username}";
    const WS_ENDPOINT  = "${wsEndpoint}";
    let   stompClient  = null;
    let   activeChat   = { type: 'group', username: null }; // Current chat context
    let   selectedFile = null; // Pending image upload
    let   unreadCounts = {}; // Track unread per user

    // ─── WebSocket Connection Setup ───────────────────────────────────────────

    function connectWebSocket() {
        setConnectionStatus('connecting');

        // SockJS provides fallbacks (xhr-streaming, xhr-polling, etc.)
        const socket = new SockJS(WS_ENDPOINT);

        // STOMP client wraps SockJS
        stompClient = new StompJs.Client({
            webSocketFactory: () => socket,
            reconnectDelay: 5000,         // Auto-reconnect after 5s
            heartbeatIncoming: 25000,     // Expect heartbeat every 25s
            heartbeatOutgoing: 25000,

            onConnect: onWebSocketConnected,
            onDisconnect: onWebSocketDisconnected,
            onStompError: onStompError
        });

        stompClient.activate();
    }

    function onWebSocketConnected(frame) {
        console.log('WebSocket connected:', frame);
        setConnectionStatus('connected');

        // 1. Subscribe to GROUP CHAT messages
        stompClient.subscribe('/topic/group-chat', onGroupMessageReceived);

        // 2. Subscribe to PRIVATE messages (Spring resolves /user/queue/private
        //    to /user/{CURRENT_USER}/queue/private based on session)
        stompClient.subscribe('/user/queue/private', onPrivateMessageReceived);

        // 3. Subscribe to PRESENCE events (users joining/leaving)
        stompClient.subscribe('/topic/presence', onPresenceEvent);

        // 4. Subscribe to ONLINE USERS list updates
        stompClient.subscribe('/topic/online-users', onOnlineUsersUpdate);

        // 5. Notify server that this user has joined
        stompClient.publish({
            destination: '/app/chat.join',
            body: JSON.stringify({
                username: CURRENT_USER,
                displayName: CURRENT_USER
            })
        });

        // 6. Load message history via REST API
        loadGroupHistory();
    }

    function onWebSocketDisconnected() {
        console.log('WebSocket disconnected');
        setConnectionStatus('disconnected');
    }

    function onStompError(frame) {
        console.error('STOMP error:', frame);
        setConnectionStatus('disconnected');
        showToast('⚠️ Connection error. Reconnecting...');
    }

    // ─── Message Handlers ─────────────────────────────────────────────────────

    function onGroupMessageReceived(stompMessage) {
        const msg = JSON.parse(stompMessage.body);
        console.log('Group message received:', msg);

        // Only show in messages area if currently viewing group chat
        if (activeChat.type === 'group') {
            appendMessage(msg, 'group');
        }
        // (In a more complex UI, you'd show a badge indicator)
    }

    function onPrivateMessageReceived(stompMessage) {
        const msg = JSON.parse(stompMessage.body);
        console.log('Private message received:', msg);

        const otherUser = msg.senderUsername === CURRENT_USER
                        ? msg.receiverUsername
                        : msg.senderUsername;

        // Show message if it's the current private chat window
        if (activeChat.type === 'private' && activeChat.username === otherUser) {
            appendMessage(msg, 'private');
        } else {
            // Show notification badge and toast for background messages
            if (msg.senderUsername !== CURRENT_USER) {
                incrementUnread(msg.senderUsername);
                showToast('💬 New message from ' + msg.senderUsername + ': ' +
                         (msg.content ? msg.content.substring(0, 50) : '[Image]'));
            }
        }
    }

    function onPresenceEvent(stompMessage) {
        const event = JSON.parse(stompMessage.body);
        console.log('Presence event:', event);
        // Online users list update is handled separately via /topic/online-users
    }

    function onOnlineUsersUpdate(stompMessage) {
        const users = JSON.parse(stompMessage.body);
        renderOnlineUsers(users);
    }

    // ─── Load History ────────────────────────────────────────────────────────

    async function loadGroupHistory() {
        try {
            const response = await fetch('/api/messages/group?limit=50');
            const data = await response.json();
            if (data.success && data.data) {
                data.data.forEach(msg => appendMessage(msg, 'group'));
                scrollToBottom();
            }
        } catch (e) {
            console.error('Failed to load group history:', e);
        }
    }

    async function loadPrivateHistory(otherUsername) {
        clearMessages();
        try {
            const response = await fetch('/api/messages/private/' + CURRENT_USER + '/' + otherUsername + '?limit=50');
            const data = await response.json();
            if (data.success && data.data) {
                data.data.forEach(msg => appendMessage(msg, 'private'));
                scrollToBottom();
            }
        } catch (e) {
            console.error('Failed to load private history:', e);
        }
    }

    // ─── Send Messages ────────────────────────────────────────────────────────

    function sendMessage() {
        const input = document.getElementById('messageInput');
        const content = input.value.trim();

        // Must have text content or an image selected
        if (!content && !selectedFile) return;

        // If image is selected, upload it first, then send
        if (selectedFile) {
            uploadAndSendImage(content);
            return;
        }

        // Text-only message
        const payload = {
            senderUsername: CURRENT_USER,
            content: content,
            messageType: 'TEXT',
            groupMessage: activeChat.type === 'group',
            receiverUsername: activeChat.type === 'private' ? activeChat.username : null
        };

        const destination = activeChat.type === 'group'
                          ? '/app/chat.sendGroupMessage'
                          : '/app/chat.sendPrivateMessage';

        stompClient.publish({
            destination: destination,
            body: JSON.stringify(payload)
        });

        input.value = '';
        input.style.height = 'auto';
    }

    async function uploadAndSendImage(caption) {
        const formData = new FormData();
        formData.append('file', selectedFile);
        formData.append('senderUsername', CURRENT_USER);
        formData.append('isGroupMessage', activeChat.type === 'group');
        if (activeChat.type === 'private') formData.append('receiverUsername', activeChat.username);
        if (caption) formData.append('caption', caption);

        try {
            const response = await fetch('/api/images/upload', {
                method: 'POST',
                body: formData
            });
            const data = await response.json();

            if (data.success) {
                const imageUrl = data.data.imageUrl;

                // Send WebSocket message with image URL
                const payload = {
                    senderUsername: CURRENT_USER,
                    content: caption || '',
                    messageType: 'IMAGE',
                    imageUrl: imageUrl,
                    imageContentType: selectedFile.type,
                    groupMessage: activeChat.type === 'group',
                    receiverUsername: activeChat.type === 'private' ? activeChat.username : null
                };

                const destination = activeChat.type === 'group'
                                  ? '/app/chat.sendGroupMessage'
                                  : '/app/chat.sendPrivateMessage';

                stompClient.publish({
                    destination: destination,
                    body: JSON.stringify(payload)
                });

                clearImagePreview();
                document.getElementById('messageInput').value = '';
            } else {
                showToast('❌ Image upload failed: ' + (data.error || 'Unknown error'));
            }
        } catch (e) {
            console.error('Upload error:', e);
            showToast('❌ Image upload failed. Check your connection.');
        }
    }

    // ─── Render Messages ──────────────────────────────────────────────────────

    function appendMessage(msg, chatType) {
        const area = document.getElementById('messagesArea');
        const isSelf = msg.senderUsername === CURRENT_USER;
        const isSystem = msg.messageType === 'SYSTEM';

        const row = document.createElement('div');
        row.className = 'message-row ' + (isSystem ? 'system' : isSelf ? 'self' : 'other');

        if (!isSystem && !isSelf) {
            const senderEl = document.createElement('div');
            senderEl.className = 'message-sender';
            senderEl.textContent = msg.senderDisplayName || msg.senderUsername;
            row.appendChild(senderEl);
        }

        const bubble = document.createElement('div');
        bubble.className = 'bubble ' + (isSystem ? 'system' : isSelf ? 'self' : 'other');

        if (msg.messageType === 'IMAGE') {
            // Image message
            const img = document.createElement('img');
            img.className = 'chat-image';
            img.src = msg.imageUrl || ('data:' + msg.imageContentType + ';base64,' + msg.imageBase64);
            img.alt = 'Shared image';
            img.onclick = () => openLightbox(img.src);
            img.onerror = () => { img.style.display = 'none'; };
            bubble.appendChild(img);

            if (msg.content) {
                const caption = document.createElement('div');
                caption.className = 'image-caption';
                caption.textContent = msg.content;
                bubble.appendChild(caption);
            }
        } else {
            // Text message (escape HTML to prevent XSS)
            bubble.textContent = msg.content;
        }

        row.appendChild(bubble);

        // Timestamp
        if (msg.timestamp) {
            const timeEl = document.createElement('div');
            timeEl.className = 'message-time';
            timeEl.textContent = formatTime(msg.timestamp);
            row.appendChild(timeEl);
        }

        area.appendChild(row);
        scrollToBottom();
    }

    function clearMessages() {
        document.getElementById('messagesArea').innerHTML = '';
    }

    // ─── Chat Switching ───────────────────────────────────────────────────────

    function switchToGroup() {
        activeChat = { type: 'group', username: null };

        document.getElementById('chatTitle').textContent = 'Group Chat';
        document.getElementById('chatSubtitle').textContent = 'Everyone';
        document.querySelector('.group-chat-btn').classList.add('active');

        // Deactivate user items
        document.querySelectorAll('.user-item').forEach(el => el.classList.remove('active'));

        clearMessages();
        loadGroupHistory();
    }

    function switchToPrivate(username) {
        activeChat = { type: 'private', username: username };

        document.getElementById('chatTitle').textContent = '@ ' + username;
        document.getElementById('chatSubtitle').textContent = 'Private conversation';
        document.querySelector('.group-chat-btn').classList.remove('active');

        // Highlight active user
        document.querySelectorAll('.user-item').forEach(el => {
            el.classList.toggle('active', el.dataset.username === username);
        });

        // Clear unread badge
        clearUnread(username);

        clearMessages();
        loadPrivateHistory(username);
    }

    // ─── Online Users Sidebar ────────────────────────────────────────────────

    function renderOnlineUsers(users) {
        const list = document.getElementById('onlineUsersList');
        const others = users.filter(u => u.username !== CURRENT_USER);

        document.getElementById('onlineCount').textContent = users.length;
        list.innerHTML = '';

        if (others.length === 0) {
            list.innerHTML = '<div style="color:var(--text-dim);font-size:13px;padding:8px 10px">No other users online</div>';
            return;
        }

        others.forEach(user => {
            const item = document.createElement('div');
            item.className = 'user-item';
            item.dataset.username = user.username;
            if (activeChat.type === 'private' && activeChat.username === user.username) {
                item.classList.add('active');
            }
            item.onclick = () => switchToPrivate(user.username);

            const initials = (user.displayName || user.username).substring(0, 2).toUpperCase();
            const color = userColor(user.username);
            const unreadBadge = (unreadCounts[user.username] || 0) > 0
                ? `<div class="unread-badge">${unreadCounts[user.username]}</div>`
                : '';

            // Create structure without user data first
            item.innerHTML = `
                <div class="avatar" style="background:${color};">${initials}</div>
                <div class="online-indicator"></div>
                <div class="user-info">
                    <div class="user-name"></div>
                    <div style="font-size:11px;color:var(--online)">● Online</div>
                </div>
                ${unreadBadge}
            `;

            // Safely set user display name using textContent (auto-escapes HTML)
            item.querySelector('.user-name').textContent = user.displayName || user.username;

            list.appendChild(item);
        });
    }

    // ─── Image Handling ───────────────────────────────────────────────────────

    function triggerImageUpload() {
        document.getElementById('imageFileInput').click();
    }

    function handleImageSelected(event) {
        const file = event.target.files[0];
        if (!file) return;

        const maxMB = 10;
        if (file.size > maxMB * 1024 * 1024) {
            showToast('❌ Image too large. Max ' + maxMB + 'MB allowed.');
            return;
        }

        selectedFile = file;

        // Show preview bar
        const reader = new FileReader();
        reader.onload = (e) => {
            document.getElementById('previewImg').src = e.target.result;
            document.getElementById('previewInfo').textContent = file.name + ' (' + formatBytes(file.size) + ')';
            document.getElementById('imagePreviewBar').style.display = 'flex';
        };
        reader.readAsDataURL(file);
        event.target.value = ''; // Reset input for re-selection
    }

    function clearImagePreview() {
        selectedFile = null;
        document.getElementById('previewImg').src = '';
        document.getElementById('imagePreviewBar').style.display = 'none';
    }

    function openLightbox(src) {
        document.getElementById('lightboxImg').src = src;
        document.getElementById('lightbox').classList.add('open');
    }

    function closeLightbox() {
        document.getElementById('lightbox').classList.remove('open');
    }

    // ─── Unread Badges ────────────────────────────────────────────────────────

    function incrementUnread(username) {
        unreadCounts[username] = (unreadCounts[username] || 0) + 1;
        renderOnlineUsers(getCurrentOnlineUsers());
    }

    function clearUnread(username) {
        unreadCounts[username] = 0;
    }

    // Cache last known online users list
    let lastOnlineUsers = [];
    function getCurrentOnlineUsers() { return lastOnlineUsers; }

    // Override the handler to also cache
    const _onOnlineUsersUpdate = onOnlineUsersUpdate;
    window.onOnlineUsersUpdate = function(msg) {
        lastOnlineUsers = JSON.parse(msg.body);
        renderOnlineUsers(lastOnlineUsers);
    };

    // ─── UI Utilities ─────────────────────────────────────────────────────────

    function setConnectionStatus(status) {
        const dot  = document.getElementById('statusDot');
        const text = document.getElementById('statusText');
        dot.className = 'status-dot ' + status;
        text.textContent = { connecting: 'Connecting...', connected: 'Connected', disconnected: 'Disconnected' }[status] || status;
    }

    function scrollToBottom() {
        const area = document.getElementById('messagesArea');
        area.scrollTop = area.scrollHeight;
    }

    function handleKeyDown(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    }

    function autoResize(el) {
        el.style.height = 'auto';
        el.style.height = Math.min(el.scrollHeight, 120) + 'px';
    }

    function showToast(message, duration = 4000) {
        const container = document.getElementById('toastContainer');
        const toast = document.createElement('div');
        toast.className = 'toast';
        toast.textContent = message;
        container.appendChild(toast);
        setTimeout(() => { toast.remove(); }, duration);
    }

    function formatTime(timestamp) {
        if (!timestamp) return '';
        const d = new Date(timestamp.replace(' ', 'T'));
        if (isNaN(d)) return '';
        return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    function formatBytes(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }

    function escapeHtml(str) {
        return (str || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
    }

    // Generate a consistent color per username
    function userColor(username) {
        const colors = ['#e91e63','#9c27b0','#3f51b5','#2196f3','#009688','#4caf50','#ff5722','#795548'];
        let hash = 0;
        for (let c of username) hash = c.charCodeAt(0) + ((hash << 5) - hash);
        return colors[Math.abs(hash) % colors.length];
    }

    // Set self avatar
    document.getElementById('selfAvatar').textContent = CURRENT_USER.substring(0, 2).toUpperCase();

    // ─── Bootstrap ────────────────────────────────────────────────────────────
    connectWebSocket();
</script>

</body>
</html>
