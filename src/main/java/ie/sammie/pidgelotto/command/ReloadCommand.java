package ie.sammie.pidgelotto.command;


import com.mojang.brigadier.context.CommandContext;
import ie.sammie.pidgelotto.Pidgelotto;
import ie.sammie.pidgelotto.config.PidgelottoConfig;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ReloadCommand {

    private static PidgelottoConfig CONFIG;

    private ReloadCommand() {}

    public static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        Pidgelotto.INSTANCE.reloadConfig();
        source.sendMessage(Text.literal("Pokelotto configuration reloaded successfully."));
        return 1;
    }
}
