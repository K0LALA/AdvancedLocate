package fr.kolala.config;

import com.google.gson.*;
import fr.kolala.AdvancedLocate;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ConfigHelper {

    // Files related methods

    private static File getConfigFile() {
        return new File(new File(MinecraftClient.getInstance().runDirectory, "config"), AdvancedLocate.MOD_ID + ".json");
    }

    public static boolean doesConfigFileExists() {
        return getConfigFile().exists();
    }

    public static boolean createConfigFileIfNotExisting() {
        // Create the file
        try {
            if (getConfigFile().createNewFile()) {
                AdvancedLocate.LOGGER.info("Created config file.");
            }
        }
        catch (IOException e) {
            AdvancedLocate.LOGGER.error("Couldn't create config file.");
            return false;
        }

        // Write default content into the file
        JsonObject defaultContent = new JsonObject();
        defaultContent.addProperty("default_amount", 5);
        defaultContent.addProperty("max_amount", 10);
        defaultContent.addProperty("max_delay", 15);
        defaultContent.addProperty("max_radius", 50);
        defaultContent.addProperty("max_neighbour_radius", 5);
        return write(defaultContent);
    }

    public static @Nullable JsonObject read() {
        File configFile = getConfigFile();

        if (!configFile.exists() || !configFile.isFile() || !configFile.canRead()) {
            AdvancedLocate.LOGGER.error("Config file is not readable, Creating a new one.");
            if (!createConfigFileIfNotExisting()) {
                AdvancedLocate.LOGGER.error("Couldn't create config file.");
                return null;
            }
        }

        try (InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(inputStreamReader).getAsJsonObject();
        }
        catch (Exception e) {
            AdvancedLocate.LOGGER.error("Failed to parse the JSON file '{}'", configFile.getAbsolutePath(), e);
            return null;
        }
    }

    public static boolean write(JsonObject json) {
        File configFile = getConfigFile();

        if (!configFile.isFile() || !configFile.canWrite() || !configFile.exists()) {
            AdvancedLocate.LOGGER.error("Config file is not writable, Creating a new one.");
            if (!createConfigFileIfNotExisting()) {
                AdvancedLocate.LOGGER.error("Couldn't create config file.");
                return false;
            }
        }

        File fileTmp = new File(configFile.getParentFile(), configFile.getName() + ".tmp");

        if (fileTmp.exists())
        {
            fileTmp = new File(configFile.getParentFile(), UUID.randomUUID() + ".tmp");
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileTmp), StandardCharsets.UTF_8))
        {
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(json));
            writer.close();

            if (configFile.exists() && configFile.isFile() && !configFile.delete())
            {
                AdvancedLocate.LOGGER.warn("Failed to delete file '{}'", configFile.getAbsolutePath());
            }

            return fileTmp.renameTo(configFile);
        }
        catch (Exception e)
        {
            AdvancedLocate.LOGGER.warn("Failed to write JSON data to file '{}'", fileTmp.getAbsolutePath(), e);
        }

        return false;
    }


    // Config related methods

    public static int getInt(String name) {
        return Objects.requireNonNull(get(name)).getAsInt();
    }

    private static JsonElement get(String name) {
        JsonObject json = read();
        if (json == null) {
            AdvancedLocate.LOGGER.error("Couldn't retrieve the value for {}!", name);
            return null;
        }
        return json.get(name);
    }

    public static Set<String> listFields() {
        JsonObject json = read();
        if (json == null) {
            AdvancedLocate.LOGGER.error("Couldn't list fields in json config!");
            return new HashSet<>();
        }
        return json.keySet();
    }

}
