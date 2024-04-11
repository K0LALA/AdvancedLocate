package fr.kolala.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fr.kolala.AdvancedLocate;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ConfigHelper {

    // Files related methods

    private static File getConfigFile() {
        return new File(new File(MinecraftClient.getInstance().runDirectory, "config"), AdvancedLocate.MOD_ID + ".json");
    }

    private static boolean createConfigFileIfNotExist(File configFile) {
        try {
            if (configFile.createNewFile()) {
                AdvancedLocate.LOGGER.info("Created config file.");
                return true;
            }
        }
        catch (IOException e) {
            AdvancedLocate.LOGGER.error("Couldn't create config file.");
            return false;
        }
        return false;
    }

    public static JsonElement read() {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            if (createConfigFileIfNotExist(configFile)) {
                AdvancedLocate.LOGGER.error("An exception during file creation occurred.");
                return null;
            }
        }

        if (configFile.isFile() && configFile.canRead()) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(inputStreamReader);
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

    public static boolean write(JsonElement json) {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            if (createConfigFileIfNotExist(configFile)) {
                AdvancedLocate.LOGGER.error("An exception during file creation occurred.");
                return false;
            }
        }
        if (!configFile.isFile() || !configFile.canWrite()) {
            AdvancedLocate.LOGGER.error("Config file is not writable.");
            return false;
        }

        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            outputStreamWriter.write(new GsonBuilder().setPrettyPrinting().create().toJson(json));
            return true;
        }
        catch (Exception e) {
            AdvancedLocate.LOGGER.error("Couldn't write file.");
        }

        return false;
    }




}
