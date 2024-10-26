package fr.kolala.advancedlocate.config;

import com.google.gson.*;
import fr.kolala.advancedlocate.AdvancedLocate;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ConfigHelper {

    public static final Map<String, Integer> defaultFieldValueMap = Map.of(
            "default_amount", 5,
            "max_amount", 10,
            "default_max_distance", 1600,
            "max_radius", 50,
            "max_neighbour_radius", 5
    );

    public static Set<String> listFields() {
        return defaultFieldValueMap.keySet();
    }

    public static int getDefaultValue(String field) {
        return defaultFieldValueMap.get(field);
    }

    public static JsonObject getDefaultJson() {
        JsonObject defaultContent = new JsonObject();
        defaultFieldValueMap.forEach(defaultContent::addProperty);
        return defaultContent;
    }

    /**
     * This method makes sure the config file is valid
     * @return True if it managed to repair the file or if it was already repaired, False otherwise.
     */
    public static boolean checkConfigFileIntegrity() {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            return createConfigFile();
        }
        JsonObject json = read();
        if(!write(checkConfigFields(json)) || !write(checkConfigValueTypes(json))) {
            AdvancedLocate.LOGGER.error("Could not fix the config file, the mod may not work properly.");
            return false;
        }

        AdvancedLocate.LOGGER.info("Config file is valid.");
        return true;
    }

    /**
     * This method checks if all the fields are present in the config file
     * @param json The JSON to check
     * @return The fixed JSON
     */
    private static JsonObject checkConfigFields(JsonObject json) {
        defaultFieldValueMap.forEach((key,value) -> {
            if (!json.has(key)) {
                json.addProperty(key, value);
                AdvancedLocate.LOGGER.warn("Config file does not contain {} key, adding with default value.", key);
            }
        });
        return json;
    }

    /**
     * This method checks if all the fields in the config file have a value matching their types
     * @param json The JSON to check
     * @return The fixed JSON
     */
    private static JsonObject checkConfigValueTypes(JsonObject json) {
        defaultFieldValueMap.forEach((key, value) -> {
            try {
                json.get(key).getAsInt();
            } catch(NumberFormatException e) {
                AdvancedLocate.LOGGER.warn("Config value for {} key is not a valid value, replacing by default value", key);
                json.addProperty(key, value);
            }
        });
        return json;
    }

    // Files related methods

    public static File getConfigFile() {
        return new File("config", AdvancedLocate.MOD_ID + ".json");
    }

    /**
     * This creates a new config file at the default path. Note that if a file already exists, it will be overwritten
     * @return True if it succeeded, False otherwise
     */
    public static boolean createConfigFile() {
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
            if (!createConfigFile()) {
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

    /**
     * Writes JSON to the config file
     * @param json The JSON to write
     * @return True if it wrote successfully, False otherwise
     */
    public static boolean write(JsonObject json) {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            createConfigFile();
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
    public static int getIntOrDefault(String name) {
        JsonElement value = get(name);
        return value == null ? defaultFieldValueMap.get(name) : value.getAsInt();
    }

    private static JsonElement get(String name) {
        JsonObject json = read();
        if (json == null) {
            AdvancedLocate.LOGGER.error("Couldn't retrieve the value for {}!", name);
            return null;
        }
        return json.get(name);
    }
}