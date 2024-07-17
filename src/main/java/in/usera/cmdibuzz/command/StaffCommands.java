package in.usera.cmdibuzz.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class StaffCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("staff")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("give")
                        .then(CommandManager.argument("key_name", StringArgumentType.word())
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> executeGiveKey(context,
                                                        StringArgumentType.getString(context, "key_name"),
                                                        StringArgumentType.getString(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "amount"))))))));
    }

    public static int executeGiveKey(CommandContext<ServerCommandSource> context, String keyName, String playerName, int amount) {
        ServerCommandSource source = context.getSource();
        String command = String.format("key give %s %s %d", keyName, playerName, amount);
        source.getServer().getCommandManager().executeWithPrefix(source.getServer().getCommandSource(), command);
        String message = String.format("[%s] %s gave %d of %s to %s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                source.getName(), amount, keyName, playerName);

        logCommandUsage(message);

        source.sendFeedback((Supplier<Text>) Text.literal("Executed: " + command), false);
        return 1;
    }

    private static void logCommandUsage(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("logs/staff_commands.log", true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
