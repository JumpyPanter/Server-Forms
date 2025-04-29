package nl.jumpypanter.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.server.command.ServerCommandSource;
import nl.jumpypanter.config.ConfigLoader;
import nl.jumpypanter.ServerForms;
import nl.jumpypanter.utils.TextFormatter;

import java.io.FileWriter;
import java.io.IOException;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Handles all commands related to forms, including creating forms, adding questions, and removing questions.
 */
public class FormCommandHandler {

    /**
     * Provides suggestions for form names based on existing forms in the configuration.
     */
    private static final SuggestionProvider<ServerCommandSource> FORM_SUGGESTIONS = (context, builder) -> {
        JsonObject forms = ConfigLoader.getForms();
        if (forms != null) {
            forms.keySet().forEach(builder::suggest);
        }
        return builder.buildFuture();
    };

    /**
     * Registers all form-related commands.
     */
    public static void registerCommands() {
        CommandRegistry.registerCommand("createform", dispatcher -> {
            dispatcher.register(
                    literal("createform")
                            .requires(source -> source.hasPermissionLevel(4)) // Only allow OPs
                            .then(argument("formName", StringArgumentType.string())
                                    .then(argument("allowMultipleResponses", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                            .executes(context -> {
                                                String formName = StringArgumentType.getString(context, "formName");
                                                boolean allowMultipleResponses = com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "allowMultipleResponses");
                                                return createForm(context.getSource(), formName, allowMultipleResponses);
                                            })))
            );
        });

        CommandRegistry.registerCommand("addquestion", dispatcher -> {
            dispatcher.register(
                    literal("addquestion")
                            .requires(source -> source.hasPermissionLevel(4)) // Only allow OPs
                            .then(argument("formName", StringArgumentType.string())
                                    .suggests(FORM_SUGGESTIONS)
                                    .then(argument("questionId", StringArgumentType.string())
                                            .then(argument("questionText", StringArgumentType.greedyString())
                                                    .executes(context -> {
                                                        String formName = StringArgumentType.getString(context, "formName");
                                                        String questionId = StringArgumentType.getString(context, "questionId");
                                                        String questionText = StringArgumentType.getString(context, "questionText");
                                                        return addQuestion(context.getSource(), formName, questionId, questionText);
                                                    }))))
            );
        });

        CommandRegistry.registerCommand("removequestion", dispatcher -> {
            dispatcher.register(
                    literal("removequestion")
                            .requires(source -> source.hasPermissionLevel(4)) // Only allow OPs
                            .then(argument("formName", StringArgumentType.string())
                                    .suggests(FORM_SUGGESTIONS)
                                    .then(argument("questionId", StringArgumentType.string())
                                            .executes(context -> {
                                                String formName = StringArgumentType.getString(context, "formName");
                                                String questionId = StringArgumentType.getString(context, "questionId");
                                                return removeQuestion(context.getSource(), formName, questionId);
                                            })))
            );
        });
    }

    /**
     * Adds a question to the specified form.
     *
     * @param source       The command source.
     * @param formName     The name of the form.
     * @param questionId   The ID of the question.
     * @param questionText The text of the question.
     * @return 1 if the question was added successfully, 0 otherwise.
     */
    private static int addQuestion(ServerCommandSource source, String formName, String questionId, String questionText) {
        JsonObject forms = ConfigLoader.getForms();

        if (forms == null || !forms.has(formName)) {
            source.sendError(TextFormatter.formatColor(ConfigLoader.getMessage("formNotFound", "&cForm '{form}' does not exist.")
                    .replace("{form}", formName)));
            return 0;
        }

        JsonObject form = forms.getAsJsonObject(formName);
        JsonArray questions = form.getAsJsonArray("questions");

        for (int i = 0; i < questions.size(); i++) {
            JsonObject question = questions.get(i).getAsJsonObject();
            if (question.get("id").getAsString().equals(questionId)) {
                source.sendError(TextFormatter.formatColor(ConfigLoader.getMessage("questionExists", "&cA question with ID '{id}' already exists.")
                        .replace("{id}", questionId)));
                return 0;
            }
        }

        JsonObject newQuestion = new JsonObject();
        newQuestion.addProperty("id", questionId);
        newQuestion.addProperty("question", questionText);
        questions.add(newQuestion);

        saveConfig(source, ConfigLoader.getMessage("questionAdded", "&aQuestion added successfully to form '{form}'.")
                .replace("{form}", formName));
        return 1;
    }

