# Admin-Side (ESP32 Firmware) Changes

This file contains **GitHub Copilot Agent-friendly** instructions for changes
that must be made to the HopFog ESP32 firmware repository
([hyoono/HopFog-Web](https://github.com/hyoono/HopFog-Web),
branch `copilot/convert-repo-for-esp32-deployment`).

> **Note:** Despite the "Web" name, `HopFog-Web` is the ESP32 firmware project.
> It runs on an ESP32-CAM, hosts a WiFi access point, and serves a REST API
> that the mobile app connects to. The source files are under `src/` (C++/PlatformIO).

---

## Change 1 — Exclude admin users from the `/users` mobile endpoint

### Background

The mobile app calls `GET /users?user_id=X` to populate the New Message screen.
Admin accounts must **never** appear in the New Messages picker — users contact
admins via SOS only. The server is the authoritative place to enforce this rule;
no `is_admin` field is needed in the response.

### File to change

`src/api_handlers.cpp`

### What to change

Locate the handler registered for `GET /users` (search for `MOBILE: GET /users?user_id=X`).
Inside the loop that builds the response array, add a role check to skip admin
accounts entirely.

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

**After (add role check shown with `// NEW` comment):**

```cpp
for (JsonObject u : doc.as<JsonArray>()) {
    int uid = u["id"] | 0;
    if (uid == requestingUserId) continue;  // exclude self
    int isActive = u["is_active"] | 0;
    if (!isActive) continue;  // exclude deactivated users
    String role = u["role"] | "";
    if (role == "admin") continue;  // NEW: never send admin accounts to mobile clients

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

### Why

- Admin accounts must not be DM-able from the mobile app — contact is via SOS only.
- Filtering server-side is the single authoritative rule; the mobile app does not
  need any client-side admin check and does not receive an `is_admin` field.

### How to verify

After flashing the updated firmware:
1. Log in to the mobile app as a mobile user.
2. Open **New Message** and switch to **All Users**.
3. Confirm that admin usernames do **not** appear in the list.
4. Log in to the web admin dashboard and confirm admin accounts still work
   normally (this change only affects `GET /users`, not admin dashboard routes).

---

## Change 2 — Exclude admin conversations from `/conversations`; add `other_user_id`

### Background

The mobile app calls `GET /conversations?user_id=X` to populate the Chats screen.
Two things are required:

1. **Exclude admin conversations** — conversations with admin accounts must not
   appear in the Chats list (users contact admins via SOS only). Filter them
   server-side; no `is_admin` field is needed.
2. **Include `other_user_id`** — the mobile app needs the ID of the other
   participant so it can open the message thread with the correct Room DB cache key.

### File to change

`src/api_handlers.cpp`

### What to change

Locate the handler registered for `GET /conversations`
(search for `MOBILE: GET /conversations?user_id=X`).

Add a skip for admin conversations and add `other_user_id` to each object.

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
// NEW: skip conversations where the other participant is an admin
String otherRole = conv["other_user_role"] | "";  // requires DB join — see note below
if (otherRole == "admin") continue;

int otherUserId = conv["other_user_id"] | 0;  // NEW: requires DB join

JsonObject c = arr.add<JsonObject>();
c["conversation_id"] = conv["conversation_id"];
c["contact_name"]    = conv["contact_name"];
c["last_message"]    = conv["last_message"];
c["timestamp"]       = conv["timestamp"];
c["other_user_id"]   = otherUserId;  // NEW
```

> **Note:** `other_user_id` and `other_user_role` must be included in the SQL
> query that fetches conversations. Ensure the query joins the `users` table on
> the other participant's ID to retrieve both fields.

### How to verify

After flashing:
1. Log in to the mobile app as a mobile user.
2. Open **Chats**. Confirm that any conversation with an admin is **not** shown.
3. Tap a DM conversation. Confirm it opens correctly and messages load from the
   Room DB cache.

---

## Summary of mobile app changes that drove these firmware requirements

| Mobile behaviour | Firmware change required |
|---|---|
| New Message shows only non-admin users | Skip admin accounts in `GET /users` response |
| Chats shows only non-admin conversations | Skip admin conversations in `GET /conversations` response |
| Room DB cache key for DM threads | `other_user_id` field in `GET /conversations` response |
