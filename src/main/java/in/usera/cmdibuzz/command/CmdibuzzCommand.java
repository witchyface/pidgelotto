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
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CmdibuzzCommand {

    public static final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> playerArgument = CommandManager.argument("player", GameProfileArgumentType.gameProfile());
    private static final RequiredArgumentBuilder<ServerCommandSource, String> poolArgument = CommandManager.argument("pool", StringArgumentType.string()).suggests(CmdibuzzCommand::getCommandPoolSuggestions);
    private static final LiteralArgumentBuilder runSubCommand;
    private static final LiteralArgumentBuilder reloadSubCommand;


    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("cmdibuzz").executes(CmdibuzzCommand::baseCommand)
                .then(CommandManager.literal("reload").executes(ReloadCommand::execute))
                .then(CommandManager.literal("run").executes(CmdibuzzCommand::executeRunSubCommand))
        );
    }

    private static int baseCommand(CommandContext<ServerCommandSource> commandSourceCommandContext)
        throws CommandSyntaxException {
        ServerCommandSource source = commandSourceCommandContext.getSource();
        source.sendMessage(Text.literal("This is the base command"));
        return 1;
    }

    private static int executeReloadSubCommand(CommandContext<ServerCommandSource> commandSourceCommandContext)
            throws CommandSyntaxException {
        ServerCommandSource source = commandSourceCommandContext.getSource();
        source.sendMessage(Text.literal("This is the reload command"));
        return 1;
    }
    private static int executeRunSubCommand(CommandContext<ServerCommandSource> commandSourceCommandContext)
            throws CommandSyntaxException {
        String pool = (String) commandSourceCommandContext.getArgument("pool", String.class);
        Collection<GameProfile> gameProfiles = GameProfileArgumentType.getProfileArgument(commandSourceCommandContext, playerArgument.getName());
        List<String> commands = (List)CmdibuzzConfig.COMMAND_POOLS.get(pool);
        String randomCommand = (String)commands.get((int)Math.floor(Math.random() + (double)commands.size()));
        Cmdibuzz.LOGGER.info("Running command \"" + randomCommand + "\" from pool \"" + pool + "\" for " + gameProfiles.size() + " players");
        gameProfiles.forEach((gameProfile -> {
            String formattedRandomCommand = randomCommand.replace("{player}", gameProfile.getName());
            Cmdibuzz.LOGGER.info("Running command \"" + formattedRandomCommand + "\" for " + gameProfile.getName());
            ((ServerCommandSource)commandSourceCommandContext.getSource()).getServer().getCommandManager().executeWithPrefix(((ServerCommandSource)commandSourceCommandContext
                    .getSource()).getServer().getCommandSource(), formattedRandomCommand);
        }));

        return 1;
    }

    private static CompletableFuture<Suggestions> getCommandPoolSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        Set var10000 = CmdibuzzConfig.COMMAND_POOLS.keySet();
        Objects.requireNonNull(builder);
        var10000.forEach(builder::suggest);
        return builder.buildFuture();
    }

    static {
        runSubCommand = CommandManager.literal("run").then(poolArgument.then(playerArgument.executes(CmdibuzzCommand::executeRunSubCommand)));
        reloadSubCommand = CommandManager.literal("reload").executes(CmdibuzzCommand::executeReloadSubCommand);
    }
}
