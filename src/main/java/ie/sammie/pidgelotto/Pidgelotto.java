package ie.sammie.pidgelotto;

        import com.mojang.brigadier.ParseResults;
        import com.mojang.brigadier.exceptions.CommandSyntaxException;
        import ie.sammie.pidgelotto.command.PidgelottoCommand;
        import ie.sammie.pidgelotto.config.PidgelottoConfig;
        import ie.sammie.pidgelotto.utils.DebugLogger;
        import ie.sammie.pidgelotto.utils.EconService;
        import ie.sammie.pidgelotto.utils.ToggleNotifications;
        import net.fabricmc.api.ModInitializer;
        import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
        import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
        import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
        import net.fabricmc.loader.api.FabricLoader;
        import net.minecraft.item.Item;
        import net.minecraft.item.ItemStack;
        import net.minecraft.item.Items;
        import net.minecraft.registry.Registries;
        import net.minecraft.server.MinecraftServer;
        import net.minecraft.server.command.ServerCommandSource;
        import net.minecraft.server.network.ServerPlayerEntity;
        import net.minecraft.server.world.ServerWorld;
        import net.minecraft.text.Text;
        import net.minecraft.util.Identifier;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;

        import java.util.*;

        public class Pidgelotto implements ModInitializer {
            public static final String MOD_ID = "Pidgelotto";
            public static final Pidgelotto INSTANCE = new Pidgelotto();
            public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
            public static PidgelottoConfig CONFIG;
            private static int numberRange;
            private static final Random RANDOM = new Random();
            private static Map<Integer, String> currentPrizePool = new HashMap<>();
            private static boolean lotteryRunning;
            private Map<ServerPlayerEntity, Integer> playerNumbers = new HashMap<>();
            private boolean timerEnabled = PidgelottoConfig.TIMER_ENABLED;
            private boolean timerDisabledMessageShown;
            public boolean noPlayersFoundMsgSent;
            private boolean lottoOnCooldown;
            private static int lottoCooldown;
            public int lotteryDuration;
            public static int lotteryTimer;
            public boolean debugMode = PidgelottoConfig.DEBUG_MODE;
            private boolean playersWereOffline = false;
            private static int graceTimer = PidgelottoConfig.GRACE_PERIOD;
            public static boolean isGracePeriod;
            private boolean graceMessageSent;
            public static String prefix = PidgelottoConfig.PREFIX;
            public static int ticketPrice = PidgelottoConfig.TICKET_PRICE;
            private static final Map<UUID, Integer> playerTicketsPurchased = new HashMap<>();
            private static int totalTicketsSold;
            private static final Set<Integer> usedTicketNumbers = new HashSet<>();



            @Override
            public void onInitialize() {
                CONFIG = PidgelottoConfig.loadConfig();
                timerDisabledMessageShown = false;
                lottoOnCooldown = true;
                debugMode = PidgelottoConfig.DEBUG_MODE;
                logDebug("-----------------------------------------------");
                logDebug("Initializing Pidgelotto. Debug mode is enabled.");
                logDebug("-----------------------------------------------");
                logInfo("Initializing Pidgelotto.");
                logDebug("Loaded Pidgelotto configuration.");
                logDebug("Registering Pidgelotto command.");
                CommandRegistrationCallback.EVENT.register(PidgelottoCommand::register);
                logInfo("Pidgelotto initialized!");
                ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);
                logDebug("Server Tick Event registered.");
                lottoCooldown = PidgelottoConfig.LOTTO_COOLDOWN;
                logDebug("Lottery cooldown set to: " + lottoCooldown / 20 + " seconds.");
                lotteryDuration = PidgelottoConfig.LOTTO_DURATION;
                logDebug("Lottery duration set to: " + lotteryDuration / 20 + " seconds.");
                numberRange = PidgelottoConfig.NUMBER_RANGE;
                logDebug("Number range set to: " + numberRange + ".");
                ticketPrice = PidgelottoConfig.TICKET_PRICE;
                logDebug("Ticket price set to: " + ticketPrice + ".");
                shufflePrizes();
                logDebug("Prizes shuffled.");
                lotteryTimer = lotteryDuration;
                logDebug("Lottery timer set to: " + lotteryDuration / 20 + " seconds.");
                prefix = PidgelottoConfig.PREFIX;
                logDebug("Prefix set to: " + prefix);
                ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
                    DebugLogger.close();
                    logInfo("Wild Pidgelotto fled.");
                    logDebug("-----------------------------------------------");
                    logDebug(" _______ _Wild Pidgelotto fled!_       _     _ \n" +
                            "⠀⠀⠀⠀⠀⠀⠀⠀⠀⣠⢴⡇⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
                            "⠀⠀⠀⠀⣀⡴⣧⠔⠋⠀⠞⣛⣿⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
                            "⠀⠀⠀⡰⢙⡔⠁⢀⣠⡴⠾⠛⠓⣶⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
                            "⠀⢀⣠⣷⣾⣶⣿⣭⡤⢤⣤⣤⣤⣄⣈⣓⡄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
                            "⣾⣡⣀⡀⠸⡏⠙⠻⣤⣾⣿⣿⣶⡢⢿⠀⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
                            "⠀⠉⢻⣽⡷⣿⠀⠀⠀⠙⣿⣿⣿⡗⡺⡇⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
                            "⠀⠀⣾⣥⣞⡟⠀⠀⠀⢠⣿⠟⡣⡍⠵⣻⡄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
                            "⠀⠀⢀⠏⠁⠀⠀⠀⢀⠏⠀⣘⣱⡼⠛⠓⠛⠦⣄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
                            "⠀⠀⡘⠀⠀⠀⠀⠀⠘⣆⠠⣟⡟⠀⠀⠀⠀⠀⠀⠉⢲⣄⡀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
                            "⠀⡰⠁⠀⠀⠀⠀⠀⠀⠈⠳⣾⠁⠀⠀⠀⠀⠀⠀⠀⠈⡳⢿⣧⠀⠀⠀⠀⣀⠴⠢⠤⣄⡀⠀⠀\n" +
                            "⢠⠇⠀⠀⠀⠀⠀⠀⠀⠀⠀⢹⡄⠀⠀⠀⠀⠀⠀⠀⡀⢗⣺⣿⠀⢀⣠⠔⠉⠀⠀⠀⠉⠓⠤⣀\n" +
                            "⢸⢠⠀⠀⡄⠀⠀⢀⠀⢣⡀⣿⣏⢟⣛⠶⣛⠷⣄⣦⡝⡾⣜⣿⢿⡿⣏⣶⣶⡶⠶⢛⠩⢛⣾⡏\n" +
                            "⢹⣸⠀⠀⢧⡀⠀⠘⡄⠀⣧⡆⢿⡞⣧⣻⢼⡻⣿⣶⣿⣽⡟⠋⢈⣷⡿⣏⣶⣹⣭⣦⣇⡦⠶⠛\n" +
                            "⠀⠙⢇⣰⣸⣷⡀⠀⢳⣀⡟⠁⠈⢿⣳⣝⢾⣱⢿⣎⠁⠉⠀⠀⡈⡟⡍⠉⠉⠁⠀⠀⠀⠀⠀⠀\n" +
                            "⠀⠀⠀⠈⠫⣉⠙⠒⠚⠉⠀⠀⠀⠀⠙⠿⣟⡿⠞⠋⠀⡀⠀⠀⠑⡸⣇⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
                            "⠀⠀⠀⠀⠀⠀⠑⠢⣀⠀⠀⠀⠀⠀⠀⠀⠀⠉⠲⢤⣀⠈⠢⣄⠀⠘⣇⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
                            "⠀⠀⠀⠀⠀⣀⣀⣀⣀⣨⠷⢲⣶⣤⣤⡤⠶⡶⠒⠉⠁⠉⠁⠚⠓⠤⠼⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
                            "⠀⠀⢠⠚⣻⣭⠻⣟⣹⡭⠶⢧⢮⠿⣻⠅⠀⠛⠶⠦⣄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
                            "⠀⠀⠙⠋⠉⠧⠶⡟⢁⡷⢤⠾⢏⠉⢁⡴⠛⠙⠒⠒⠓⠃⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
                            "⠀⠀⠀⠀⠀⠀⠀⠋⠁⠀⣇⠴⠾⢿⠋⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀");
                    logDebug("-----------------------------------------------");
                });
            }

            public void reloadConfig() {
                PidgelottoConfig.reloadConfig();
                timerEnabled = PidgelottoConfig.TIMER_ENABLED;
                lottoCooldown = PidgelottoConfig.LOTTO_COOLDOWN;
                lottoOnCooldown = true;
                lotteryDuration = PidgelottoConfig.LOTTO_DURATION;
                numberRange = PidgelottoConfig.NUMBER_RANGE;
                ticketPrice = PidgelottoConfig.TICKET_PRICE;
                debugMode = PidgelottoConfig.DEBUG_MODE;
                graceTimer = PidgelottoConfig.GRACE_PERIOD;
                lotteryTimer = lotteryDuration;
                isGracePeriod = false;
                lotteryRunning = false;
                timerDisabledMessageShown = false;
                noPlayersFoundMsgSent = false;
                playersWereOffline = false;
                graceMessageSent = false;
                playerNumbers.clear();
                shufflePrizes();
                logInfo("Pidgelotto config reloaded.");
                if (debugMode) {
                    logDebug("Pidgelotto config reloaded.");
                    logDebug("Timer Enabled: %s", timerEnabled);
                    logDebug("Lottery Cooldown: %s", lottoCooldown);
                    logDebug("Lottery Duration: %s", lotteryDuration);
                    logDebug("Lottery Timer: %s", lotteryTimer);
                    logDebug("Number Range: %s", numberRange);
                    logDebug("Ticket Price: %s", ticketPrice);
                    logDebug("Debug Mode: %s", debugMode);
                    logDebug("Prizes: %s", currentPrizePool.values());
                    logDebug("Grace Period: %s", graceTimer);
                }
            }

            private void onServerTick(MinecraftServer server) {
                ServerWorld world = server.getOverworld();
                if (world == null) return; // Ensure the world is not null

                // Check if players are online
                boolean playersOnline = !world.getPlayers().isEmpty();

                // Handle the flow based on the current state
                if (graceTimer > 0) {
                    handleGracePeriod(world);
                } else if (lotteryRunning) {
                    handleLotteryRunning(server, world);
                } else if (lottoCooldown > 0) {
                    handleCooldown(server);
                } else if (playersOnline) {
                    startGracePeriod(world);
                } else {
                    handleNoPlayers();
                }
            }

            private void handleGracePeriod(ServerWorld world) {
                isGracePeriod = true;
                graceTimer--;

                // Broadcast messages at specific intervals
                if (graceTimer % 20 == 0) { // Every second
                    int secondsRemaining = graceTimer / 20;
                    for (ServerPlayerEntity player : world.getPlayers()) {
                        if (ToggleNotifications.areNotificationsEnabled(player.getUuid())) {
                            if (secondsRemaining == 60) {
                                player.sendMessage(Text.literal(prefix + "Lottery starting in " + secondsRemaining + " seconds. Purchase your ticket now!"), false);
                            } else if (secondsRemaining == 30) {
                                player.sendMessage(Text.literal(prefix + "Lottery starting in " + secondsRemaining + " seconds. Purchase your ticket now!"), false);
                            } else if (secondsRemaining == 15) {
                                player.sendMessage(Text.literal(prefix + "Lottery starting in " + secondsRemaining + " seconds. Purchase your ticket now!"), false);
                            } else if (secondsRemaining == 10) {
                                player.sendMessage(Text.literal(prefix + "Lottery starting in " + secondsRemaining + " seconds. Purchase your ticket now!"), false);
                            } else if (secondsRemaining == 5) {
                                player.sendMessage(Text.literal(prefix + "Lottery starting in " + secondsRemaining + " seconds. Purchase your ticket now!"), false);
                            } else if (secondsRemaining == 1) {
                                player.sendMessage(Text.literal(prefix + "Lottery starting now. Ticket purchases are now closed."), false);
                            }
                        }
                    }
                }

                // Start the lottery when the grace period ends
                if (graceTimer == 0) {
                    isGracePeriod = false;
                    startLottery(world);
                }
            }

            private void handleLotteryRunning(MinecraftServer server, ServerWorld world) {
               int remainingTime = lotteryTimer / 20;
                if (lotteryTimer > 0) {
                    lotteryTimer--;
                    if (lotteryTimer % 20 == 0 && debugMode) {
                        logDebug("Time remaining in lottery: %s seconds", lotteryTimer / 20);
                    }
                    for (ServerPlayerEntity player : world.getPlayers()) {
                        if (ToggleNotifications.areNotificationsEnabled(player.getUuid())) {
                            if (remainingTime == 60) {
                                player.sendMessage(Text.literal(prefix + "Lottery ending in " + remainingTime + " seconds. Good luck!"), false);
                            } else if (remainingTime == 30) {
                                player.sendMessage(Text.literal(prefix + "Lottery ending in " + remainingTime + " seconds. Good luck!"), false);
                            } else if (remainingTime == 15) {
                                player.sendMessage(Text.literal(prefix + "Lottery ending in " + remainingTime + " seconds. Good luck!"), false);
                            } else if (remainingTime == 10) {
                                player.sendMessage(Text.literal(prefix + "Lottery ending in " + remainingTime + " seconds. Good luck!"), false);
                            } else if (remainingTime == 5) {
                                player.sendMessage(Text.literal(prefix + "Lottery ending in " + remainingTime + " seconds. Good luck!"), false);
                            } else if (remainingTime == 1) {
                                player.sendMessage(Text.literal(prefix + "Lottery ending in " + remainingTime + " seconds. Good luck!"), false);
                            }
                        }
                    }
                } else {
                    stopLottery(server.getOverworld());
                }
            }

            private void handleCooldown(MinecraftServer server) {
                lottoCooldown--;
                if (lottoCooldown % 20 == 0 && debugMode) {
                    logDebug("Cooldown remaining: %s seconds", lottoCooldown / 20);
                }

                // Start the grace period when the cooldown ends
                if (lottoCooldown == 0) {
                    startGracePeriod(server.getOverworld());
                }
            }

            public void startGracePeriod(ServerWorld world) {
                if (world.getPlayers().isEmpty()) {
                    handleNoPlayers();
                    return;
                }

                graceTimer = PidgelottoConfig.GRACE_PERIOD;
                isGracePeriod = true;

                // Broadcast the initial grace period message
                world.getServer().getPlayerManager().broadcast(
                        Text.literal(prefix + "A lottery is scheduled to begin in 30 seconds. Purchase your ticket now!"),
                        false
                );

                logDebug("Grace period started. Players can now purchase tickets.");
            }

            private void handleNoPlayers() {
                if (!noPlayersFoundMsgSent) {
                    logDebug("No players found. Grace period and lottery will not start.");
                    noPlayersFoundMsgSent = true;
                }
            }

            public void buyTicket(ServerPlayerEntity player) {
                if (lotteryRunning) {
                    player.sendMessage(Text.literal(prefix + "A lottery is already running. Please wait for it to finish."));
                    return;
                }

                ServerWorld world = player.getServerWorld();
                if (world == null) {
                    player.sendMessage(Text.literal(prefix + "Unable to determine the server world."));
                    return;
                }

                UUID playerId = player.getUuid();
                double ticketPrice = PidgelottoConfig.TICKET_PRICE;

                if (totalTicketsSold >= PidgelottoConfig.NUMBER_RANGE) {
                    player.sendMessage(Text.literal(prefix + "You're out of luck. " +
                            "The last ticket for this lotto has just been sold! Try again in the next lottery!"), false);
                    return;
                }

                int ticketsPurchased = playerTicketsPurchased.getOrDefault(playerId, 0);
                if (ticketsPurchased >= PidgelottoConfig.MAX_TICKETS_PER_PLAYER) {
                    player.sendMessage(Text.literal(prefix + "You have reached the maximum number of tickets ("
                            + PidgelottoConfig.MAX_TICKETS_PER_PLAYER + ") for this lottery."), false);
                    return;
                }

                if (EconService.hasBalance(playerId, ticketPrice)) {
                    if (EconService.withdraw(playerId, ticketPrice)) {
                        player.sendMessage(Text.literal(prefix + "You have paid $" + ticketPrice + " to enter the lottery. Good luck!"), false);
                        if (debugMode) {
                            LOGGER.info("Player {} has paid ${} to enter the lottery.", player.getName().getString(), ticketPrice);
                        }

                        int ticketNumber;
                        do {
                            ticketNumber = RANDOM.nextInt(numberRange) + 1;
                        } while (usedTicketNumbers.contains(ticketNumber));

                        usedTicketNumbers.add(ticketNumber);

                        String ticketNumberString = "PKLT00" + ticketNumber;
                        playerNumbers.put(player, ticketNumber);

                        playerTicketsPurchased.put(playerId, ticketsPurchased + 1);
                        totalTicketsSold++;

                        player.sendMessage(Text.literal(prefix + "Your ticket number is " + ticketNumberString + ". Good Luck!"),
                                false);
                        logDebug("Player %s has purchased ticket number: %s", player.getName().getString(), ticketNumberString);
                    } else {
                        player.sendMessage(Text.literal(prefix + "Failed to process your payment. Please try again."), false);
                    }
                } else {
                    player.sendMessage(Text.literal(prefix + "You do not have enough money to enter the lottery. You need $" + ticketPrice + " to enter."), false);
                }
            }

            public void startLottery(ServerWorld world) {
                if (lotteryRunning) {
                    LOGGER.info("A lottery is already running!");
                    return;
                }

                lotteryRunning = true;
                lotteryTimer = lotteryDuration; // Reset the lottery timer
                LOGGER.info("Lottery has started! Lottery Timer set to {} seconds.", lotteryTimer / 20);

                if (world.getPlayers().isEmpty()) {
                    LOGGER.info("No players found. Ending the lottery.");
                    stopLottery(world);
                }
            }

            public void stopLottery(ServerWorld world) {
                if (lotteryRunning) {
                    announceResults(world);
                    lotteryRunning = false;
                    playerNumbers.clear();
                    playerTicketsPurchased.clear();
                    usedTicketNumbers.clear();
                    totalTicketsSold = 0;
                    LOGGER.info("Lottery has ended!");
                    lottoCooldown = PidgelottoConfig.LOTTO_COOLDOWN;
                } else {
                    LOGGER.info("No lottery is currently running!");
                }
            }


            private void shufflePrizes() {
                currentPrizePool.clear();

                List<String> prizes = PidgelottoConfig.PRIZES;

                if (prizes == null || prizes.isEmpty()) {
                    LOGGER.warn("No prizes found in the config file. Using default prizes.");
                    prizes = Arrays.asList("minecraft:diamond", "minecraft:emerald", "minecraft:gold_ingot");
                }

                for (int i = 0; i < prizes.size(); i++) {
                    String prize = prizes.get(i).toLowerCase();

                    if (prize.startsWith("cmd:") || getItemByPrizeName(prize) != null) {
                        currentPrizePool.put(i + 1, prize);
                    } else {
                        LOGGER.error("Invalid prize detected while shuffling: {}. Skipping.", prize);
                    }
                }

                logDebug("Prize pool shuffled with the following prizes: " + currentPrizePool.values());
            }

            private void announceResults(ServerWorld world) {
                if (playerNumbers.isEmpty()) {
                    LOGGER.info("No players participated in the lottery. Ending the lottery without results.");
                    return;
                }

                int winningNumber1 = RANDOM.nextInt(numberRange) + 1;
                int winningNumber2 = RANDOM.nextInt(numberRange) + 1;
                int winningNumber3 = RANDOM.nextInt(numberRange) + 1;
                List<ServerPlayerEntity> winners = new ArrayList<>();

                for (Map.Entry<ServerPlayerEntity, Integer> entry : playerNumbers.entrySet()) {
                    int ticketNumber = entry.getValue();
                    if (ticketNumber == winningNumber1 || ticketNumber == winningNumber2 || ticketNumber == winningNumber3) {
                        winners.add(entry.getKey());
                    }
                }

                LOGGER.info("The winning tickets were: PKLT00{}, PKLT00{}, PKLT00{}.", winningNumber1, winningNumber2, winningNumber3);

                if (winners.isEmpty()) {
                    world.getServer().sendMessage(Text.literal("No winners this time. Better luck next time!"));
                } else {
                    for (ServerPlayerEntity winner : winners) {
                        String prize = currentPrizePool.getOrDefault(RANDOM.nextInt(currentPrizePool.size()), PidgelottoConfig.FALLBACK_PRIZE);
                        givePrizeToPlayer(winner, prize);
                        winner.sendMessage(Text.literal(prefix + "Congratulations! Your number has won " + prize + "!"), false);
                    }

                }
                LOGGER.info("Lottery results have been announced, and the winners have been informed.");
                world.getServer().getPlayerManager().broadcast(Text.literal(prefix + "The winning tickets were: " + "PKLT00" + winningNumber1 + ", " + "PKLT00" + winningNumber2 + ", " + "PKLT00" + winningNumber3 + ".\nCongratulations to the winners!"), false);
            }

            private void givePrizeToPlayer(ServerPlayerEntity player, String prize) {
                if (prize.startsWith("cmd:")) {
                    String command = prize.substring(4).replace("{player}", player.getName().getString());
                    executeServerCommand(command);
                    player.sendMessage(Text.of(prefix + "A special reward has been executed for you!"), false);
                    return;
                }

                ItemStack itemStack = getItemByPrizeName(prize);
                if (itemStack == null || itemStack.isEmpty()) {
                    LOGGER.error("Failed to process prize: {}. Item mapping not found.", prize);
                    player.sendMessage(Text.literal(prefix + "Unable to process your prize: " + prize + ". Please contact an administrator."), false);
                    LOGGER.info("Player did not receive their prize. Please investigate why.");
                    return;
                }
                boolean success = player.getInventory().insertStack(itemStack);
                if (success) {
                    player.sendMessage(Text.literal(prefix + "Congratulations! You have received: " + prize + "!"), false);
                } else {
                    LOGGER.warn("Failed to deliver prize '{}' to player {}. Inventory full?", prize, player.getName().getString());
                    player.sendMessage(Text.literal(prefix + "Your prize could not be delivered because your inventory is full. Please make space and contact an admin."), false);
                }
            }

            private ItemStack getItemByPrizeName(String prizeName) {
                if (prizeName.startsWith("cmd:")) {
                    return null;
                }

                if (!prizeName.contains(":")) {
                    prizeName = "minecraft:" + prizeName;
                }

                Identifier itemId = Identifier.tryParse(prizeName);
                if (itemId == null) {
                    LOGGER.error("Invalid prize identifier: {}", prizeName);
                    return null;
                }

                Item item = Registries.ITEM.get(itemId);
                if (item == Items.AIR) {
                    LOGGER.error("Could not find item for prize: {}", prizeName);
                    return null;
                }

                return new ItemStack(item, 1);
            }

            private void executeServerCommand(String command) {
                MinecraftServer server = getServerInstance();
                if (server == null) return;

                ServerCommandSource commandSource = server.getCommandSource();
                try {
                    ParseResults<ServerCommandSource> parseResults = server.getCommandManager().getDispatcher().parse(command, commandSource);
                    server.getCommandManager().getDispatcher().execute(parseResults);
                } catch (CommandSyntaxException e) {
                    commandSource.sendError(Text.of("Failed to execute command: " + e.getMessage()));
                }
            }

            private MinecraftServer getServerInstance() {
                return FabricLoader.getInstance().getGameInstance() instanceof MinecraftServer server ? server : null;
            }

            public static boolean isLotteryRunning() {
                return lotteryRunning;
            }

            public static boolean isGracePeriod() {
                return isGracePeriod;
            }

            public static int getGraceTimer() {
                return graceTimer;
            }

            public static int getLotteryTimer() {
                return lotteryTimer;
            }

            public static int getLottoCooldown() {
                return lottoCooldown;
            }

            public void logDebug(String format, Object... args) {
                debugMode = PidgelottoConfig.DEBUG_MODE;
                if (debugMode) {
                    String formattedMessage = String.format(format, args);
                    DebugLogger.log(formattedMessage);
                }
            }

            public void logInfo(String message) {
                LOGGER.info(message);
            }
        }