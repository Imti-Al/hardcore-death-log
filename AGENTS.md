# Hardcore Death Log

## Inspect before editing
- Read the source, build scripts, fabric.mod.json and existing tests first. Verify behavior against the current implementation.
- Preserve the declared Minecraft, Fabric Loader, Fabric API, mappings, Loom, Gradle and Java versions unless the task requests an upgrade.

## Required behavior
- Record only the first death for each world/run, across all players. Do not change this to one death per player. Duplicate callbacks, reconnects and server restarts must not produce additional entries for that world.
- Inspect and preserve the current world/run identification mechanism. A new world must be distinguishable from a restart of an existing world; do not invent a new identity scheme without explaining migration effects.
- Preserve cumulative hardcore-deaths.json history across world resets, server restarts and mod JAR updates. Never clear, replace with sample data, silently discard malformed records or regenerate the live file.
- Preserve existing imported records and JSON compatibility. Any schema/path migration requires an explicit plan, a backup and tests on disposable copies.
- The server owns death detection, world identity, deduplication, persistence and the data sent to players. Clients display server-supplied data; client requests must not create or modify authoritative death records.
- /deaths opens the death-log GUI using server data. Preserve the command, permissions, network payload compatibility and correct client-thread screen updates. Do not restore the removed K keybind without a request.
- Common/server code must remain safe on a dedicated server: no client-only class loading.

## Validation
- Use the checked-in Gradle wrapper: .\gradlew.bat build on Windows. Determine the required JDK from the project before choosing JAVA_HOME.
- Run relevant existing checks and add focused regression coverage when changing persistence, deduplication, world identity or networking.
- For behavior changes, test only disposable local worlds/server data: first death recorded once, later deaths ignored, restart retains deduplication, a new world adds one record while preserving history, and /deaths displays server data on the client.
- Check dedicated-server startup when changing common/server initialization. Clearly distinguish a successful build from in-game validation; report anything untested.
- Report the actual installable JAR path in build/libs; do not select a sources JAR.

## Git and review
- Keep the GitHub repository private. Never change visibility or grant collaborators access without explicit approval.
- Inspect Git status and preserve unrelated changes. Work on a focused branch; stage an explicit file allowlist after inspecting the diff and checking for secrets/runtime data.
- Preserve the existing Git identity. Use concise developer-style commit messages without attribution trailers. Do not rewrite history, force-push or move tags.
- Local edits and validation may proceed within the task. Before pushing or opening a PR, present the concrete diff summary, validation results, destination and branch, then obtain explicit approval. Do not treat silence as approval.
- Merge only with separate explicit approval. Do not enable automatic merging.
- Never commit credentials, AMP connection information, world files, player/server data or the real cumulative death log. Use synthetic fixtures with names distinct from hardcore-deaths.json.

## Deployment boundary
- Never automatically deploy to, connect to, restart or modify the live AMP server. Do not copy a built JAR into live client/server folders, modify live JSON or add deployment hooks/actions.
- Builds and approved GitHub pushes/PRs do not authorize deployment. Any future live deployment requires a separate explicit user request.
