package nl.jumpypanter.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import nl.jumpypanter.config.ConfigLoader;
import nl.jumpypanter.events.FormHandler;
import nl.jumpypanter.ServerForms;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import nl.jumpypanter.utils.TextFormatter;

/**
 * Handles the registration of commands for the form mod.
 * Includes commands for starting forms, answering forms, and viewing form responses.
 */
public class CommandRegistry {
    private static final File FORM_ANSWERS_DIR = new File("mods", "FormAnswers"); // Reverted to match the rest of the codebase

    /**
     * Registers all commands for the form mod.
     * This includes commands for starting forms, answering forms, and viewing form responses.
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
                    continue; // Skip this form if the "command" field is invalid
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
                    .then(argument("playername", word()) // Suggest player names
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
                            .then(argument("formname", greedyString()) // Suggest form names
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
        });
    }

    /**
     * Handles the /viewform command to view a player's form responses.
     *
     * @param source    The command source (e.g., the player or console executing the command).
     * @param playerName The name of the player whose form responses are being viewed.
     * @param formName   The name of the specific form to view (optional).
     * @return 1 if the command executed successfully, 0 otherwise.
     */
    private static int viewForm(ServerCommandSource source, String playerName, String formName) {
        // Resolve the player's UUID from the player name
        UUID playerUUID = source.getServer().getUserCache().findByName(playerName)
                .map(profile -> profile.getId())
                .orElse(null);

        if (playerUUID == null) {
            source.sendError(TextFormatter.formatColor("&cPlayer '" + playerName + "' does not exist or has never joined the server."));
            return 0;
        }

        // Locate the file using the UUID
        File playerFile = new File(FORM_ANSWERS_DIR, playerUUID.toString() + ".json");

        if (!playerFile.exists()) {
            source.sendError(TextFormatter.formatColor("&cNo forms found for player: " + playerName));
            return 0;
        }

        try (FileReader reader = new FileReader(playerFile)) {
            JsonObject allForms = ConfigLoader.GSON.fromJson(reader, JsonObject.class);

            if (formName == null) {
                // Get the latest form if no formName is provided
                formName = allForms.keySet().stream().reduce((first, second) -> second).orElse(null);
            }

            if (formName == null || !allForms.has(formName)) {
                source.sendError(TextFormatter.formatColor("&cForm '" + formName + "' not found for player: " + playerName));
                return 0;
            }

            JsonObject formAnswers = allForms.getAsJsonObject(formName);
            String finalFormName = formName;
            source.sendFeedback(() -> TextFormatter.formatColor("&aViewing form: " + finalFormName + " for player: " + playerName), false);

            formAnswers.entrySet().forEach(entry -> {
                String questionId = entry.getKey();
                String answer = entry.getValue().getAsString();
                source.sendFeedback(() -> TextFormatter.formatColor("&b" + questionId + ": &f" + answer), false);
            });

            return 1;
        } catch (IOException e) {
            ServerForms.LOGGER.error("Failed to read form file for player: " + playerName, e);
            source.sendError(TextFormatter.formatColor("&cAn error occurred while reading the form file."));
            return 0;
        }
    }
}