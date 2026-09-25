package dev.whitelistnames;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Server toggles, saved to config/whitelistnames-settings.json */
public final class Settings {
	private static Path file;
	private static volatile boolean eyeOfEnderRecipeDisabled;

	private Settings() {}

	public static void load(Path path) {
		file = path;
		if (!Files.exists(path)) return;
		try (Reader reader = Files.newBufferedReader(path)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			if (root.has("eyeOfEnderRecipeDisabled")) {
				eyeOfEnderRecipeDisabled = root.get("eyeOfEnderRecipeDisabled").getAsBoolean();
			}
		} catch (Exception ex) {
			WhitelistNames.LOGGER.error("Could not read {}", path, ex);
		}
	}

	private static void save() {
		JsonObject root = new JsonObject();
		root.addProperty("eyeOfEnderRecipeDisabled", eyeOfEnderRecipeDisabled);
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
			}
		} catch (Exception ex) {
			WhitelistNames.LOGGER.error("Could not save {}", file, ex);
		}
	}

	public static boolean isEyeOfEnderRecipeDisabled() {
		return eyeOfEnderRecipeDisabled;
	}

	public static void setEyeOfEnderRecipeDisabled(boolean disabled) {
		eyeOfEnderRecipeDisabled = disabled;
		save();
	}
}
