package nl.jumpypanter.config;

import com.google.gson.*;
import nl.jumpypanter.ServerForms;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handles loading, saving, and managing the configuration for the form mod.
 * Provides access to forms and settings defined in the configuration file.
 */
public class ConfigLoader {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = new File("config");
    public static final File CONFIG_FILE = new File(CONFIG_DIR, "ServerForms.json");
    private static JsonObject config;

    private static final String FORMS_KEY = "forms";
    private static final String MESSAGES_KEY = "messages";

    /**
     * Loads the configuration file. If the file does not exist or is invalid,
     * a default configuration is generated.
     */
    public static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            ServerForms.LOGGER.warn("Config file not found. Generating default config...");
            generateDefaultConfig();
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            config = GSON.fromJson(reader, JsonObject.class);
            validateConfig();
            ServerForms.LOGGER.info("Config loaded successfully.");
        } catch (IOException e) {
            ServerForms.LOGGER.error("Failed to load config file: ", e);
            generateDefaultConfig();
        }
    }

    /**
     * Validates the loaded configuration and regenerates it if invalid.
     */
    private static void validateConfig() {
        if (config == null || !config.has(FORMS_KEY) || !config.get(FORMS_KEY).isJsonObject()) {
            ServerForms.LOGGER.error("Config file is missing the '{}' section. Regenerating default config...", FORMS_KEY);
            generateDefaultConfig();
        }
    }

    /**
     * Generates a default configuration file with predefined forms and settings.
     * This method is called if the configuration file is missing or invalid.
     */
    private static void generateDefaultConfig() {
        if (!CONFIG_DIR.exists() && !CONFIG_DIR.mkdirs()) {
            ServerForms.LOGGER.error("Failed to create configuration directory: {}", CONFIG_DIR.getAbsolutePath());
            return;
        }

        JsonObject defaultConfig = new JsonObject();
        JsonObject forms = new JsonObject();

        // Add predefined forms
        forms.add("single_response_form", createForm(
                "single_response_form", false, true, "single_response",
                new String[][]{
                        {"1", "What is your name?"},
                        {"2", "How old are you?"}
                }
        ));

        forms.add("multiple_responses_form", createForm(
                "multiple_responses_form", true, true, "multiple_responses",
                new String[][]{
                        {"1", "What is your favorite color?"},
                        {"2", "What is your favorite food?"},
                        {"3", "What is your favorite hobby?"}
                }
        ));

        defaultConfig.add(FORMS_KEY, forms);

        // Add default messages
        defaultConfig.add(MESSAGES_KEY, createDefaultMessages());

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(defaultConfig, writer);
            ServerForms.LOGGER.info("Default forms configuration generated at: {}", CONFIG_FILE.getAbsolutePath());
        } catch (IOException e) {
            ServerForms.LOGGER.error("Failed to generate default forms configuration: ", e);
        }

        config = defaultConfig;
    }

    /**
     * Creates a form with the specified properties and questions.
     *
     * @param name                  The name of the form.
     * @param allowMultipleResponses Whether the form allows multiple responses.
     * @param returnAnswers         Whether the form returns answers.
     * @param command               The command to trigger the form.
     * @param questions             An array of questions, where each question is a pair of [id, question text].
     * @return A JsonObject representing the form.
     */
    private static JsonObject createForm(String name, boolean allowMultipleResponses, boolean returnAnswers, String command, String[][] questions) {
        JsonObject form = new JsonObject();
        form.addProperty("name", name);
        form.addProperty("allowMultipleResponses", allowMultipleResponses);
        form.addProperty("returnAnswers", returnAnswers);
        form.addProperty("command", command);

        JsonArray questionArray = new JsonArray();
        for (String[] question : questions) {
            JsonObject questionObject = new JsonObject();
            questionObject.addProperty("id", question[0]);
            questionObject.addProperty("question", question[1]);
            questionArray.add(questionObject);
        }
        form.add("questions", questionArray);

        return form;
    }

    /**
     * Creates the default messages for the configuration.
     *
     * @return A JsonObject containing default messages.
     */
    private static JsonObject createDefaultMessages() {
        JsonObject messages = new JsonObject();
        messages.addProperty("formSuccess", "Thank you for completing the form!");
        messages.addProperty("formError", "An error occurred. Please try again.");
        messages.addProperty("playerNotFound", "&cPlayer '{player}' does not exist or has never joined the server.");
        messages.addProperty("formNotFound", "&cForm '{form}' not found for player: {player}.");
        messages.addProperty("viewingForm", "&aViewing form: {form} for player: {player}.");
        messages.addProperty("answerRecorded", "&aYour answer has been recorded.");
        messages.addProperty("noFormsFound", "&cNo forms found for player: {player}.");
        messages.addProperty("readError", "&cAn error occurred while reading the form file.");
        messages.addProperty("formExists", "&cA form with this name already exists.");
        messages.addProperty("formCreated", "&aForm '{form}' created successfully!");
        messages.addProperty("questionAdded", "&aQuestion added successfully to form '{form}'.");
        messages.addProperty("questionRemoved", "&aQuestion removed successfully from form '{form}'.");
        messages.addProperty("questionExists", "&cA question with ID '{id}' already exists.");
        messages.addProperty("questionNotFound", "&cNo question with ID '{id}' found in form '{form}'.");
        messages.addProperty("saveError", "&cAn error occurred while saving the configuration.");
        return messages;
    }

    /**
     * Retrieves the entire configuration as a JsonObject.
     *
     * @return The configuration JsonObject.
     */
    public static JsonObject getConfig() {
        return config;
    }

    /**
     * Retrieves the forms section of the configuration.
     *
     * @return A JsonObject containing all forms, or an empty JsonObject if missing.
     */
    public static JsonObject getForms() {
        return config != null && config.has(FORMS_KEY) && config.get(FORMS_KEY).isJsonObject()
                ? config.getAsJsonObject(FORMS_KEY)
                : new JsonObject();
    }

    /**
     * Retrieves a message from the configuration by its key.
     *
     * @param key            The key of the message to retrieve.
     * @param defaultMessage The default message to return if the key is not found.
     * @return The message corresponding to the key, or the default message if not found.
     */
    public static String getMessage(String key, String defaultMessage) {
        JsonObject messages = config != null && config.has(MESSAGES_KEY) && config.get(MESSAGES_KEY).isJsonObject()
                ? config.getAsJsonObject(MESSAGES_KEY)
                : new JsonObject();
        return messages.has(key) ? messages.get(key).getAsString() : defaultMessage;
    }
}
