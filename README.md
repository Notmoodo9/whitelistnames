# Whitelist Names (Fabric, Minecraft 26.2)

Server-side only. Players don't need to install anything.

Whitelisted players get a nickname that shows in chat, join/leave/death messages, the tab list
and on their vanilla nametag. Other players' games are told the nickname as the player's name, so the
nametag above their head is the real vanilla one (dims when sneaking, hidden when invisible). Each
player still sees their own real name, and the server itself keeps using real usernames.

## Commands
- `/whitelist add <username> <nickname>` - whitelists them and sets their nickname. Same permission as vanilla `/whitelist`.
- `/whitelist add <username>` - normal vanilla behavior
- `/changename <username or current nickname> <new nickname>` - renames someone.
  Same permission as `/whitelist` (or `/gamemode` in singleplayer/LAN).
- `/namecheck <nickname>` - shows the real username behind a nickname (or the nickname of a username). Ops only.
- `/nicks` - lists everyone's nickname and username. Ops only.
- `/eyerecipe disable` / `/eyerecipe enable` - turns the Eye of Ender crafting recipe off or on (ops only).
  `/eyerecipe` on its own shows whether it's on. Applies to crafting tables, the inventory grid and crafters,
  and is saved in `config/whitelistnames-settings.json` so it stays that way after a restart.

Nicknames must be valid Minecraft names: 1-16 characters, no spaces or quotes, not starting with `@`
(e.g. `MrNotch`, `Mr_Notch`, `Mr.Notch`). A nickname can't be another player's nickname or username.
Renaming someone who's online makes them briefly disappear and reappear for other players, so their
games pick up the new name. Nicknames are saved in `config/whitelistnames.json`.

Anywhere a command takes an online player (`/tp`, `/msg`, `/give`, `/kill`, ...) you can use either
their username or their nickname. Nicknames also show up in tab-complete.
Commands that take offline profiles (`/whitelist`, `/op`, `/ban`) still need the real username.

## Xaero's Minimap / World Map
Players are sent Xaero's fair-play code (`§f§a§i§r§x§a§e§r§o`) when they join, which turns off
cave mode (including on the world map) and the entity radar. It shows up as an empty chat line.

## Upgrading from an older version
Older versions put a floating text entity above players and added them to the team `wn_nicknamed`.
This version removes both automatically. Nicknames with spaces or over 16 characters from older
versions still show in chat and the tab list, but not above the head until you `/changename` them;
`/nicks` marks them.

## Building
Needs JDK 25. Run `./gradlew build`; the mod is `build/libs/whitelistnames-1.0.0.jar`
(not the `-sources` one). Put it plus Fabric API in the server's `mods` folder.

## Downloading the jar
- **Latest build:** the "Latest build" release on the repo's Releases page, updated on every push to `main`.
- **Versioned release:** push a tag, e.g. `git tag v1.0.0 && git push origin v1.0.0`, and a release with the jar is created.
- **Any branch/PR:** each workflow run in the Actions tab has the jar as a zipped artifact.
