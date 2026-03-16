# Create: Signalbox – Minecraft Webhook Notifications

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-5E7C16?logo=minecraft&logoColor=white)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.219-orange)
![Create](https://img.shields.io/badge/Create-Optional-blue)
[![Wiki](https://img.shields.io/badge/Wiki-GitHub-lightgrey?logo=github)](https://github.com/saunhardy/create-signalbox/wiki)

**Create: Signalbox** is a NeoForge mod that sends configurable webhook notifications for in-game events from the [Create](https://github.com/Creators-of-Create/Create) mod. When a train crashes, the mod automatically sends a detailed report to a Discord webhook or any custom API endpoint — no backend server required.

---

## Features

### Train Crash Notifications

- **Automatic detection:** Uses a Mixin to hook into Create's `Train.crash()` method. Whenever a train derails, a report is fired off asynchronously.
- **Rich crash data:** Reports include train name, speed, carriage count, position, dimension, owner, driver, all passengers, and backwards driver info.
- **Conditional loading:** The mixin is only applied when the Create mod is present. Without Create, the mod loads cleanly with no errors.

### Discord Webhook Support

- **Embedded reports:** Crash data is formatted as a Discord embed with colour-coded fields, inline layout, and ISO timestamps.
- **Server name footer:** Optionally include your server's name in the embed footer to distinguish between multiple servers posting to the same channel.

### Custom API Support

- **Raw JSON mode:** Disable Discord formatting to send a flat JSON payload to any HTTP endpoint.
- **No authentication:** Requests are plain `POST` with `Content-Type: application/json` — no tokens, no verification, just data.

---

## ⚙ Requirements

### Minecraft

- Minecraft version **1.21.1**
- Requires **NeoForge** mod loader (21.1.219+)

### Optional

- **Create** 6.0.0+ — required for train crash notifications

> Without Create installed, the mod will load but no crash notifications will be sent.

---

## Configuration

Upon first launch, the mod generates a config file at:
`/serverconfig/createsignalbox-common.toml`

### Webhook Settings (`[webhook]`)

| Option | Default | Description |
|---|---|---|
| `webhookUrl` | `""` | Discord webhook URL or custom API endpoint |
| `useDiscordFormat` | `true` | `true` = Discord embed, `false` = raw JSON |
| `timeoutMs` | `5000` | HTTP request timeout (1000–30000 ms) |
| `serverName` | `""` | Optional server name shown in notifications |

### Event Settings (`[events]`)

| Option | Default | Description |
|---|---|---|
| `events.trainCrash.enabled` | `true` | Toggle train crash notifications |
| `events.trainCrash.cooldownSeconds` | `60` | Cooldown before the same train can trigger another notification (0 to disable) |

All settings are also accessible in-game via the mod config screen (Mods → Create: Signalbox → Config).

---

## Payload Formats

### Discord Embed (default)

When `useDiscordFormat` is enabled, crash reports are sent as a red Discord embed with the following fields:

| Field | Description |
|---|---|
| Train | Train name |
| Speed | Speed at time of crash (m/t) |
| Carriages | Number of carriages |
| Dimension | World dimension |
| Position | X, Y, Z coordinates |
| Owner | Train owner (resolved name, falls back to UUID) |
| Driver | Player driving the train |
| Passengers | Other players aboard |
| Backwards Driver | Player driving in reverse |

### Raw JSON

When `useDiscordFormat` is disabled, the payload is a flat JSON object:

```json
{
  "event": "train_crash",
  "trainId": "uuid",
  "trainName": "My Train",
  "speed": 1.25,
  "carriageCount": 3,
  "timestamp": 1710000000000,
  "position": { "x": 100, "y": 64, "z": -200 },
  "dimension": "minecraft:overworld",
  "owner": "uuid",
  "driverUuid": "uuid",
  "passengers": [
    { "uuid": "uuid", "name": "Player1", "isDriver": true }
  ],
  "backwardsDriver": { "uuid": "uuid", "name": "Player2" },
  "serverName": "My Server"
}
```

---

## Development

- Built with **NeoForge** for Minecraft 1.21.1.
- Developed using Java & Gradle.
- Uses Mixin for non-invasive Create integration.

### Build Instructions

1. Clone the repo.
2. Open in an IDE (e.g., IntelliJ).
3. Use JDK 21+.
4. Run `gradlew build` to compile.
