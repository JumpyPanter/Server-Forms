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
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "ServerForms.json");
    private static JsonObject config;

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

            if (config == null || !config.has("forms") || !config.get("forms").isJsonObject()) {
                ServerForms.LOGGER.error("Config file is missing the 'forms' section. Regenerating default config...");
                generateDefaultConfig();
            } else {
                ServerForms.LOGGER.info("Config loaded successfully.");
            }
        } catch (IOException e) {
            ServerForms.LOGGER.error("Failed to load config file: ", e);
            generateDefaultConfig();
        }
    }

    /**
     * Generates a default configuration file with predefined forms and settings.
     * This method is called if the configuration file is missing or invalid.
     */
    private static void generateDefaultConfig() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }

        JsonObject defaultConfig = new JsonObject();

        JsonObject forms = new JsonObject();

        // Define a single-response form
        JsonObject singleResponseForm = new JsonObject();
        singleResponseForm.addProperty("name", "single_response_form");
        singleResponseForm.addProperty("allowMultipleResponses", false);
        singleResponseForm.addProperty("command", "single_response");

        JsonArray singleResponseQuestions = new JsonArray();

        JsonObject question1 = new JsonObject();
        question1.addProperty("id", "1");
        question1.addProperty("question", "What is your name?");
        singleResponseQuestions.add(question1);

        JsonObject question2 = new JsonObject();
        question2.addProperty("id", "2");
        question2.addProperty("question", "How old are you?");
        singleResponseQuestions.add(question2);

        singleResponseForm.add("questions", singleResponseQuestions);

        // Define a multiple-responses form
        JsonObject multipleResponsesForm = new JsonObject();
        multipleResponsesForm.addProperty("name", "multiple_responses_form");
        multipleResponsesForm.addProperty("allowMultipleResponses", true);
        multipleResponsesForm.addProperty("command", "multiple_responses");

        JsonArray multipleResponseQuestions = new JsonArray();

        JsonObject question3 = new JsonObject();
        question3.addProperty("id", "1");
        question3.addProperty("question", "What is your favorite color?");
        multipleResponseQuestions.add(question3);

        JsonObject question4 = new JsonObject();
        question4.addProperty("id", "2");
        question4.addProperty("question", "What is your favorite food?");
        multipleResponseQuestions.add(question4);

        JsonObject question5 = new JsonObject();
        question5.addProperty("id", "3");
        question5.addProperty("question", "What is your favorite hobby?");
        multipleResponseQuestions.add(question5);

        multipleResponsesForm.add("questions", multipleResponseQuestions);

        forms.add("single_response_form", singleResponseForm);
        forms.add("multiple_responses_form", multipleResponsesForm);

        defaultConfig.add("forms", forms);

        // Add default messages
        JsonObject messages = new JsonObject();
        messages.addProperty("formSuccess", "Thank you for completing the form!");
        messages.addProperty("formError", "An error occurred. Please try again.");
        messages.addProperty("playerNotFound", "&cPlayer '{player}' does not exist or has never joined the server.");
        messages.addProperty("formNotFound", "&cForm '{form}' not found for player: {player}.");
        messages.addProperty("viewingForm", "&aViewing form: {form} for player: {player}.");
        messages.addProperty("answerRecorded", "&aYour answer has been recorded.");
        messages.addProperty("noFormsFound", "&cNo forms found for player: {player}.");
        messages.addProperty("readError", "&cAn error occurred while reading the form file.");

        defaultConfig.add("messages", messages);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(defaultConfig, writer);
            ServerForms.LOGGER.info("Default forms configuration generated at: " + CONFIG_FILE.getAbsolutePath());
        } catch (IOException e) {
            ServerForms.LOGGER.error("Failed to generate default forms configuration: ", e);
        }

        config = defaultConfig;
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
        if (config == null || !config.has("forms") || config.get("forms").isJsonNull()) {
            ServerForms.LOGGER.error("The 'forms' section is missing in the configuration.");
            return new JsonObject();
        }
        return config.getAsJsonObject("forms");
    }

    /**
     * Retrieves a specific message from the configuration.
     *
     * @param key          The key of the message.
     * @param defaultValue The default value to return if the key is not found.
     * @return The message as a String.
     */
    public static String getMessage(String key, String defaultValue) {
        if (config != null && config.has("messages")) {
            JsonObject messages = config.getAsJsonObject("messages");
            return messages.has(key) ? messages.get(key).getAsString() : defaultValue;
        }
        return defaultValue;
    }
}