# HopFog Mobile Application (Android)

## Overview

**HopFog** is a fog-computing-based, infrastructure-independent community communication system designed for offline and local emergency messaging. It operates without an internet connection by leveraging a local Wi-Fi network formed by ESP32-based Fog Access Point Nodes.

This repository contains the **HopFog Mobile Application**, the primary resident-facing Android client in the HopFog system. Residents use this app to send and receive messages, view community announcements, and trigger SOS emergency alerts — all through a local area network, without relying on the public internet.

---

## Architecture Summary

```
                         ┌──────────────────────────────────────────────────────────────┐
                         │                   LOCAL AREA NETWORK (LANET)                 │
                         │                                                              │
  ┌──────────────────┐   │   ┌───────────────────────────┐                             │
  │  HopFog Mobile   │Wi-Fi  │   ESP32 Fog Access Point   │                             │
  │  App (Android)   │◄─────►│         Node(s)            │                             │
  │  (this repo)     │  SoftAP│  • Wi-Fi SoftAP gateway   │                             │
  └──────────────────┘   │   │  • HTTP server (hopfog.com)│                             │
                         │   │  • Auth, messaging, routing│                             │
                         │   └──────────────┬────────────┘                             │
                         │                  │                                           │
                         │      IEEE 802.15.4 XBee / Zigbee Multi-hop Mesh             │
                         │                  │                                           │
                         │   ┌──────────────▼────────────────────────────────────┐     │
                         │   │           Community Data Center                   │     │
                         │   │  ┌─────────────────┐   ┌───────────────────────┐ │     │
                         │   │  │  FastAPI Server  │   │   Admin Web Dashboard │ │     │
                         │   │  │  (Python)        │   │   (Jinja2 templates)  │ │     │
                         │   │  └────────┬─────────┘   └───────────────────────┘ │     │
                         │   │           │                                        │     │
                         │   │  ┌────────▼─────────┐   ┌───────────────────────┐ │     │
                         │   │  │  SQLite Database  │   │   XBee Module         │ │     │
                         │   │  │  (SQLAlchemy ORM) │   │   (Broadcast dispatch)│ │     │
                         │   │  └──────────────────┘   └───────────────────────┘ │     │
                         │   └───────────────────────────────────────────────────┘     │
                         │                                                              │
                         └──────────────────────────────────────────────────────────────┘
```

**Components:**

| Component | Role |
|-----------|------|
| **HopFog Mobile App** | Resident-facing Android client. Connects to the nearest ESP32 node over Wi-Fi and polls for messages, announcements, and SOS updates. |
| **ESP32 Fog Access Point Node(s)** | Field-deployed IoT gateways. Each creates a Wi-Fi SoftAP (SSID: `HopFog-Node-XX` or `HopFog-Network`) and runs an HTTP server at `hopfog.com`. Handles resident auth, messaging, and online tracking. Connected to the backbone via XBee. |
| **IEEE 802.15.4 XBee/Zigbee Mesh** | Multi-hop wireless backbone linking ESP32 nodes to the Community Data Center, enabling data relay across the community without internet. |
| **Community Data Center** | Central hub running a **Python FastAPI** web server backed by a **SQLite** database (via SQLAlchemy). Hosts the **admin web dashboard**, manages users, processes messages, dispatches broadcasts, monitors fog nodes, and drives XBee transmissions for community-wide announcements. |

**Communication Flows:**

1. **Resident login / messaging / SOS:** The resident's phone connects to a nearby **ESP32 SoftAP** over Wi-Fi. The mobile app sends HTTP requests to `http://hopfog.com` (the ESP32 HTTP server). The ESP32 node relays data to the **Community Data Center** over the **XBee mesh backbone**.
2. **Community broadcast / announcement:** An admin creates a broadcast on the **Admin Web Dashboard** → the **FastAPI BroadcastDispatcher** queues it → the **XBee module** transmits it wirelessly to all ESP32 nodes → the mobile app receives it via HTTP polling.
3. **SOS alert:** A resident taps SOS → the message is tagged `kind = "sos_request"` and sent through the ESP32 → the Data Center admin is notified and can escalate it to an alert or broadcast.

> **Note:** All communication stays within the local area network. No internet connection is required or used.

---

## Tech Stack

