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
import in.usera.cmdibuzz.config.SuggestionProvider;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CmdibuzzCommand {

    public static final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> playerArgument = CommandManager.argument("player", GameProfileArgumentType.gameProfile());
    private static final RequiredArgumentBuilder<ServerCommandSource, String> poolArgument = CommandManager.argument("pool", StringArgumentType.string()).suggests(CmdibuzzCommand::getCommandPoolSuggestions);
    private static final LiteralArgumentBuilder baseCommand = CommandManager.literal("cmdibuzz").executes(CmdibuzzCommand::executeBaseCommand);
    private static final LiteralArgumentBuilder runSubCommand = CommandManager.literal("run").then(poolArgument.then(playerArgument.executes(CmdibuzzCommand::executeRunSubCommand)));
    private static final LiteralArgumentBuilder reloadSubCommand = CommandManager.literal("reload").executes(ReloadCommand::execute);



    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
                CommandManager.literal("cmdibuzz")
                        .executes(CmdibuzzCommand::executeBaseCommand) // Allow all players to execute this
                        .then(
                                CommandManager.literal("run")
                                        .requires(Permissions.require("cmdibuzz.command.run"))
                                        .then(poolArgument.then(playerArgument.executes(CmdibuzzCommand::executeRunSubCommand)))
                        )
                        .then(
                                CommandManager.literal("reload")
                                        .requires(Permissions.require("cmdibuzz.command.reload"))
                                        .executes(ReloadCommand::execute)
                        )
        );
    }

    private static int executeBaseCommand(CommandContext<ServerCommandSource> context)
        throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        source.sendMessage(Text.literal("Cmdibuzz (2024): Author (UseRainDance), Credits to Licious and Alxnns1.")

                .setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
        return 1;
    }

    private static int executeRunSubCommand(CommandContext<ServerCommandSource> context) {
        try {
            String pool = context.getArgument("pool", String.class);
            Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument(context, playerArgument.getName());
            List<String> commands = CmdibuzzConfig.COMMAND_POOLS.get(pool);

            if (commands == null || commands.isEmpty()) {
                throw new IllegalArgumentException("No commands found in the specified pool: " + pool);
            }

            String randomCommand = commands.get((int) Math.floor(Math.random() * commands.size()));
            Cmdibuzz.LOGGER.info("Running command \"" + randomCommand + "\" from pool \"" + pool + "\" for " + gameProfiles.size() + " players");

            gameProfiles.forEach((gameProfile -> {
                String formattedRandomCommand = randomCommand.replace("{player}", gameProfile.getName());
                Cmdibuzz.LOGGER.info("Running command \"" + formattedRandomCommand + "\" for " + gameProfile.getName());
                context.getSource().getServer().getCommandManager().executeWithPrefix(context.getSource()
                        .getServer().getCommandSource(), formattedRandomCommand);
            }));

            return 1;
        } catch (Exception e) {
            Cmdibuzz.LOGGER.error("An unexpected error occurred while executing the command: " + e.getMessage());
            e.printStackTrace(); // You can choose to remove this line in production
            return 0; // Return 0 to indicate failure
        }
    }

    private static CompletableFuture<Suggestions> getCommandPoolSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
     CmdibuzzConfig.COMMAND_POOLS.keySet().forEach(builder::suggest);
        return builder.buildFuture();
    }
}
