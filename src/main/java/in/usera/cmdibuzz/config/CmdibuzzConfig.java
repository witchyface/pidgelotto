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

public class CmdibuzzConfig {

    private final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    public static Map<String, List<String>> COMMAND_POOLS = new HashMap();

    public static String TEST_STRING = "This is a test";

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
            List<JsonObject> commandPoolsJson = obj.get("command_pools").getAsJsonArray().asList().stream().map(JsonElement::getAsJsonObject).toList();
            commandPoolsJson.forEach((jsonObject -> {
                jsonObject.asMap().forEach((name, jsonElement) -> {
                    List<String> commands = jsonElement.getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList();
                    COMMAND_POOLS.put(name, commands);
                    Cmdibuzz.LOGGER.info("Loaded command pool \"" + name + "\" (" + commands.size() + " commands)");

                }
                );
            }));
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
                    .endArray().endObject()
                    .flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