### Mobile App (this repository)

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| IDE | Android Studio |
| UI Framework | Jetpack Compose |
| HTTP Client | [Ktor](https://ktor.io/) 2.3.8 (CIO engine) |
| JSON Parsing | `kotlinx.serialization` + `org.json.JSONObject` |
| Local Database | Room 2.6.1 (SQLite, for offline message cache) |
| Navigation | Jetpack Navigation Compose 2.7.7 |
| Background Service | Android `Service` (foreground, for message polling) |
| Message Updates | HTTP polling (every 3 s in-app, every 15 s background) |
| Async | Kotlin Coroutines |

### Community Data Center (server-side)

| Component | Technology |
|-----------|-----------|
| Web Framework | Python FastAPI |
| Database | SQLite via SQLAlchemy ORM |
| Admin UI | Jinja2 HTML templates |
| Authentication | JWT (bcrypt password hashing) |
| Wireless Backbone | XBee/Zigbee (digi-xbee library, IEEE 802.15.4) |
| Broadcast Dispatch | Async broadcast dispatcher (priority queue) |

---

## Features

### Authentication
- Resident login with username and password
- "Remember Me" option persists the username across sessions
- Token-based session management (access token stored in-memory + SharedPreferences)
- Password change from the Settings screen

### Home / Announcements
- Displays community announcements fetched from the fog node
- **Priority-based sorting** (default): SOS → Alert → Other
- Also supports sorting by Newest First or Oldest First
- Manual refresh button

### Chats / Inbox
- List of all active conversations with the last message preview and timestamp
- Create a new direct message to another resident via the **New Message** screen
- Online indicator (green dot) for currently active users

### Chat / Messages
- Full chat view with message bubbles (sender left, current user right)
- Timestamps displayed below each message bubble
- Real-time message delivery via HTTP polling (every 3 seconds while the chat is open)
- Message length limited to **60 characters**; a counter appears when nearing the limit
- **Send cooldown:** 10-second cooldown between messages to prevent spam

### SOS / Emergency Messaging
- Dedicated SOS chat flow with one-tap access from the Home screen
- SOS agreement confirmation before first use
- SOS messages tagged with `kind = "sos_request"` for server-side prioritization

### Settings
- View and manage account information
- Change password
- Notifications settings
- Help, Terms of Service, and Privacy Policy pages

### Connection Status
- Live indicator in the top bar showing whether the app is connected to the HopFog fog node
- Status is checked every 10 seconds

---

## Setup / How to Run

### Requirements

| Requirement | Version |
|-------------|---------|
| Android Studio | Ladybug or newer (AGP 8.10.1) |
| Kotlin | 2.0.21 |
| minSdk | 24 (Android 7.0 Nougat) |
| targetSdk | 35 (Android 15) |
| compileSdk | 35 |

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/christian-dela-cruz/HopFogMobile.git
   ```

2. **Open in Android Studio**
   - Launch Android Studio and select **File → Open**, then choose the cloned folder.

3. **Sync Gradle**
   - Android Studio should prompt you to sync. Click **Sync Now** or go to **File → Sync Project with Gradle Files**.

4. **Connect to HopFog Wi-Fi**
   - On your Android device, connect to the HopFog node Wi-Fi (SSID starts with `HopFog`, e.g., `HopFog-Node-01` or `HopFog-Network`).

5. **Run on device**
   - Select your Android device and click **Run ▶**.

---

## Configuration Notes

### LANET Requirement
The app is designed to run on a **local intranet only**. It does not connect to the public internet. The device's Wi-Fi must be connected to a HopFog ESP32 node SSID before the app can communicate with the server.

- **Expected SSID prefix:** `HopFog` (e.g., `HopFog-Node-01`, `HopFog-Network`)
- The app checks the connected SSID at runtime in `ESP32ConnectionManager.kt`.

### Base URL / Server Address
The server base URL is defined as a constant in `app/src/main/java/com/example/hopfog/NetworkManager.kt`:

```kotlin
private const val BASE_URL = "http://hopfog.com"
```

The hostname `hopfog.com` resolves to the ESP32 fog node's IP address on the local Wi-Fi network (the node acts as a DHCP server and DNS host). This address is **not** a public internet address — it is only reachable when connected to the HopFog LANET.

> To change the server address (e.g., for development or testing), update `BASE_URL` in `NetworkManager.kt`.

### User Registration
User accounts are created and managed at the **Community Data Center** by an administrator. Self-registration is not available in the mobile app.

---

## API Endpoints Used

All requests go to `BASE_URL` (`http://hopfog.com`) over the local LANET connection.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/login` | Authenticate a resident user |
| `GET` | `/conversations` | List all conversations for the current user |
| `GET` | `/messages` | Get messages for a specific conversation |
| `POST` | `/send` | Send a message (DM or SOS) |
| `GET` | `/new-messages` | Poll for new messages since a given message ID |
| `GET` | `/users` | List all users (for New Message screen) |
| `POST` | `/create-chat` | Find or create a direct message conversation |
| `POST` | `/sos` | Find or create the SOS emergency conversation |
| `POST` | `/agree-sos` | Record the user's SOS agreement |
| `POST` | `/change-password` | Change the logged-in user's password |
| `GET` | `/announcements` | Fetch community announcements |
| `GET` | `/status` | Check if the fog node is reachable |
| `GET` | `/api/conversation/{otherUserId}` | Get full conversation history with a user |

---

## Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/66e56f3b-e8cd-41c8-80b3-0ab78fffd113" width="22%" />
  <img src="https://github.com/user-attachments/assets/dc5230a6-ad0a-4dfd-856a-6875b0b9dc35" width="22%" />
  <img src="https://github.com/user-attachments/assets/84142b14-dc1e-42d5-9259-1d3a26ac1e3f" width="22%" />
  <img src="https://github.com/user-attachments/assets/d7ce698c-f9dd-4a32-8de6-23b19b2c4fea" width="22%" />
</p>

---

## Credits

Capstone project under **Mapúa Malayan Colleges Laguna**.
