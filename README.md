# Whitelist Names (Fabric, Minecraft 26.2)

Server-side only. Players don't need to install anything.

Whitelisted players get a custom display name that shows in chat, join/leave/death
messages, the tab list and above their head.

## Commands
- `/whitelist add <username> <name>` - whitelists them and sets their name. Same permission as vanilla `/whitelist`.
- `/whitelist add <username>` - normal vanilla behavior
- `/changename <username or current name> <new name>` - put a current name with spaces in "quotes".
  Same permission as `/whitelist` (or `/gamemode` in singleplayer/LAN).
- `/eyerecipe disable` / `/eyerecipe enable` - turns the Eye of Ender crafting recipe off or on (ops only).
  `/eyerecipe` on its own shows whether it's on. Applies to crafting tables, the inventory grid and crafters,
  and is saved in `config/whitelistnames-settings.json` so it stays that way after a restart.

Anywhere a command takes an online player (`/tp`, `/msg`, `/give`, `/kill`, ...) you can use either
their username or their nickname, e.g. `/tp Steve` or `/tp "Mr Notch"`. Nicknames also show up in tab-complete.
Vanilla only accepts names up to 16 characters there, so longer nicknames only work in `/changename`.
Commands that take offline profiles (`/whitelist`, `/op`, `/ban`) still need the real username.

Names are trimmed, `§` and control characters are stripped, and they can be at most 32 characters.
A name can't be the same as another player's nickname or username.
They're saved in `config/whitelistnames.json`.

## Known limitations
- Nicknamed players are put on the scoreboard team `wn_nicknamed` (to hide the vanilla nametag),
  so this conflicts with other team usage.
- Players see their own floating name in third person.
- The floating name doesn't dim when sneaking like the vanilla nametag does.

## Building
Needs JDK 25. Run `./gradlew build`; the mod is `build/libs/whitelistnames-1.0.0.jar`
(not the `-sources` one). Put it plus Fabric API in the server's `mods` folder.

## Downloading the jar
- **Latest build:** the "Latest build" release on the repo's Releases page, updated on every push to `main`.
- **Versioned release:** push a tag, e.g. `git tag v1.0.0 && git push origin v1.0.0`, and a release with the jar is created.
- **Any branch/PR:** each workflow run in the Actions tab has the jar as a zipped artifact.
