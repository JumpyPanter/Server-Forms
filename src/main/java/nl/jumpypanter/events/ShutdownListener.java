package nl.jumpypanter.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import nl.jumpypanter.ServerForms;

/**
 * Listens for server shutdown events and performs necessary cleanup or logging.
 */
public class ShutdownListener {

    /**
     * Registers the shutdown listener to handle server stopping events.
     * Logs a message indicating the server is shutting down.
     */
    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (server != null) {
                ServerForms.LOGGER.info("The server is stopping. FormMod is shutting down...");
                // Add any additional cleanup logic here if needed
            } else {
                ServerForms.LOGGER.warn("Server is null during shutdown. Skipping cleanup.");
            }
        });
    }
}