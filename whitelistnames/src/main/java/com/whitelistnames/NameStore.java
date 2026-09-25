package com.whitelistnames;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Holds the config and the username -> custom name list. Files live in config/whitelistnames/. */
public final class NameStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("whitelistnames");
    private static final Path CONFIG_FILE = DIR.resolve("config.json");
    private static final Path NAMES_FILE = DIR.resolve("names.json");

    public static Config config = new Config();
    /** key = lowercase username */
    private static Map<String, Entry> names = new LinkedHashMap<>();

    private NameStore() {}

    public static class Config {
        // Placeholders: {username} {name} {oldname}.  Color codes: &a, &l, etc.
        public String whitelistAddMessage = "&a{username} &7has been whitelisted as &e{name}&7!";
        public String alreadyWhitelistedMessage = "&e{username} &7was already whitelisted, they now go by &e{name}&7.";
        public String nameChangedMessage = "&7Renamed &e{oldname} &7(&f{username}&7) to &e{name}&7.";
        // Shown to people who try to join but aren't whitelisted. Leave empty for the vanilla message.
        public String notWhitelistedKickMessage = "&cYou are not whitelisted on this server!";
        // Also show the whitelist/rename message to other online ops (like vanilla does)
        public boolean broadcastToOps = true;
        // Show the custom name above players' heads
        public boolean customNameTags = true;
    }

    public static class Entry {
        public String username;
        public String name;

        public Entry(String username, String name) {
            this.username = username;
            this.name = name;
        }
    }

    public static void load() {
        try {
            Files.createDirectories(DIR);
            if (Files.exists(CONFIG_FILE)) {
                try (Reader r = Files.newBufferedReader(CONFIG_FILE)) {
                    Config loaded = GSON.fromJson(r, Config.class);
                    if (loaded != null) config = loaded;
                }
            }
            saveConfig(); // writes defaults / adds any new keys

            if (Files.exists(NAMES_FILE)) {
                try (Reader r = Files.newBufferedReader(NAMES_FILE)) {
                    Map<String, Entry> loaded = GSON.fromJson(r, new TypeToken<LinkedHashMap<String, Entry>>() {}.getType());
                    names = loaded != null ? loaded : new LinkedHashMap<>();
                }
            }
        } catch (Exception e) {
            WhitelistNames.LOGGER.error("Failed to load Whitelist Names files", e);
        }
    }

    private static void saveConfig() throws IOException {
        try (Writer w = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(config, w);
        }
    }

    private static void saveNames() {
        try {
            Files.createDirectories(DIR);
            try (Writer w = Files.newBufferedWriter(NAMES_FILE)) {
                GSON.toJson(names, w);
            }
        } catch (IOException e) {
            WhitelistNames.LOGGER.error("Failed to save names.json", e);
        }
    }

    public static Entry get(String username) {
        return names.get(username.toLowerCase(Locale.ROOT));
    }

    /** Finds by username first, then by current custom name (color codes and case ignored). */
    public static Entry find(String query) {
        Entry byUsername = get(query);
        if (byUsername != null) return byUsername;
        String plain = Text.strip(query);
        for (Entry e : names.values()) {
            if (e.name.equalsIgnoreCase(query) || Text.strip(e.name).equalsIgnoreCase(plain)) return e;
        }
        return null;
    }

    public static void setName(String username, String name) {
        names.put(username.toLowerCase(Locale.ROOT), new Entry(username, name));
        saveNames();
    }

    public static Collection<Entry> entries() {
        return names.values();
    }
}
