package dev.whitelistnames;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Stores UUID -> (username, nickname), saved to config/whitelistnames.json */
public final class NickStore {
	public record Entry(String username, String nickname) {}

	private static final Map<UUID, Entry> ENTRIES = new ConcurrentHashMap<>();
	private static Path file;

	private NickStore() {}

	public static void load(Path path) {
		file = path;
		if (!Files.exists(path)) return;
		try (Reader reader = Files.newBufferedReader(path)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			for (var e : root.entrySet()) {
				JsonObject o = e.getValue().getAsJsonObject();
				ENTRIES.put(UUID.fromString(e.getKey()),
						new Entry(o.get("username").getAsString(), o.get("nickname").getAsString()));
			}
			WhitelistNames.LOGGER.info("Loaded {} nicknames", ENTRIES.size());
		} catch (Exception ex) {
			WhitelistNames.LOGGER.error("Could not read {}", path, ex);
		}
	}

	private static void save() {
		JsonObject root = new JsonObject();
		ENTRIES.forEach((id, entry) -> {
			JsonObject o = new JsonObject();
			o.addProperty("username", entry.username());
			o.addProperty("nickname", entry.nickname());
			root.add(id.toString(), o);
		});
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
			}
		} catch (Exception ex) {
			WhitelistNames.LOGGER.error("Could not save {}", file, ex);
		}
	}

	public static String getNick(UUID id) {
		Entry e = ENTRIES.get(id);
		return e == null ? null : e.nickname();
	}

	public static void set(UUID id, String username, String nickname) {
		ENTRIES.put(id, new Entry(username, nickname));
		save();
	}

	/** Keeps the stored username current if the player renamed their Minecraft account. */
	public static void updateUsername(UUID id, String username) {
		Entry e = ENTRIES.get(id);
		if (e != null && !e.username().equals(username)) {
			ENTRIES.put(id, new Entry(username, e.nickname()));
			save();
		}
	}

	/** Finds a player by username first, then by current nickname (case-insensitive). */
	public static Map.Entry<UUID, Entry> find(String query) {
		for (var e : ENTRIES.entrySet()) {
			if (e.getValue().username().equalsIgnoreCase(query)) return e;
		}
		for (var e : ENTRIES.entrySet()) {
			if (e.getValue().nickname().equalsIgnoreCase(query)) return e;
		}
		return null;
	}

	public static Iterable<Entry> all() {
		return ENTRIES.values();
	}
}
