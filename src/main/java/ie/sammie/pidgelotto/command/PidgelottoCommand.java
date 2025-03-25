package ie.sammie.pidgelotto.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import ie.sammie.pidgelotto.Pidgelotto;
import ie.sammie.pidgelotto.config.PidgelottoConfig;
import ie.sammie.pidgelotto.utils.ToggleNotifications;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;


public class PidgelottoCommand {

    private static final PidgelottoConfig config = PidgelottoConfig.loadConfig();
    public static String prefix = PidgelottoConfig.PREFIX;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
                CommandManager.literal("pidgelotto")
                        .executes(PidgelottoCommand::executeBaseCommand)
                        .then(
                                CommandManager.literal("buy").executes(PidgelottoCommand::executeBuySubCommand)
                        )
                        .then(
                                CommandManager.literal("cooldown").requires(source ->
                                                source.getEntity() == null || Permissions.check(source, "pidgelotto.command.cooldown"))
                                        .executes(PidgelottoCommand::executeCooldownSubCommand)
                        )
                        .then(
                                CommandManager.literal("end").requires(source ->
                                                source.getEntity() == null || Permissions.check(source, "pidgelotto.command.end"))
                                        .executes(PidgelottoCommand::executeEndSubCommand)
                        )
                        .then(
                                CommandManager.literal("notify").requires(source ->
                                                source.getEntity() == null || Permissions.check(source, "pidgelotto.command.notify"))
                                        .executes(PidgelottoCommand::executeNotifySubCommand)
                        )
                        .then(
                                CommandManager.literal("reload").requires(source ->
                                                source.getEntity() == null || Permissions.check(source, "pidgelotto.command.reload"))
                                        .executes(ReloadCommand::execute)
                        )
                        .then(
                                CommandManager.literal("run").requires(source ->
                                                source.getEntity() == null || Permissions.check(source, "pidgelotto.command.run"))
                                        .executes(PidgelottoCommand::executeRunSubCommand)
                        )
                        .then(
                                CommandManager.literal("time").requires(source ->
                                                source.getEntity() == null || Permissions.check(source, "pidgelotto.command.time"))
                                        .executes(PidgelottoCommand::executeTimeSubCommand)
                        )
        );
    }

    private static int executeBaseCommand(CommandContext<ServerCommandSource> context)
            throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        source.sendMessage(Text.literal(prefix + " (2025): Sammie_Dev, For help, run /pidgelotto help")
                .setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
        return 1;
    }

    private static int executeBuySubCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            Pidgelotto.INSTANCE.buyTicket(player);
        } else {
            source.sendMessage(Text.literal(prefix + "You must be a player to buy a ticket."));
        }
        return 1;
    }

    private static int executeNotifySubCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ToggleNotifications.toggleNotifications(player);
        } else {
            source.sendMessage(Text.literal(prefix + "This command can only be used by players."));
        }
        return 1;

    }

    private static int executeRunSubCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            if (Pidgelotto.INSTANCE == null) {
                source.sendError(Text.of(prefix + "Pidgelotto is not properly initialized. Please contact the server administrator."));
                return 0;
            }

            if (Pidgelotto.INSTANCE.isLotteryRunning()) {
                source.sendError(Text.of(prefix + "A lottery is already running. Please wait for it to finish."));
                return 0;
            }

            ServerWorld world = source.getWorld();
            if (world == null) {
                source.sendError(Text.of(prefix + "Unable to determine the server world."));
                return 0;
            }

            Pidgelotto.INSTANCE.startGracePeriod(world);

            return 1;
        } catch (Exception e) {
            Pidgelotto.LOGGER.error("An unexpected error occurred while executing the lottery: " + e.getMessage(), e);
            source.sendError(Text.of(prefix + "An error occurred while trying to start the lottery. Check the logs for more details."));
            return 0;
        }
    }

    private static int executeEndSubCommand(CommandContext<ServerCommandSource> context) {
        MinecraftServer server = context.getSource().getServer();
        ServerWorld world = server.getOverworld();

        if (Pidgelotto.INSTANCE.isLotteryRunning()) {
            Pidgelotto.INSTANCE.stopLottery(world);
            context.getSource().sendMessage(Text.literal(prefix + "Lottery has ended!"));
        } else {
            context.getSource().sendMessage(Text.literal(prefix + "No lottery is currently running."));
        }
        return 1;
    }

    private static int executeTimeSubCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (PidgelottoConfig.DEBUG_MODE) {
            Pidgelotto.LOGGER.info("{DEBUG} Lottery running: " + Pidgelotto.isLotteryRunning());
            Pidgelotto.LOGGER.info("{DEBUG} Current lottery timer: " + Pidgelotto.getLotteryTimer()/20 + " seconds");
            Pidgelotto.LOGGER.info("{DEBUG} Current lottery cooldown: " + Pidgelotto.getLottoCooldown()/20 + " seconds");
        }

        if (Pidgelotto.isLotteryRunning()) {
            // Lottery is running, display time remaining
            int timeRemaining = Pidgelotto.getLotteryTimer() / 20; // Convert ticks to seconds
            source.sendMessage(Text.literal(prefix + "Time remaining in the current lottery: " + timeRemaining + " seconds"));
        } else if (Pidgelotto.isGracePeriod()) {
            int gracePeriodRemaining = Pidgelotto.getGraceTimer() / 20;
            source.sendMessage(Text.literal(prefix + "Grace period remaining: " + gracePeriodRemaining + " seconds"));
        } else {
            // Lottery is not running, display cooldown time
            int cooldownRemaining = Pidgelotto.getLottoCooldown() / 20; // Convert ticks to seconds
            source.sendMessage(Text.literal(prefix + "Time remaining until the next lottery: " + cooldownRemaining + " seconds"));
        }

        return 1;

    }
    private static int executeCooldownSubCommand(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (Pidgelotto.INSTANCE.isLotteryRunning()) {
            source.sendMessage(Text.of(prefix + "A lottery is currently running."));
        } else {
            // Calculate time remaining in seconds based on the timer
            int cooldownInSecs = Pidgelotto.INSTANCE.getLottoCooldown();
            int minutes = cooldownInSecs / 60;
            int seconds = cooldownInSecs % 60;

            // Create a readable time message
            String timeMessage = String.format(prefix + "Time remaining before next lotto: %02d:%02d", minutes, seconds);
            source.sendMessage(Text.of(timeMessage));
        }
        return 1; // Indicates success
    }

}
