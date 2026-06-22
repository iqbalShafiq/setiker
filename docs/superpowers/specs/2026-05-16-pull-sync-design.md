# Pull Sync Design

## Goal

Implement cloud-to-device pull sync for sticker packs and stickers using the existing `GET /api/v1/sync` API, while preserving local offline edits and keeping packs visible even when they are not yet WhatsApp-valid.

## API Contract

The app will use `GET /api/v1/sync` with an optional `lastSyncAt` query parameter.

The API returns an envelope whose `data` contains:

- `stickerPacks.created`, `stickerPacks.updated`, `stickerPacks.deleted`
- `stickers.created`, `stickers.updated`, `stickers.deleted`
- `syncToken`, currently base64-encoded server timestamp

Sticker packs can include `stickers`, where each relation includes a `sticker` with fields such as `id`, `name`, `filename`, `url`, `width`, `height`, `mimeType`, and timestamps.

## Sync Order

Manual sync and background sync will run in this order:

1. Push pending local operations first.
2. Pull remote changes from `GET /api/v1/sync`.
3. Persist the server timestamp from `syncToken` for the next incremental pull.

This order prevents stale remote data from overwriting edits made offline on the device.

## Conflict Rule

Pending local changes win over pull sync.

If a local pack or sticker has a pending/failed sync operation, pull sync will not overwrite that local item. The app will push local changes first. After the push succeeds, a later pull can apply the resulting server state.

This design does not implement per-field merge. If multiple devices edit the same cloud object, the last successful server update wins after all pending local operations are pushed.

## Import Rules

Remote packs will be imported even if they contain fewer than 3 stickers. These packs may not be valid for WhatsApp export yet, but they should still be visible and editable in the app.

For each remote created or updated pack:

- Find the local pack by `cloudId`.
- If not found, create a new local pack with a new local identifier.
- Set `cloudId`, `cloudOwnerId`, `visibility`, `syncState = SYNCED`, and `lastSyncAt`.
- Use the remote owner display name or username as `publisher`, falling back to `Setiker`.
- Download each remote sticker image from `sticker.url` into `StickerFileStorage`.
- Save successfully downloaded stickers locally with their cloud ids.
- Use the first successfully downloaded sticker image as `trayImageFile`.
- If no sticker image is available, use a safe local placeholder tray image strategy rather than crashing.

For each remote deleted pack:

- Find the local pack by `cloudId`.
- If there is no pending local operation for it, delete the local pack, local sticker rows, and local files.

For remote sticker deletions:

- Remove matching local stickers by `cloudId` when no pending local operation protects them.

## Error Handling

Pull sync should be partial-success tolerant.

If a sticker image download fails, the pack can still be imported with the stickers that downloaded successfully. The sync report should include failure counts or messages so the sync screen can communicate that cloud sync completed with warnings.

Network/API failures should make WorkManager retry. Authentication or offline skips should not create endless retries.

## UX Improvements

The sync UI should explain the current stage and result more clearly:

- `Syncing local changes`
- `Downloading cloud changes`
- `Up to date`
- `Waiting for internet`
- `Sign in to sync`

The manual sync result should show useful counts such as uploaded operations, downloaded packs/stickers, and failures. Packs imported with fewer than 3 stickers should appear normally, not as an error state.

The existing pending badge should continue to represent local changes waiting to be pushed.

## Testing

Add regression coverage for:

- Remote pack with fewer than 3 stickers is imported.
- Remote pack maps to local pack by `cloudId`.
- Pending local operation prevents remote overwrite.
- Remote delete removes local pack by `cloudId`.
- `lastSyncAt` is sent as an ISO timestamp, not a raw epoch number.
- `syncToken` is decoded and persisted as the next pull timestamp.

## Scope Boundaries

This implementation will not add automatic per-field conflict merging. It will not change the API schema unless the current response proves insufficient during implementation. If the API response lacks data needed for safe import, the app will handle the gap defensively and document the remaining server-side contract issue.
