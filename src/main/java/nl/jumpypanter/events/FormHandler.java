package nl.jumpypanter.events;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.command.ServerCommandSource;
import nl.jumpypanter.ServerForms;
import nl.jumpypanter.commands.PlayerFormSession;
import nl.jumpypanter.config.ConfigLoader;
import nl.jumpypanter.utils.TextFormatter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the logic for managing forms, including starting forms, handling answers,
 * and saving responses to files.
 */
public class FormHandler {
    private static final Map<String, PlayerFormSession> activeSessions = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FORM_ANSWERS_DIR = new File("mods", "FormAnswers");

    static {
        // Ensure the directory for form answers exists
        if (!FORM_ANSWERS_DIR.exists()) {
            FORM_ANSWERS_DIR.mkdirs();
        }
    }

    /**
     * Starts a new form session for the player.
     *
     * @param source The command source (e.g., the player or console executing the command).
     * @param form   The form to be started.
     * @return 1 if the form starts successfully, 0 otherwise.
     */
    public static int startForm(ServerCommandSource source, JsonObject form) {
        String playerName = source.getName();
        UUID playerUUID = source.getPlayer().getUuid();

        if (!form.has("name") || form.get("name").isJsonNull()) {
            source.sendError(TextFormatter.formatColor("&cThe form is missing a 'name' field."));
            ServerForms.LOGGER.error("The form JSON is missing a 'name' field: {}", form.toString());
            return 0;
        }
        String formName = form.get("name").getAsString();

        boolean allowMultipleResponses = form.has("allowMultipleResponses") && form.get("allowMultipleResponses").getAsBoolean();

        // Check if the player has already completed this form
        if (!allowMultipleResponses && hasExistingResponse(playerUUID, formName)) {
            source.sendError(TextFormatter.formatColor("&cYou have already completed this form!"));
            return 0;
        }

        // Check if the player is already filling out a form
        if (activeSessions.containsKey(playerName)) {
            source.sendError(TextFormatter.formatColor("&cYou are already filling out a form!"));
            return 0;
        }

        // Start a new session
        PlayerFormSession session = new PlayerFormSession(playerName, form);
        activeSessions.put(playerName, session);

        // Display the first question
        askNextQuestion(source, session);
        return 1;
    }

    /**
     * Handles the player's answer to the current question.
     *
     * @param source The command source (e.g., the player or console executing the command).
     * @param answer The player's answer.
     */
    public static void handleAnswer(ServerCommandSource source, String answer) {
        String playerName = source.getName();

        // Check if the player has an active session
        if (!activeSessions.containsKey(playerName)) {
            source.sendError(TextFormatter.formatColor("&cYou are not currently filling out a form."));
            return;
        }

        PlayerFormSession session = activeSessions.get(playerName);

        // Record the answer
        JsonObject currentQuestion = session.getCurrentQuestion();
        if (!currentQuestion.has("id") || currentQuestion.get("id").isJsonNull()) {
            source.sendError(TextFormatter.formatColor("&cThe current question is missing an 'id' field."));
            return;
        }
        String questionId = currentQuestion.get("id").getAsString();
        session.recordAnswer(questionId, answer);

        // Display the next question or end the form
        if (session.hasNextQuestion()) {
            askNextQuestion(source, session);
        } else {
            endForm(source, session);
        }
    }

    /**
     * Displays the next question in the form to the player.
     *
     * @param source  The command source (e.g., the player or console executing the command).
     * @param session The player's form session.
     */
    private static void askNextQuestion(ServerCommandSource source, PlayerFormSession session) {
        JsonObject question = session.getCurrentQuestion();
        String questionText = question.get("question").getAsString();
        source.sendFeedback(() -> TextFormatter.formatColor("&eNext question: &f" + questionText), false);
    }

    /**
     * Ends the form session and saves the player's answers.
     *
     * @param source  The command source (e.g., the player or console executing the command).
     * @param session The player's form session.
     */
    private static void endForm(ServerCommandSource source, PlayerFormSession session) {
        String playerName = session.getPlayerName();
        UUID playerUUID = source.getPlayer().getUuid();
        String formName = session.getFormName();

        activeSessions.remove(playerName);

        // Save the answers to a file
        saveAnswersToFile(source, playerUUID, formName, session.getAnswers());

        // Retrieve the formSuccess message from the config
        String formSuccessMessage = ConfigLoader.getMessage("formSuccess", "&aForm completed!");

        // Send the formSuccess message
        source.sendFeedback(() -> TextFormatter.formatColor(formSuccessMessage), false);

        // Check if returning answers is enabled for this form
        JsonObject formConfig = ConfigLoader.getForms().getAsJsonObject(formName);
        boolean returnAnswers = formConfig.has("returnAnswers") && formConfig.get("returnAnswers").getAsBoolean();

        if (returnAnswers) {
            // Display the answers to the player
            session.getAnswers().forEach((questionId, answer) -> {
                source.sendFeedback(() -> TextFormatter.formatColor("&b" + questionId + ": &f" + answer), false);
            });
        }
    }

    /**
     * Saves the player's answers to a file.
     *
     * @param playerUUID The UUID of the player.
     * @param formName   The name of the form.
     * @param answers    The player's answers.
     */
    private static void saveAnswersToFile(ServerCommandSource source, UUID playerUUID, String formName, Map<String, String> answers) {
        File answersFile = new File(FORM_ANSWERS_DIR, playerUUID.toString() + ".json");

        JsonObject allForms;

        // Read existing data from the file (if present)
        if (answersFile.exists()) {
            try (FileReader reader = new FileReader(answersFile)) {
                allForms = GSON.fromJson(reader, JsonObject.class);
            } catch (IOException e) {
                ServerForms.LOGGER.error("Failed to read answers file for player UUID " + playerUUID, e);
                allForms = new JsonObject();
            }
        } else {
            allForms = new JsonObject();
        }

        // Add the player's name to the file
        allForms.addProperty("playerName", source.getName());

        // Add the current form's answers
        JsonObject formAnswers = new JsonObject();
        answers.forEach(formAnswers::addProperty);
        allForms.add(formName, formAnswers);

        // Write the updated data back to the file
        try (FileWriter writer = new FileWriter(answersFile)) {
            GSON.toJson(allForms, writer);
            ServerForms.LOGGER.info("Saved answers for player UUID {} to {}", playerUUID, answersFile.getAbsolutePath());
        } catch (IOException e) {
            ServerForms.LOGGER.error("Failed to save answers for player UUID " + playerUUID, e);
        }
    }

    /**
     * Checks if the player has already completed the specified form.
     *
     * @param playerUUID The UUID of the player.
     * @param formName   The name of the form.
     * @return true if the player has already completed the form, false otherwise.
     */
    private static boolean hasExistingResponse(UUID playerUUID, String formName) {
        File answersFile = new File(FORM_ANSWERS_DIR, playerUUID.toString() + ".json");

        if (!answersFile.exists()) {
            return false;
        }

        // Check if the form already exists in the file
        try (FileReader reader = new FileReader(answersFile)) {
            JsonObject allForms = GSON.fromJson(reader, JsonObject.class);
            return allForms.has(formName);
        } catch (IOException e) {
            ServerForms.LOGGER.error("Failed to check existing responses for player UUID " + playerUUID, e);
            return false;
        }
    }
}