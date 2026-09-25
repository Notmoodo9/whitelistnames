# Whitelist Names (Fabric, Minecraft 26.2)

Server-side only. Players don't need to install anything.

## Commands (op only)
- `/whitelist add <username> <name>` - whitelists them and gives them a name (chat, tab list, nametag)
- `/whitelist add <username>` - normal vanilla behavior
- `/changename <username or current name> <new name>` - put a current name with spaces in "quotes"
- `/whitelistnames reload` - reload the config after editing it

Names support & color codes, e.g. `/whitelist add Notch &6Mr. Notch`.

## Config: config/whitelistnames/config.json
Created on first launch. Placeholders: {username} {name} {oldname}
- whitelistAddMessage, alreadyWhitelistedMessage, nameChangedMessage
- notWhitelistedKickMessage (use \n for a new line, empty = vanilla message)
- broadcastToOps, customNameTags

Names are saved in config/whitelistnames/names.json.

## Building
Needs JDK 25. This folder has no gradlew scripts (they're binary), so either:
- open it in IntelliJ IDEA (it'll set up Gradle), or
- copy gradlew, gradlew.bat and gradle/wrapper/gradle-wrapper.jar from the Fabric template
  (https://fabricmc.net/develop/template/) into this folder.
Then run `./gradlew build`; the mod is build/libs/whitelistnames-1.0.0.jar. Put it plus Fabric API in the server's mods folder.
