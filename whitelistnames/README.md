# Whitelist Names (Fabric, Minecraft 26.2)

Server-side only. Players don't need to install anything.

Whitelisted players get a custom display name that shows in chat, join/leave/death
messages, the tab list and above their head.

## Commands
- `/whitelist add <username> <name>` - whitelists them and sets their name. Same permission as vanilla `/whitelist`.
- `/whitelist add <username>` - normal vanilla behavior
- `/changename <username or current name> <new name>` - put a current name with spaces in "quotes".
  Same permission as `/whitelist` (or `/gamemode` in singleplayer/LAN).

Names are trimmed, `§` and control characters are stripped, and they can be at most 32 characters.
They're saved in `config/whitelistnames.json`.

## Known limitations
- Nicknamed players are put on the scoreboard team `wn_nicknamed` (to hide the vanilla nametag),
  so this conflicts with other team usage.
- Players see their own floating name in third person.
- The floating name doesn't dim when sneaking like the vanilla nametag does.

## Building
Needs JDK 25. Run `./gradlew build`; the mod is `build/libs/whitelistnames-1.0.0.jar`
(not the `-sources` one). Put it plus Fabric API in the server's `mods` folder.

GitHub Actions (`.github/workflows/build.yml`) builds on every push and uploads the jar as an artifact.
