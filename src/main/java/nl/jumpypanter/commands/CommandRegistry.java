package nl.jumpypanter.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import nl.jumpypanter.ServerForms;
import nl.jumpypanter.config.ConfigLoader;
import nl.jumpypanter.events.FormHandler;
import nl.jumpypanter.events.FormValidator;
import nl.jumpypanter.utils.TextFormatter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Handles the registration of commands for the form mod.
 * Includes commands for starting forms, answering forms, viewing form responses, and reloading forms.
 */
public class CommandRegistry {
    private static final File FORM_ANSWERS_DIR = new File("mods", "FormAnswers");

    /**
     * Registers all commands for the form mod.
     * This includes commands for starting forms, answering forms, viewing form responses, and reloading forms.
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Load forms from the configuration
            JsonObject forms = ConfigLoader.getForms();
            for (Map.Entry<String, JsonElement> entry : forms.entrySet()) {
                String formId = entry.getKey();
                JsonObject form = entry.getValue().getAsJsonObject();

                // Ensure the "command" key exists and is valid
                if (!form.has("command") || form.get("command").isJsonNull()) {
                    ServerForms.LOGGER.error("Form '{}' is missing the 'command' field or it is null. JSON: {}", formId, form);
                    continue;
                }

                String commandName = form.get("command").getAsString();

                // Register the command to start the form
                dispatcher.register(literal(commandName)
                        .executes(context -> FormHandler.startForm(context.getSource(), form)));

                ServerForms.LOGGER.info("Registered form command: /" + commandName);
            }

            // Register the command to handle answers
            dispatcher.register(literal("answer")
                    .then(argument("response", greedyString())
                            .executes(context -> {
                                String answer = getString(context, "response");
                                ServerCommandSource source = context.getSource();
                                FormHandler.handleAnswer(source, answer);
                                return 1;
                            })));

            // Register the /viewform command
            dispatcher.register(literal("viewform")
                    .then(argument("playername", word())
                            .suggests((context, builder) -> {
                                File[] files = FORM_ANSWERS_DIR.listFiles();
                                if (files != null) {
                                    for (File file : files) {
                                        if (file.isFile() && file.getName().endsWith(".json")) {
                                            try (FileReader reader = new FileReader(file)) {
                                                JsonObject data = ConfigLoader.GSON.fromJson(reader, JsonObject.class);
                                                if (data.has("playerName")) {
                                                    builder.suggest(data.get("playerName").getAsString());
                                                }
                                            } catch (IOException e) {
                                                ServerForms.LOGGER.error("Failed to read player name from file: " + file.getName(), e);
                                            }
                                        }
                                    }
                                }
                                return builder.buildFuture();
                            })
                            .executes(context -> {
                                String playerName = getString(context, "playername");
                                return viewForm(context.getSource(), playerName, null);
                            })
                            .then(argument("formname", greedyString())
                                    .suggests((context, builder) -> {
                                        String playerName = getString(context, "playername").toLowerCase();
                                        File[] files = FORM_ANSWERS_DIR.listFiles();
                                        if (files != null) {
                                            for (File file : files) {
                                                if (file.isFile() && file.getName().endsWith(".json")) {
                                                    try (FileReader reader = new FileReader(file)) {
                                                        JsonObject data = ConfigLoader.GSON.fromJson(reader, JsonObject.class);
                                                        if (data.has("playerName") && data.get("playerName").getAsString().equalsIgnoreCase(playerName)) {
                                                            for (String formName : data.keySet()) {
                                                                if (!formName.equals("playerName")) {
                                                                    builder.suggest(formName);
                                                                }
                                                            }
                                                        }
                                                    } catch (IOException e) {
                                                        ServerForms.LOGGER.error("Failed to read form names for player: " + playerName, e);
                                                    }
                                                }
                                            }
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String playerName = getString(context, "playername");
                                        String formName = getString(context, "formname");
                                        return viewForm(context.getSource(), playerName, formName);
                                    }))));

            // Register the /reloadforms command
            dispatcher.register(literal("reloadforms")
                    .requires(source -> source.hasPermissionLevel(4)) // Only allow OPs
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        return reloadForms(source);
                    }));
        });
    }

    /**
     * Utility method to register a command with the dispatcher.
     *
     * @param commandName The name of the command.
     * @param commandLogic A consumer that defines the command's logic.
     */
    public static void registerCommand(String commandName, Consumer<CommandDispatcher<ServerCommandSource>> commandLogic) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            commandLogic.accept(dispatcher);
            ServerForms.LOGGER.info("Registered command: /" + commandName);
        });
    }

    /**
     * Handles the /viewform command to view a player's form responses.
     *
     * @param source     The command source (e.g., the player or console executing the command).
     * @param playerName The name of the player whose form responses are being viewed.
     * @param formName   The name of the specific form to view (optional).
     * @return 1 if the command executed successfully, 0 otherwise.
     */
    private static int viewForm(ServerCommandSource source, String playerName, String formName) {
        UUID playerUUID = resolvePlayerUUID(source, playerName);
        if (playerUUID == null) return 0;

        File playerFile = new File(FORM_ANSWERS_DIR, playerUUID + ".json");
        if (!playerFile.exists()) {
            source.sendError(TextFormatter.formatColor("&cNo forms found for player: " + playerName));
            return 0;
        }

        try (FileReader reader = new FileReader(playerFile)) {
            JsonObject allForms = ConfigLoader.GSON.fromJson(reader, JsonObject.class);
            formName = resolveFormName(source, allForms, formName, playerName);
            if (formName == null) return 0;

            displayFormAnswers(source, allForms.getAsJsonObject(formName), formName, playerName);
            return 1;
        } catch (IOException e) {
            ServerForms.LOGGER.error("Failed to read form file for player: " + playerName, e);
            source.sendError(TextFormatter.formatColor("&cAn error occurred while reading the form file."));
            return 0;
        }
    }

    /**
     * Resolves the form name from the provided JSON object.
     *
     * @param source     The command source for sending feedback.
     * @param allForms   The JSON object containing all forms.
     * @param formName   The name of the form to resolve (can be null).
     * @param playerName The name of the player whose forms are being resolved.
     * @return The resolved form name, or null if not found.
     */
    private static String resolveFormName(ServerCommandSource source, JsonObject allForms, String formName, String playerName) {
        if (formName == null) {
            formName = allForms.keySet().stream().reduce((first, second) -> second).orElse(null);
        }
        if (formName == null || !allForms.has(formName)) {
            source.sendError(TextFormatter.formatColor("&cForm '" + formName + "' not found for player: " + playerName));
            return null;
        }
        return formName;
    }

    /**
     * Displays the answers of a specific form to the command source.
     *
     * @param source      The command source (e.g., the player or console executing the command).
     * @param formAnswers The JSON object containing the form answers.
     * @param formName    The name of the form being displayed.
     * @param playerName  The name of the player whose answers are being displayed.
     */
    private static void displayFormAnswers(ServerCommandSource source, JsonObject formAnswers, String formName, String playerName) {
        source.sendFeedback(() -> TextFormatter.formatColor("&aViewing form: " + formName + " for player: " + playerName), false);
        formAnswers.entrySet().forEach(entry -> {
            source.sendFeedback(() -> TextFormatter.formatColor("&b" + entry.getKey() + ": &f" + entry.getValue().getAsString()), false);
        });
    }

    /**
     * Resolves the UUID of a player by their name.
     *
     * @param source     The command source for sending feedback.
     * @param playerName The name of the player.
     * @return The UUID of the player, or null if not found.
     */
    private static UUID resolvePlayerUUID(ServerCommandSource source, String playerName) {
        return source.getServer().getUserCache().findByName(playerName)
                .map(profile -> profile.getId())
                .orElseGet(() -> {
                    source.sendError(TextFormatter.formatColor("&cPlayer '" + playerName + "' does not exist or has never joined the server."));
                    return null;
                });
    }

    /**
     * Handles the /reloadforms command to reload the configuration.
     *
     * @param source The command source (e.g., the player or console executing the command).
     * @return 1 if the configuration reloads successfully, 0 otherwise.
     */
    private static int reloadForms(ServerCommandSource source) {
        try {
            ConfigLoader.loadConfig();
            FormValidator.validateForms(ConfigLoader.getForms());
            source.sendFeedback(() -> TextFormatter.formatColor("&aForms configuration reloaded successfully!"), false);
            return 1;
        } catch (Exception e) {
            ServerForms.LOGGER.error("Failed to reload forms configuration.", e);
            source.sendError(TextFormatter.formatColor("&cFailed to reload forms configuration. Check the logs for details."));
            return 0;
        }
    }
}
