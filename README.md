# 💬 RealTime Chat Application

> Production-grade Spring Boot WebSocket Chat with MySQL — Group chat, private DMs, image sharing, and real-time presence.

---

## 🏗️ Architecture Overview

```
realtime-chat/
├── src/main/java/com/chatapp/
│   ├── ChatApplication.java               ← Spring Boot entry point
│   ├── config/
│   │   ├── WebSocketConfig.java           ← STOMP broker + SockJS setup
│   │   ├── SecurityConfig.java            ← Spring Security configuration
│   │   └── AppConfig.java                 ← CORS, resource handlers, upload dir
│   ├── controller/
│   │   ├── ChatWebSocketController.java   ← @MessageMapping handlers (WS)
│   │   ├── ChatRestController.java        ← REST API (history, upload, users)
│   │   └── ViewController.java            ← JSP page routing
│   ├── service/
│   │   ├── MessageService.java            ← Chat message business logic
│   │   ├── UserService.java               ← User presence management
│   │   └── ImageService.java              ← File upload + validation
│   ├── repository/
│   │   ├── MessageRepository.java         ← JPA queries for messages
│   │   └── UserRepository.java            ← JPA queries for users
│   ├── entity/
│   │   ├── Message.java                   ← Message JPA entity
│   │   └── User.java                      ← User JPA entity
│   ├── dto/
│   │   └── ChatDTOs.java                  ← All request/response DTOs
│   └── exception/
│       ├── ChatExceptions.java            ← Custom exception hierarchy
│       └── GlobalExceptionHandler.java    ← @ControllerAdvice handler
├── src/main/resources/
│   ├── application.properties             ← App + DB + WS configuration
│   └── logback-spring.xml                 ← Structured logging config
├── src/main/webapp/WEB-INF/views/
│   ├── login.jsp                          ← User login page
│   └── chat.jsp                           ← Main chat UI (SockJS + STOMP)
├── schema.sql                             ← MySQL schema + seed data
└── pom.xml                                ← Maven dependencies
```

---

## 🔌 WebSocket Messaging Architecture

| Direction | Destination | Purpose |
|---|---|---|
| Client → Server | `/app/chat.sendGroupMessage` | Send group message |
| Client → Server | `/app/chat.sendPrivateMessage` | Send private message |
| Client → Server | `/app/chat.join` | Announce user joined |
| Server → All | `/topic/group-chat` | Broadcast group messages |
| Server → All | `/topic/presence` | User join/leave events |
| Server → All | `/topic/online-users` | Updated online user list |
| Server → User | `/user/queue/private` | Private message delivery |
| Server → User | `/user/queue/private-history` | Private chat history |

---

## 🚀 Local Setup Instructions

### Prerequisites

| Requirement | Version |
|---|---|
| Java | 17+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Browser | Chrome/Firefox/Edge (modern) |

---

### Step 1: Set Up MySQL

```bash
# Log into MySQL as root
mysql -u root -p

# Run the schema script
source /path/to/your/project/schema.sql
```

Or manually:
```sql
CREATE DATABASE chatdb CHARACTER SET utf8mb4;
CREATE USER 'chatuser'@'localhost' IDENTIFIED BY 'chatpassword';
GRANT ALL PRIVILEGES ON chatdb.* TO 'chatuser'@'localhost';
FLUSH PRIVILEGES;
USE chatdb;
-- Then run the CREATE TABLE statements from schema.sql
```

---

### Step 2: Configure application.properties

Edit `src/main/resources/application.properties`:

```properties
# Update these if your MySQL setup is different
spring.datasource.url=jdbc:mysql://localhost:3306/chatdb?useSSL=false&serverTimezone=UTC
spring.datasource.username=chatuser
spring.datasource.password=chatpassword

# Upload directory (absolute path recommended for production)
app.upload.dir=uploads/images
```

---

### Step 3: Build and Run

```bash
# Navigate to project root
cd realtime-chat

# Build (skip tests for first run)
mvn clean package -DskipTests

# Run the application
mvn spring-boot:run
```

Or run the JAR directly:
```bash
java -jar target/realtime-chat-1.0.0.war
```

---

### Step 4: Access the Application

Open your browser:
- **Login Page:** http://localhost:8080
- **Direct Chat:** http://localhost:8080/chat?username=yourname
- **API Status:** http://localhost:8080/api/status
- **Health Check:** http://localhost:8080/actuator/health

---

## 🌐 Expose to Internet with ngrok

ngrok creates a public tunnel to your local server — perfect for sharing with friends.

### Install ngrok

```bash
# macOS
brew install ngrok

# Linux
snap install ngrok

# Windows: Download from https://ngrok.com/download
```

### Start ngrok Tunnel

