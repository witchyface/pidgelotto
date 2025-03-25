package ie.sammie.pidgelotto.utils;

import com.google.gson.*;
import ie.sammie.pidgelotto.Pidgelotto;
import ie.sammie.pidgelotto.config.PidgelottoConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ToggleNotifications {
    private static final Logger LOGGER = LoggerFactory.getLogger("ToggleNotifications");
    private static final Set<UUID> playersWithNotificationsDisabled = new HashSet<>();
    private static final File NOTIFICATION_PREFS_FILE = new File("config/pidgelotto/preferences/notifications.json");
    public static String prefix = PidgelottoConfig.PREFIX;

    // Load preferences when the class is initialized
    static {
        loadNotificationPreferences();
    }

    /**
     * Toggles notifications for a player.
     *
     * @param player The player to toggle notifications for.
     */
    public static void toggleNotifications(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        if (playersWithNotificationsDisabled.contains(playerId)) {
            playersWithNotificationsDisabled.remove(playerId);
            player.sendMessage(Text.literal(prefix + "Lottery notifications have been enabled."), false);
        } else {
            playersWithNotificationsDisabled.add(playerId);
            player.sendMessage(Text.literal(prefix + "Lottery notifications have been disabled."), false);
        }

        saveNotificationPreferences(); // Save preferences after toggling
    }

    /**
     * Checks if a player has notifications enabled.
     *
     * @param playerId The UUID of the player to check.
     * @return True if notifications are enabled, false otherwise.
     */
    public static boolean areNotificationsEnabled(UUID playerId) {
        return !playersWithNotificationsDisabled.contains(playerId);
    }

    /**
     * Saves notification preferences to a file.
     */
    private static void saveNotificationPreferences() {
        // Ensure the directory exists
        File preferencesDir = NOTIFICATION_PREFS_FILE.getParentFile();
        if (!preferencesDir.exists()) {
            if (!preferencesDir.mkdirs()) {
                LOGGER.error("Failed to create directory: " + preferencesDir.getAbsolutePath());
                return;
            }
        }

        // Save the notification preferences to the file
        JsonArray jsonArray = new JsonArray();
        for (UUID playerId : playersWithNotificationsDisabled) {
            jsonArray.add(playerId.toString());
        }

        try (FileWriter writer = new FileWriter(NOTIFICATION_PREFS_FILE)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonArray, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save notification preferences.", e);
        }
    }

    /**
     * Loads notification preferences from a file.
     */
    private static void loadNotificationPreferences() {
        if (!NOTIFICATION_PREFS_FILE.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(NOTIFICATION_PREFS_FILE)) {
            JsonArray jsonArray = new Gson().fromJson(reader, JsonArray.class);
            for (JsonElement element : jsonArray) {
                UUID playerId = UUID.fromString(element.getAsString());
                playersWithNotificationsDisabled.add(playerId);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load notification preferences.", e);
        }
    }
}