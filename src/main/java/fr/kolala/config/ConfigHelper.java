package fr.kolala.config;

import com.google.gson.*;
import fr.kolala.AdvancedLocate;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ConfigHelper {

    public static final int DEFAULT_AMOUNT = 5;
    public static final int MAX_AMOUNT = 10;
    public static final int MAX_DELAY = 15;
    public static final int MAX_RADIUS = 50;
    public static final int MAX_NEIGHBOUR_RADIUS = 5;

    public static int getDefaultValue(String field) {
        if (Objects.equals(field, "default_amount")) return DEFAULT_AMOUNT;
        else if (Objects.equals(field, "max_amount")) return MAX_AMOUNT;
        else if (Objects.equals(field, "max_delay")) return MAX_DELAY;
        else if (Objects.equals(field, "max_radius")) return MAX_RADIUS;
        else if (Objects.equals(field, "max_neighbour_radius")) return MAX_NEIGHBOUR_RADIUS;
        else return 10;
    }

    public static JsonObject getDefaultJson() {
        JsonObject defaultContent = new JsonObject();
        defaultContent.addProperty("default_amount", DEFAULT_AMOUNT);
        defaultContent.addProperty("max_amount", MAX_AMOUNT);
        defaultContent.addProperty("max_delay", MAX_DELAY);
        defaultContent.addProperty("max_radius", MAX_RADIUS);
        defaultContent.addProperty("max_neighbour_radius", MAX_NEIGHBOUR_RADIUS);
        return defaultContent;
    }

    // Files related methods

    private static File getConfigFile() {
        return new File("config", AdvancedLocate.MOD_ID + ".json");
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
        return write(getDefaultJson());
    }

    public static @Nullable JsonObject read() {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            if (!createConfigFileIfNotExisting()) {
                return null;
            }
        }

        if (configFile.isFile() && configFile.canRead()) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(inputStreamReader).getAsJsonObject();
            }
            catch (Exception e) {
                AdvancedLocate.LOGGER.error("Failed to parse the JSON file '{}'", configFile.getAbsolutePath(), e);
                return null;
            }
        }
        else {
            AdvancedLocate.LOGGER.error("Config file is not readable.");
        }

        return null;
    }

    public static boolean write(JsonObject json) {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            createConfigFileIfNotExisting();
        }
        if (!configFile.isFile() || !configFile.canWrite()) {
            AdvancedLocate.LOGGER.error("Config file is not writable.");
            return false;
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