# 1.2.0

### New Event Types & Commands

- Train derailment notifications (non-crash derailments from stress or migration failures)
- Train lifecycle notifications (assembled/disassembled)
- `/signalbox test` command to verify webhook connectivity
- Per-event webhook URL overrides — send different events to different channels
- Colour-coded Discord embeds: red for crashes, yellow for derailments, green for creation, red for removal

### Config Changes

- Config is now organized into `[webhook]` and `[events.*]` TOML sections
- Each event type has its own section with independent enabled, cooldown, and webhook URL settings
- Shared webhook sender extracted for all event types
- Per-train crash notification cooldown (`cooldownSeconds`, default 60s)
