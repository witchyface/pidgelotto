package ie.sammie.pidgelotto.config;

import com.google.gson.*;
import ie.sammie.pidgelotto.Pidgelotto;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PidgelottoConfig {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public static int LOTTO_DURATION;
    public static int LOTTO_COOLDOWN;
    public static int GRACE_PERIOD;
    public static int NUMBER_RANGE;
    public static int TICKET_PRICE;
    public static int MAX_TICKETS_PER_PLAYER;
    public static boolean TIMER_ENABLED;
    public static boolean DEBUG_MODE;
    public static String PREFIX;
    public static String FALLBACK_PRIZE = "cmd:give {player} minecraft:diamond 1";
    public static List<String> PRIZES = new ArrayList<>();

    private static final File CONFIG_FOLDER = new File(System.getProperty("user.dir") + "/config/pidgelotto");
    private static final File CONFIG_FILE = new File(CONFIG_FOLDER, "config.json");

    public PidgelottoConfig() {
        if (!CONFIG_FOLDER.exists()) {
            CONFIG_FOLDER.mkdirs();
        }

        if (!CONFIG_FILE.exists()) {
            createDefaultConfig();
        }

        loadConfigValues();

    }

    private void loadConfigValues() {

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject obj = GSON.fromJson(reader, JsonObject.class);

            LOTTO_DURATION = obj.get("lotto_duration").getAsInt() * 20;// Convert seconds to ticks
            LOTTO_COOLDOWN = obj.get("lotto_cooldown").getAsInt() * 20;
            GRACE_PERIOD = obj.get("grace_period").getAsInt() * 20;
            TIMER_ENABLED = obj.get("timer_enabled").getAsBoolean();
            NUMBER_RANGE = obj.get("number_range").getAsInt();
            TICKET_PRICE = obj.get("ticket_price").getAsInt();
            MAX_TICKETS_PER_PLAYER = obj.get("max_tickets_per_player").getAsInt();
            FALLBACK_PRIZE = obj.get("fallback_prize").getAsString();
            PREFIX = obj.get("prefix").getAsString();
            DEBUG_MODE = obj.get("debug").getAsBoolean();

            JsonArray prizesArray = obj.getAsJsonArray("prizes");
            PRIZES.clear();
            for (JsonElement element : prizesArray) {
                PRIZES.add(element.getAsString());
            }

            Pidgelotto.LOGGER.info("Loaded timer_enabled: {}", TIMER_ENABLED); // Log the timer_enabled value


            Pidgelotto.LOGGER.info(" Loaded " + PRIZES.size() + " prizes from config.");
            Pidgelotto.LOGGER.info(" Available tickets set to: " + NUMBER_RANGE);
            Pidgelotto.LOGGER.info(" Ticket price set to: " + TICKET_PRICE);
            Pidgelotto.LOGGER.info(" Max tickets per player set to: " + MAX_TICKETS_PER_PLAYER);
            Pidgelotto.LOGGER.info(" Lotto duration set to: " + LOTTO_DURATION + " ticks");
            Pidgelotto.LOGGER.info(" Lotto cooldown set to: " + LOTTO_COOLDOWN + " ticks");
            Pidgelotto.LOGGER.info(" Grace period set to: " + GRACE_PERIOD + " ticks");
            Pidgelotto.LOGGER.info(" Timer enabled: " + TIMER_ENABLED);
            Pidgelotto.LOGGER.info(" Debug mode: " + DEBUG_MODE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Pidgelotto configuration", e);
        }
    }

    private void createDefaultConfig() {
        try {
            CONFIG_FOLDER.mkdirs();

            JsonObject defaultConfig = new JsonObject();
            defaultConfig.addProperty("prefix", "[Pidgelotto] ");
            defaultConfig.addProperty("timer_enabled", TIMER_ENABLED);
            defaultConfig.addProperty("grace_period", 60);
            defaultConfig.addProperty("lotto_duration", 180);
            defaultConfig.addProperty("lotto_cooldown", 600);
            defaultConfig.addProperty("number_range", 500);
            defaultConfig.addProperty("ticket_price", 500);
            defaultConfig.addProperty("max_tickets_per_player", 10);
            defaultConfig.addProperty("fallback_prize", FALLBACK_PRIZE);

            JsonArray defaultPrizes = new JsonArray();
            defaultPrizes.add("diamond_sword");
            defaultPrizes.add("netherite_ingot");
            defaultPrizes.add("golden_apple");
            defaultPrizes.add("elytra");

            defaultConfig.add("prizes", defaultPrizes);
            defaultConfig.add("debug", new JsonPrimitive(false));

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                writer.write(GSON.toJson(defaultConfig));
                Pidgelotto.LOGGER.info("[" + Pidgelotto.MOD_ID + "]: Created default Pidgelotto configuration at " + CONFIG_FILE.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create default configuration file", e);
        }
    }

    public static PidgelottoConfig loadConfig() {
        return new PidgelottoConfig();
    }

    public static void reloadConfig() {
        PidgelottoConfig config = new PidgelottoConfig();
        config.loadConfigValues();
        Pidgelotto.CONFIG = config;
    }
}
