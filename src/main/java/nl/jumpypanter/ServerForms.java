package nl.jumpypanter;

import nl.jumpypanter.commands.CommandRegistry;
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
    public static final Logger LOGGER = LoggerFactory.getLogger("FormMod");

    /**
     * Called when the mod is initialized. Performs setup tasks such as loading configuration,
     * validating forms, registering commands, and setting up event listeners.
     */
    @Override
    public void onInitialize() {
        LOGGER.info("FormMod is initializing...");

        // Load configuration
        try {
            ConfigLoader.loadConfig();
            LOGGER.info("Configuration loaded successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration.", e);
            return; // Stop initialization if configuration fails
        }

        // Validate forms
        try {
            FormValidator.validateForms(ConfigLoader.getForms());
            LOGGER.info("Forms validated successfully.");
        } catch (IllegalArgumentException e) {
            LOGGER.error("Form validation failed: {}", e.getMessage());
            return; // Stop initialization if form validation fails
        }

        // Register commands
        try {
            CommandRegistry.register();
            LOGGER.info("Commands registered successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to register commands.", e);
            return; // Stop initialization if command registration fails
        }

        // Register events
        try {
            ShutdownListener.register();
            LOGGER.info("Event listeners registered successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to register event listeners.", e);
            return; // Stop initialization if event listener registration fails
        }

        LOGGER.info("FormMod has initialized successfully.");
    }
}