```bash
# Make sure your Spring Boot app is running on 8080, then:
ngrok http 8080
```

You'll see output like:
```
Forwarding    https://abc123.ngrok-free.app -> http://localhost:8080
```

### Share the Link

Send `https://abc123.ngrok-free.app` to your friends. They can join from anywhere!

> **Note:** Free ngrok accounts get a random URL each session. For a stable URL, sign up for a free ngrok account and use their reserved domains.

### Important: CORS for ngrok

When using ngrok, the app is already configured with `allowedOriginPatterns("*")` so cross-origin WebSocket connections will work. In production, restrict this to your actual domain.

---

## 👥 Testing with Multiple Users

### Local Testing (Multiple Browser Tabs)

1. Open `http://localhost:8080` in Tab 1 → login as `alice`
2. Open `http://localhost:8080` in Tab 2 → login as `bob`  
3. Open `http://localhost:8080` in **Incognito** → login as `charlie`

### Remote Testing (Multiple Devices via ngrok)

1. Start ngrok: `ngrok http 8080`
2. Share the HTTPS URL with friends
3. Each person opens the URL and enters a unique username
4. They can see each other in the online users sidebar
5. Group chat is broadcast to everyone, private chat is one-to-one

### Concurrent User Handling

The application handles concurrent users through:
- **WebSocket sessions:** Each user has an independent WebSocket session
- **STOMP subscriptions:** Group messages use pub/sub via `/topic/group-chat`
- **Private routing:** Spring's `SimpMessagingTemplate.convertAndSendToUser()` resolves per-user queues
- **Session tracking:** User presence is tracked via session ID in MySQL
- **HikariCP pool:** Database connection pooling (20 max connections configured)

---

## 📡 REST API Reference

### Users

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/users/join` | Register/login user |
| GET | `/api/users/online` | List online users |
| GET | `/api/users/{username}/exists` | Check username availability |

### Messages

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/messages/group?limit=50` | Group chat history |
| GET | `/api/messages/private/{u1}/{u2}` | Private conversation |
| POST | `/api/messages/read` | Mark messages as read |

### Images

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/images/upload` | Upload image file |

### System

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/status` | App status + user count |
| GET | `/actuator/health` | Health check |

---

## 🔒 Security Notes

**Current Setup (Demo):**
- No authentication required (username-only)
- CSRF disabled
- All origins allowed

**Production Hardening:**
1. Enable JWT authentication in `SecurityConfig`
2. Add WebSocket channel interceptor for auth token validation
3. Restrict `allowedOriginPatterns` to your domain
4. Enable HTTPS (required for WebSocket over WSS)
5. Add rate limiting on REST endpoints
6. Restrict `/actuator` endpoints

---

## 📋 Logging

Log files are written to `./logs/`:

| File | Contents |
|---|---|
| `chat-application.log` | All application logs |
| `chat-application-error.log` | ERROR level only |
| `chat-websocket.log` | WebSocket controller logs |

Set log levels in `application.properties`:
```properties
logging.level.com.chatapp=DEBUG    # Your code
logging.level.org.hibernate.SQL=DEBUG  # SQL queries (disable in prod)
```

---

## 🛠️ Development Tips

### Hot Reload
Spring DevTools is included. Files reload without restart:
```bash
mvn spring-boot:run
# Save a .java file → auto-recompiles
```

### View Database
```bash
mysql -u chatuser -p chatdb
SELECT * FROM users;
SELECT content, sender_id, is_group_message FROM messages ORDER BY created_at DESC LIMIT 10;
```

### Reset All Data
```sql
DELETE FROM messages;
DELETE FROM users;
```

### Production Build
```bash
mvn clean package -Pprod
java -jar target/realtime-chat-1.0.0.war --spring.profiles.active=prod
```

---

## 🧪 Quick Functional Tests

```bash
# 1. Check health
curl http://localhost:8080/actuator/health

# 2. Register a user
curl -X POST http://localhost:8080/api/users/join \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","displayName":"Test User"}'

# 3. Get online users
curl http://localhost:8080/api/users/online

# 4. Get group chat history
curl http://localhost:8080/api/messages/group?limit=10
```

---

## 🤝 Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2, Spring WebMVC |
| WebSocket | Spring WebSocket, STOMP |
| Database | MySQL 8.0, Spring Data JPA, Hibernate |
| Connection Pool | HikariCP |
| Frontend | JSP, SockJS, STOMP.js |
| Security | Spring Security (minimal) |
| Logging | SLF4J + Logback |
| Build | Maven |

---

*Built with ❤️ as a production-grade chat starter. Extend it with your own features!*
#   s p r i n g - b o o t - c h a t - a p p - u s i n g - w e b s o c k e t  
 