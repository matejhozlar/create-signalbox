# 2.0.0

### Breaking Changes

- Renamed mod from **Create: Signalbox** to **Create: Webhooks**
- Mod ID changed from `createsignalbox` to `createwebhooks`
- Command renamed from `/signalbox` to `/webhooks`
- Config file path is now `serverconfig/createwebhooks-common.toml` — existing config will not migrate automatically; copy values from the old file or reconfigure

# 1.2.1

### Bug Fixes

- Fixed crash and derail reports sometimes missing position and dimension data
- Position now falls back to track graph data when no carriage entity is loaded
- Server resolution for owner name lookup now tries all carriages instead of only the first
