package in.usera.cmdibuzz.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import in.usera.cmdibuzz.Cmdibuzz;
import in.usera.cmdibuzz.config.CmdibuzzConfig;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Path;

public class ReloadCommand {

    private static CmdibuzzConfig CONFIG = new CmdibuzzConfig();

    private ReloadCommand() {}
    private static Text reloadText;
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(CommandManager.literal("reloadcmdibuzz").executes(ReloadCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> commandSourceCommandContext) {
        ServerCommandSource source = commandSourceCommandContext.getSource();
    try {
        reload(commandSourceCommandContext.getSource().getServer());
        source.sendMessage(Text.of("Reload was successful"));
    } catch (IOException e) {
        source.sendMessage(Text.of("Something unexpected happened."));
        e.printStackTrace();
    }
    return 0;
    }
    public static void reload(MinecraftServer server) throws IOException {
        CONFIG = new CmdibuzzConfig();
    }
}
