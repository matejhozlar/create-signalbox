# Changelog

## 1.0.0

### Initial Release

- Train crash notifications via Mixin hook into Create's `Train.crash()`
- Discord webhook support with rich embeds (train name, speed, position, driver, passengers, etc.)
- Custom API support with raw JSON payloads
- Configurable webhook URL, timeout, server name, and Discord/raw format toggle
- UUID-to-name resolution via profile cache with UUID fallback
- Conditional mixin loading — only applies when Create is present
- In-game config screen
