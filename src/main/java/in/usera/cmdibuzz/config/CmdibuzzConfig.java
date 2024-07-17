package in.usera.cmdibuzz.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import in.usera.cmdibuzz.Cmdibuzz;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static in.usera.cmdibuzz.Cmdibuzz.MOD_ID;

public class CmdibuzzConfig {

    private final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    public static Map<String, List<String>> COMMAND_POOLS = new HashMap<>();
    public static List<String> CRATE_KEYS = List.of();

    public CmdibuzzConfig() {
        File configFolder = new File(System.getProperty("user.dir") + "/config/cmdibuzz");
        File configFile = new File(configFolder, "config.json");
        if (!configFolder.exists()) {
            configFolder.mkdirs();
            createConfig(configFolder);
        } else if (!configFile.exists()) {
            createConfig(configFolder);
        }

        try {
            JsonObject obj = this.GSON.fromJson(new FileReader(configFile), JsonObject.class);
            List<JsonObject> commandPoolsJson = obj.get("command_pools").getAsJsonArray().asList().stream().map(JsonElement::getAsJsonObject).collect(Collectors.toList());
            commandPoolsJson.forEach(jsonObject -> {
                jsonObject.entrySet().forEach(entry -> {
                    List<String> commands = entry.getValue().getAsJsonArray().asList().stream().map(JsonElement::getAsString).collect(Collectors.toList());
                    COMMAND_POOLS.put(entry.getKey(), commands);
                    Cmdibuzz.LOGGER.info("[" + MOD_ID + "]: " + "Loaded command pool \"" + entry.getKey() + "\" (" + commands.size() + " commands)");
                });
            });

            List<JsonElement> crateKeysJson = obj.get("crate_keys").getAsJsonArray().asList();
            CRATE_KEYS = crateKeysJson.stream().map(JsonElement::getAsString).collect(Collectors.toList());
            Cmdibuzz.LOGGER.info("[" + MOD_ID + "]: " + "Loaded crate keys (" + CRATE_KEYS.size() + " keys)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createConfig(File configFolder) {
        File file = new File(configFolder, "config.json");
        try {
            file.createNewFile();
            JsonWriter writer = GSON.newJsonWriter(new FileWriter(file));
            writer.beginObject().name("command_pools").beginArray()
                    .beginObject().name("example1").beginArray()
                    .value("say Hello {player}")
                    .value("give {player} minecraft:diamond 1")
                    .value("op {player}")
                    .endArray().endObject()
                    .beginObject().name("example2").beginArray()
                    .value("say Goodbye {player}")
                    .value("give {player} minecraft:coal 1")
                    .value("deop {player}")
                    .endArray().endObject()
                    .endArray()
                    .name("crate_keys").beginArray()
                    .value("example_key_1")
                    .value("example_key_2")
                    .value("example_key_3")
                    .endArray().endObject()
                    .flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static CmdibuzzConfig loadConfig() {
        return new CmdibuzzConfig();
    }
}