    /**
     * Removes a question from the specified form.
     *
     * @param source     The command source.
     * @param formName   The name of the form.
     * @param questionId The ID of the question to remove.
     * @return 1 if the question was removed successfully, 0 otherwise.
     */
    private static int removeQuestion(ServerCommandSource source, String formName, String questionId) {
        JsonObject forms = ConfigLoader.getForms();

        if (forms == null || !forms.has(formName)) {
            source.sendError(TextFormatter.formatColor(ConfigLoader.getMessage("formNotFound", "&cForm '{form}' does not exist.")
                    .replace("{form}", formName)));
            return 0;
        }

        JsonObject form = forms.getAsJsonObject(formName);
        JsonArray questions = form.getAsJsonArray("questions");

        for (int i = 0; i < questions.size(); i++) {
            JsonObject question = questions.get(i).getAsJsonObject();
            if (question.get("id").getAsString().equals(questionId)) {
                questions.remove(i);
                saveConfig(source, ConfigLoader.getMessage("questionRemoved", "&aQuestion removed successfully from form '{form}'.")
                        .replace("{form}", formName));
                return 1;
            }
        }

        source.sendError(TextFormatter.formatColor(ConfigLoader.getMessage("questionNotFound", "&cNo question with ID '{id}' found in form '{form}'.")
                .replace("{id}", questionId)
                .replace("{form}", formName)));
        return 0;
    }

    /**
     * Saves the configuration to the file system.
     *
     * @param source        The command source.
     * @param successMessage The success message to display.
     */
    private static void saveConfig(ServerCommandSource source, String successMessage) {
        try (FileWriter writer = new FileWriter(ConfigLoader.CONFIG_FILE)) {
            ConfigLoader.GSON.toJson(ConfigLoader.getConfig(), writer);
            source.sendFeedback(() -> TextFormatter.formatColor(successMessage), false);
        } catch (IOException e) {
            ServerForms.LOGGER.error("Failed to save the updated configuration file.", e);
            source.sendError(TextFormatter.formatColor(ConfigLoader.getMessage("saveError", "&cAn error occurred while saving the configuration.")));
        }
    }

    /**
     * Creates a new form with the specified name and settings.
     *
     * @param source                The command source.
     * @param formName              The name of the form.
     * @param allowMultipleResponses Whether the form allows multiple responses.
     * @return 1 if the form was created successfully, 0 otherwise.
     */
    private static int createForm(ServerCommandSource source, String formName, boolean allowMultipleResponses) {
        JsonObject forms = ConfigLoader.getForms();

        if (forms == null || forms.has(formName)) {
            source.sendError(TextFormatter.formatColor(ConfigLoader.getMessage("formExists", "&cA form with this name already exists.")));
            return 0;
        }

        JsonObject newForm = new JsonObject();
        newForm.addProperty("name", formName);
        newForm.addProperty("allowMultipleResponses", allowMultipleResponses);
        newForm.addProperty("returnAnswers", true); // Default value
        newForm.addProperty("command", formName); // Set the command to the form name
        newForm.add("questions", new JsonArray());

        forms.add(formName, newForm);

        saveConfig(source, ConfigLoader.getMessage("formCreated", "&aForm '{form}' created successfully!")
                .replace("{form}", formName));
        return 1;
    }
}