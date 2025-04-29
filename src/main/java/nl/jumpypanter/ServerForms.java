package nl.jumpypanter;

import nl.jumpypanter.commands.CommandRegistry;
import nl.jumpypanter.commands.FormCommandHandler;
import nl.jumpypanter.config.ConfigLoader;
import nl.jumpypanter.events.FormValidator;
import nl.jumpypanter.events.ShutdownListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for the FormMod. Handles initialization of the mod, including
 * loading configuration, validating forms, registering commands, and setting up event listeners.
 */
public class ServerForms implements net.fabricmc.api.ModInitializer {

    /**
     * Logger instance for logging messages related to the FormMod.
     */
    public static final Logger LOGGER = LoggerFactory.getLogger("FormMod");

    /**
     * Initializes the mod by performing setup tasks such as loading configuration,
     * validating forms, registering commands, and setting up event listeners.
     */
    @Override
    public void onInitialize() {
        LOGGER.info("FormMod is initializing...");

        if (!initializeComponent("Configuration", ConfigLoader::loadConfig)) return;
        if (!initializeComponent("Forms Validation", () -> FormValidator.validateForms(ConfigLoader.getForms()))) return;
        if (!initializeComponent("Commands", () -> {
            CommandRegistry.register();
            FormCommandHandler.registerCommands();
        })) return;
        if (!initializeComponent("Event Listeners", ShutdownListener::register)) return;

        LOGGER.info("Server Forms has initialized successfully.");
    }

    /**
     * Utility method to initialize a component with error handling.
     *
     * @param componentName The name of the component being initialized.
     * @param initializer   The initialization logic as a Runnable.
     * @return true if the initialization succeeds, false otherwise.
     */
    private boolean initializeComponent(String componentName, Runnable initializer) {
        try {
            initializer.run();
            LOGGER.info("{} initialized successfully.", componentName);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to initialize {}.", componentName, e);
            return false;
        }
    }
}
