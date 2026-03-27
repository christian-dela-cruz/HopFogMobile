# Admin-Side (ESP32 Firmware) Changes

This file contains **GitHub Copilot Agent-friendly** instructions for changes
that must be made to the HopFog ESP32 firmware repository
([hyoono/HopFog-Web](https://github.com/hyoono/HopFog-Web),
branch `copilot/convert-repo-for-esp32-deployment`).

> **Note:** Despite the "Web" name, `HopFog-Web` is the ESP32 firmware project.
> It runs on an ESP32-CAM, hosts a WiFi access point, and serves a REST API
> that the mobile app connects to. The source files are under `src/` (C++/PlatformIO).

---

## Change 1 — Filter admin users out of the `/users` mobile endpoint

### Background

The mobile app calls `GET /users?user_id=X` to populate the New Message screen.
The ESP32 currently returns **all** active users including admins. The mobile app
filters them out client-side (`role == "mobile"`). It is cleaner and more secure
to filter them server-side so admin user records are never sent to mobile clients.

### File to change

`src/api_handlers.cpp`

### What to change

Locate the handler registered for `GET /users` (search for the comment block
`MOBILE: GET /users?user_id=X`). It is the handler that builds a JSON array of
users for the New Messages picker.

Inside the loop that iterates over each user object (`for (JsonObject u :
doc.as<JsonArray>())`), add a role check **immediately after** the existing
`!isActive` skip — so that any user whose role is not `"mobile"` is also
excluded from the response.

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

**After (add the role check shown with `// NEW` comment):**

```cpp
for (JsonObject u : doc.as<JsonArray>()) {
    int uid = u["id"] | 0;
    if (uid == requestingUserId) continue;  // exclude self
    int isActive = u["is_active"] | 0;
    if (!isActive) continue;  // exclude deactivated users
    String role = u["role"] | "";
    if (role != "mobile") continue;  // NEW: only expose mobile users to mobile clients

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

- Prevents admin account details (id, username) from being sent to mobile clients.
- Keeps the server as the source of truth for role enforcement rather than relying
  solely on client-side filtering.
- The mobile app already filters `role == "mobile"` as a defence-in-depth measure;
  this change makes the server enforce the same rule.

### How to verify

After flashing the updated firmware:
1. Log in to the mobile app as a mobile user.
2. Open **New Message** and switch to **All Users**.
3. Confirm that admin usernames do **not** appear in the list.
4. Log in to the web admin dashboard and confirm the admin account still works
   normally (the `/users` route change only affects `GET /users`, not
   `/api/users` or the admin dashboard routes).

---

## No other firmware changes are required

All other fixes (online-only default, null-safe JSON config, mobile-role
inclusion filter) have been applied to the mobile app directly and do not
require firmware changes.
