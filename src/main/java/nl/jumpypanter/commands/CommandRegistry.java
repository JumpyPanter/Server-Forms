package nl.jumpypanter.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import nl.jumpypanter.ServerForms;
import nl.jumpypanter.config.ConfigLoader;
import nl.jumpypanter.events.FormHandler;
import nl.jumpypanter.utils.TextFormatter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandRegistry {
    private static final File FORM_ANSWERS_DIR = new File("mods", "FormAnswers");

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            JsonObject forms = ConfigLoader.getForms();
            for (Map.Entry<String, JsonElement> entry : forms.entrySet()) {
                String formId = entry.getKey();
                JsonObject form = entry.getValue().getAsJsonObject();

                if (!form.has("command") || form.get("command").isJsonNull()) {
                    ServerForms.LOGGER.error("Form '{}' is missing the 'command' field or it is null. JSON: {}", formId, form);
                    continue;
                }

                String commandName = form.get("command").getAsString();

                dispatcher.register(literal(commandName)
                        .executes(context -> FormHandler.startForm(context.getSource(), form)));

                ServerForms.LOGGER.info("Registered form command: /" + commandName);
            }

            dispatcher.register(literal("answer")
                    .then(argument("response", StringArgumentType.greedyString())
                            .executes(context -> {
                                String answer = getString(context, "response");
                                ServerCommandSource source = context.getSource();
                                FormHandler.handleAnswer(source, answer);
                                return 1;
                            })));

            dispatcher.register(literal("viewform")
                    .then(argument("playername", StringArgumentType.word())
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
                            .then(argument("formname", StringArgumentType.greedyString())
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

    private static int viewForm(ServerCommandSource source, String playerName, String formName) {
        UUID playerUUID = source.getServer().getUserCache().findByName(playerName)
                .map(profile -> profile.getId())
                .orElse(null);

        if (playerUUID == null) {
            source.sendError(TextFormatter.formatColor(
                    ConfigLoader.getMessage("playerNotFound", "&cPlayer '{player}' does not exist or has never joined the server.")
                            .replace("{player}", playerName)
            ));
            return 0;
        }

        File playerFile = new File(FORM_ANSWERS_DIR, playerUUID.toString() + ".json");

        if (!playerFile.exists()) {
            source.sendError(TextFormatter.formatColor(
                    ConfigLoader.getMessage("noFormsFound", "&cNo forms found for player: {player}.")
                            .replace("{player}", playerName)
            ));
            return 0;
        }

        try (FileReader reader = new FileReader(playerFile)) {
            JsonObject allForms = ConfigLoader.GSON.fromJson(reader, JsonObject.class);

            if (formName == null) {
                formName = allForms.keySet().stream().reduce((first, second) -> second).orElse(null);
            }

            if (formName == null || !allForms.has(formName)) {
                source.sendError(TextFormatter.formatColor(
                        ConfigLoader.getMessage("formNotFound", "&cForm '{form}' not found for player: {player}.")
                                .replace("{form}", formName)
                                .replace("{player}", playerName)
                ));
                return 0;
            }

            JsonObject formAnswers = allForms.getAsJsonObject(formName);
            String finalFormName = formName;
            source.sendFeedback(() -> TextFormatter.formatColor(
                    ConfigLoader.getMessage("viewingForm", "&aViewing form: {form} for player: {player}.")
                            .replace("{form}", finalFormName)
                            .replace("{player}", playerName)
            ), false);

            formAnswers.entrySet().forEach(entry -> {
                String questionId = entry.getKey();
                String answer = entry.getValue().getAsString();
                source.sendFeedback(() -> TextFormatter.formatColor("&b" + questionId + ": &f" + answer), false);
            });

            return 1;
        } catch (IOException e) {
            ServerForms.LOGGER.error("Failed to read form file for player: " + playerName, e);
            source.sendError(TextFormatter.formatColor(
                    ConfigLoader.getMessage("readError", "&cAn error occurred while reading the form file.")
            ));
            return 0;
        }
    }
}