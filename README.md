# HopFog Mobile Application (Android)

> **Suggested repository description:**
> HopFog Android mobile app for offline community messaging via LANET (ESP32 SoftAP) + IEEE 802.15.4 (XBee/Zigbee) fog network.

> **Suggested repository topics:**
> `android` `kotlin` `jetpack-compose` `fog-computing` `lanet` `iot` `xbee` `zigbee` `ieee-802-15-4` `offline-messaging` `emergency-communications`

---

## Overview

**HopFog** is a fog-computing-based, infrastructure-independent community communication system designed for offline and local emergency messaging. It operates without an internet connection by leveraging a local Wi-Fi network formed by ESP32-based Fog Access Point Nodes.

This repository contains the **HopFog Mobile Application**, the primary resident-facing Android client in the HopFog system. Residents use this app to send and receive messages, view community announcements, and trigger SOS emergency alerts — all through a local area network, without relying on the public internet.

---

## Architecture Summary

```
┌─────────────────────┐        Wi-Fi SoftAP (LANET)       ┌──────────────────────────────┐
│  HopFog Mobile App  │  ─────────────────────────────►   │  ESP32 Fog Access Point Node  │
│  (this repository)  │  ◄─────────────────────────────   │  (HTTP server on hopfog.com)  │
└─────────────────────┘                                    └──────────────┬───────────────┘
                                                                          │
                                                           IEEE 802.15.4 XBee/Zigbee mesh
                                                                          │
                                                           ┌──────────────▼───────────────┐
                                                           │  Community Data Center        │
                                                           │  (Local fog-based services)   │
                                                           │  Admin web app, JSON storage  │
                                                           └──────────────────────────────┘
```

**Flow:**
1. The resident's phone connects to a nearby **ESP32 SoftAP** node over Wi-Fi, forming a **Local Ad-hoc Network (LANET)**.
2. The mobile app communicates with the ESP32 node at `http://hopfog.com` over this local connection.
3. The ESP32 node formats, secures (**AES-128 encryption**), and relays messages across the community through an **IEEE 802.15.4 multi-hop backbone** using XBee/Zigbee modules.
4. Messages ultimately reach the **Community Data Center**, a local fog-based server that hosts admin services and persists data using JSON-based file storage.

> **Security note:** Messages are encrypted at the fog layer using AES-128. No encryption keys are stored in this mobile application.

---

## Tech Stack

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

## Planned Improvements

- **Local conversation archive** using Room (SQLite) as an offline cache — partially implemented in `app/src/main/java/com/example/hopfog/data/`
- Push notification improvements for background message delivery
- UI/UX enhancements based on community feedback

---

## Credits

Capstone project under **Mapúa Malayan Colleges Laguna**.
