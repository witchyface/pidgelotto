package in.usera.cmdibuzz;

import in.usera.cmdibuzz.command.CmdibuzzCommand;
import in.usera.cmdibuzz.command.ReloadCommand;
import in.usera.cmdibuzz.config.CmdibuzzConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cmdibuzz implements ModInitializer {
    public static final String MOD_ID = "cmdibuzz";

    public static Cmdibuzz INSTANCE;

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static CmdibuzzConfig CONFIG;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Cmdibuzz.");
        CONFIG = new CmdibuzzConfig();
        CommandRegistrationCallback.EVENT.register(CmdibuzzCommand::register);
        LOGGER.info("Cmdibuzz initialized!");

    }
}
