package in.usera.cmdibuzz.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import in.usera.cmdibuzz.config.CmdibuzzConfig;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CmdibuzzCommand {

    public static final RequiredArgumentBuilder<ServerCommandSource, GameProfileArgumentType.GameProfileArgument> playerArgument = CommandManager.argument("player", GameProfileArgumentType.gameProfile());
    private static final RequiredArgumentBuilder<ServerCommandSource, String> poolArgument = CommandManager.argument("pool", StringArgumentType.string()).suggests(CmdibuzzCommand::getCommandPoolSuggestions);
    private static final LiteralArgumentBuilder runSubCommand;
    private static final LiteralArgumentBuilder reloadSubCommand;
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("cmdibuzz").requires((serverCommandSource) -> {
            return serverCommandSource.hasPermissionLevel(3);
        })).then(runSubCommand)).then(reloadSubCommand));
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
        ServerCommandSource source = commandSourceCommandContext.getSource();
        source.sendMessage(Text.literal("This is the run command"));
        return 1;
    }

    private static CompletableFuture<Suggestions> getCommandPoolSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        Set<String> cmidbuzzSuggestions = CmdibuzzConfig.COMMAND_POOLS.keySet();
        Objects.requireNonNull(builder);
        cmidbuzzSuggestions.forEach(builder::suggest);
        return builder.buildFuture();
    }

    static {
        runSubCommand = CommandManager.literal("run").then(poolArgument.then(playerArgument.executes(CmdibuzzCommand::executeRunSubCommand)));
        reloadSubCommand = CommandManager.literal("reload").executes(CmdibuzzCommand::executeReloadSubCommand);
    }
}
