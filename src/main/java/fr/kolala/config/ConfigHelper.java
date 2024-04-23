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

public class ConfigHelper {

    // Files related methods

    private static File getConfigFile() {
        return new File(new File(MinecraftClient.getInstance().runDirectory, "config"), AdvancedLocate.MOD_ID + ".json");
    }

    public static void createConfigFileIfNotExisting() {
        // Create the file
        try {
            if (getConfigFile().createNewFile()) {
                AdvancedLocate.LOGGER.info("Created config file.");
            }
        }
        catch (IOException e) {
            AdvancedLocate.LOGGER.error("Couldn't create config file.");
            return;
        }

        // Write default content into the file
        JsonObject defaultContent = new JsonObject();
        defaultContent.addProperty("default_amount", 5);
        defaultContent.addProperty("max_amount", 10);
        defaultContent.addProperty("max_delay", 15);
        defaultContent.addProperty("max_radius", 50);
        defaultContent.addProperty("max_neighbour_radius", 5);
        write(defaultContent);
    }

    public static @Nullable JsonObject read() {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            createConfigFileIfNotExisting();
        }

        if (configFile.isFile() && configFile.canRead()) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(inputStreamReader).getAsJsonObject();
            }
            catch (Exception e) {
                AdvancedLocate.LOGGER.error("Couldn't load config file.");
                return null;
            }
        }
        else {
            AdvancedLocate.LOGGER.error("Config file is not readable.");
        }

        return null;
    }

    public static void write(JsonObject json) {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            createConfigFileIfNotExisting();
        }
        if (!configFile.isFile() || !configFile.canWrite()) {
            AdvancedLocate.LOGGER.error("Config file is not writable.");
            return;
        }

        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            outputStreamWriter.write(new GsonBuilder().setPrettyPrinting().create().toJson(json));
        }
        catch (Exception e) {
            AdvancedLocate.LOGGER.error("Couldn't write file.");
        }

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
