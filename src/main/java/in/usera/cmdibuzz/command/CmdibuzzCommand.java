package in.usera.cmdibuzz.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import in.usera.cmdibuzz.Cmdibuzz;
import in.usera.cmdibuzz.config.CmdibuzzConfig;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class CmdibuzzCommand {

    private static final CmdibuzzConfig config = CmdibuzzConfig.loadConfig();

    public static final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> playerArgument = CommandManager.argument("player", GameProfileArgumentType.gameProfile());
    private static final RequiredArgumentBuilder<ServerCommandSource, String> poolArgument = CommandManager.argument("pool", StringArgumentType.string()).suggests(CmdibuzzCommand::getCommandPoolSuggestions);
    private static final RequiredArgumentBuilder<ServerCommandSource, String> crateKeys = CommandManager.argument("crates", StringArgumentType.string()).suggests(CmdibuzzCommand::getCrateKeySuggestions);
    private static final LiteralArgumentBuilder baseCommand = CommandManager.literal("cmdibuzz").executes(CmdibuzzCommand::executeBaseCommand);
    private static final LiteralArgumentBuilder runSubCommand = CommandManager.literal("run").then(poolArgument.then(playerArgument.executes(CmdibuzzCommand::executeRunSubCommand)));
    private static final LiteralArgumentBuilder reloadSubCommand = CommandManager.literal("reload").executes(ReloadCommand::execute);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
                CommandManager.literal("cmdibuzz")
                        .executes(CmdibuzzCommand::executeBaseCommand) // Allow all players to execute this
                        .then(
                                CommandManager.literal("run").requires(Permissions.require("cmdibuzz.command.run"))
                                        .then(poolArgument.then(playerArgument.executes(CmdibuzzCommand::executeRunSubCommand)))
                        )
                        .then(
                                CommandManager.literal("reload").requires(Permissions.require("cmdibuzz.command.reload"))
                                        .executes(ReloadCommand::execute)
                        )
                        .then(
                                CommandManager.literal("staff").requires(Permissions.require("cmdibuzz.command.staff"))
                                        .then(
                                                CommandManager.literal("givekey")
                                                        .then(CommandManager.argument("crates", StringArgumentType.string())
                                                                .suggests(CmdibuzzCommand::getCrateKeySuggestions)
                                                                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                                                        .executes(CmdibuzzCommand::executeGiveKey)
                                                                ))))
        );
    }

    private static int executeBaseCommand(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        source.sendMessage(Text.literal("Cmdibuzz (2024): Author (Sammie_Dev), Credits to Licious and Alxnns1.")
                .setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
        return 1;
    }

    private static int executeRunSubCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            String pool = context.getArgument("pool", String.class);
            Collection<GameProfile> gameProfiles = context.getArgument("player", GameProfileArgumentType.GameProfileArgument.class).getNames(context.getSource());
            List<String> commands = CmdibuzzConfig.COMMAND_POOLS.get(pool);

            if (commands == null || commands.isEmpty()) {
                throw new IllegalArgumentException("No commands found in the specified pool: " + pool);
            }

            String randomCommand = commands.get((int) Math.floor(Math.random() * commands.size()));
            Cmdibuzz.LOGGER.info("Running command \"" + randomCommand + "\" from pool \"" + pool + "\" for " + gameProfiles.size() + " players");

            gameProfiles.forEach(gameProfile -> {
                String formattedRandomCommand = randomCommand.replace("{player}", gameProfile.getName());
                Cmdibuzz.LOGGER.info("Running command \"" + formattedRandomCommand + "\" for " + gameProfile.getName());
                context.getSource().getServer().getCommandManager().executeWithPrefix(context.getSource().getServer().getCommandSource(), formattedRandomCommand);
            });

            return 1;
        } catch (Exception e) {
            Cmdibuzz.LOGGER.error("An unexpected error occurred while executing the command: " + e.getMessage());
            e.printStackTrace();
            return 0; // Return 0 to indicate failure
        }
    }

    private static int executeGiveKey(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String keyName = context.getArgument("crates", String.class).replace('_', ' '); // Replace underscores with spaces
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument(context, "player");
        try{

            for (GameProfile profile : gameProfiles) {
                String command = String.format("padmin givekey %s 1 %s", profile.getName(), keyName);
                source.getServer().getCommandManager().executeWithPrefix(source.getServer().getCommandSource(), command);

                String logMessage = String.format("[%s] %s gave 1x of %s to %s",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    source.getName(), keyName, profile.getName());

                logCommandUsage(logMessage);
                source.sendMessage(Text.literal("Executed: " + command));
        }

            return 1;
        } catch (Exception e) {
            Cmdibuzz.LOGGER.error("Error executing givekey command: " + e.getMessage());
            e.printStackTrace();
            return 0; // Return 0 to indicate failure
        }
    }

    private static void logCommandUsage(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("config/cmdibuzz/keys_issued.log", true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            Cmdibuzz.LOGGER.error("Error logging command usage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static CompletableFuture<Suggestions> getCommandPoolSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        CmdibuzzConfig.COMMAND_POOLS.keySet().forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> getCrateKeySuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        CmdibuzzConfig.CRATE_KEYS.forEach(key -> builder.suggest(key.replace(' ', '_')));
        return builder.buildFuture();
    }
}
