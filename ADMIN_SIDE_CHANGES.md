# Admin-Side (ESP32 Firmware) Changes

This file contains **GitHub Copilot Agent-friendly** instructions for changes
that must be made to the HopFog ESP32 firmware repository
([hyoono/HopFog-Web](https://github.com/hyoono/HopFog-Web),
branch `copilot/convert-repo-for-esp32-deployment`).

> **Note:** Despite the "Web" name, `HopFog-Web` is the ESP32 firmware project.
> It runs on an ESP32-CAM, hosts a WiFi access point, and serves a REST API
> that the mobile app connects to. The source files are under `src/` (C++/PlatformIO).

---

## Change 1 — Add `is_admin` to the `/users` mobile endpoint response

### Background

The mobile app calls `GET /users?user_id=X` to populate the New Message screen.
The mobile app must know which users are admins so it can hide them from the
New Messages picker (users contact admins via SOS only, not via DM).

The server must include an **`is_admin`** boolean field for each user in the
response. The mobile app uses `!user.isAdmin` to filter the list — this is
more robust than relying on role strings, since the server is the authoritative
source of whether an account is an admin.

### File to change

`src/api_handlers.cpp`

### What to change

Locate the handler registered for `GET /users` (search for the comment block
`MOBILE: GET /users?user_id=X`). It is the handler that builds a JSON array of
users for the New Messages picker.

Inside the loop that iterates over each user object, add an `is_admin` field to
each output object so the mobile app can filter admin accounts client-side.

**Before (roughly lines 1370–1384 of `src/api_handlers.cpp`):**

```cpp
for (JsonObject u : doc.as<JsonArray>()) {
    int uid = u["id"] | 0;
    if (uid == requestingUserId) continue;  // exclude self
    int isActive = u["is_active"] | 0;
    if (!isActive) continue;  // exclude deactivated users

    bool online = false;
    for (int i = 0; i < onlineCount; i++) {
        if (onlineIds[i] == uid) { online = true; break; }
    }

    JsonObject o = arr.add<JsonObject>();
    o["id"]        = uid;
    o["username"]  = u["username"];
    o["role"]      = u["role"];
    o["is_online"] = online;
}
```

**After (add `is_admin` to the output object, shown with `// NEW` comment):**

```cpp
for (JsonObject u : doc.as<JsonArray>()) {
    int uid = u["id"] | 0;
    if (uid == requestingUserId) continue;  // exclude self
    int isActive = u["is_active"] | 0;
    if (!isActive) continue;  // exclude deactivated users

    bool online = false;
    for (int i = 0; i < onlineCount; i++) {
        if (onlineIds[i] == uid) { online = true; break; }
    }

    String role = u["role"] | "";
    bool isAdmin = (role == "admin");  // NEW

    JsonObject o = arr.add<JsonObject>();
    o["id"]        = uid;
    o["username"]  = u["username"];
    o["role"]      = u["role"];
    o["is_online"] = online;
    o["is_admin"]  = isAdmin;          // NEW
}
```

### Why

- The mobile app's New Message screen filters by `!user.isAdmin` to hide admin
  accounts. Without `is_admin` in the response the field defaults to `false`,
  meaning **all** users (including admins) would appear in the picker.
- Using an explicit boolean field is more robust than relying on the client
  comparing role strings, because the server is the authoritative source of the
  admin/non-admin decision.
- Admin accounts remain visible in the Chats list only if an existing SOS
  conversation exists; the `is_admin` field on `/conversations` (see Change 2)
  handles that separation.

### How to verify

After flashing the updated firmware:
1. Log in to the mobile app as a mobile user.
2. Open **New Message** and switch to **All Users**.
3. Confirm that admin usernames do **not** appear in the list.
4. Log in to the web admin dashboard and confirm the admin account still works
   normally (this change only affects `GET /users`, not admin dashboard routes).

---

## Change 2 — Add `other_user_id` and `is_admin` to the `/conversations` response

### Background

The mobile app calls `GET /conversations?user_id=X` to populate the Chats
screen. The app needs two extra fields per conversation:

- **`other_user_id`** — the ID of the other participant, used to open the
  message thread with the correct Room DB cache key.
- **`is_admin`** — whether the other participant is an admin; admin conversations
  are hidden from the Chats list (users use SOS for admin contact).

### File to change

`src/api_handlers.cpp`

### What to change

Locate the handler registered for `GET /conversations`
(search for `MOBILE: GET /conversations?user_id=X`).

Add `other_user_id` and `is_admin` to each conversation object in the response.

**Before:**

```cpp
JsonObject c = arr.add<JsonObject>();
c["conversation_id"] = conv["conversation_id"];
c["contact_name"]    = conv["contact_name"];
c["last_message"]    = conv["last_message"];
c["timestamp"]       = conv["timestamp"];
```

**After:**

```cpp
int otherUserId = conv["other_user_id"] | 0;  // NEW: resolve from DB join
String otherRole = conv["other_user_role"] | "";  // NEW: resolve from DB join
bool isAdmin = (otherRole == "admin");             // NEW

JsonObject c = arr.add<JsonObject>();
c["conversation_id"] = conv["conversation_id"];
c["contact_name"]    = conv["contact_name"];
c["last_message"]    = conv["last_message"];
c["timestamp"]       = conv["timestamp"];
c["other_user_id"]   = otherUserId;   // NEW
c["is_admin"]        = isAdmin;       // NEW
```

> **Note:** `other_user_id` and `other_user_role` must be included in the SQL
> query that fetches conversations. Ensure the query joins the `users` table on
> the other participant's ID to retrieve both fields.

### How to verify

After flashing:
1. Log in to the mobile app as a mobile user.
2. Open **Chats**. Confirm that any conversation with an admin is **not** shown
   in the list (users contact admins via SOS only).
3. Tap a DM conversation. Confirm it opens correctly and messages load from the
   Room DB cache.

---

## Summary of mobile app changes that drove these firmware requirements

| Mobile behaviour | Firmware field required |
|---|---|
| New Message hides admin accounts | `is_admin` on `/users` response |
| Chats list hides admin conversations | `is_admin` on `/conversations` response |
| Room DB cache key for DM threads | `other_user_id` on `/conversations` response |
