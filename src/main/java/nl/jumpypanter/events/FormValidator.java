package nl.jumpypanter.events;

import com.google.gson.JsonObject;
import nl.jumpypanter.ServerForms;

/**
 * Validates the structure and content of forms in the configuration.
 * Ensures that all required fields are present and correctly formatted.
 */
public class FormValidator {

    /**
     * Validates the provided forms configuration.
     *
     * @param forms The JSON object containing all forms to validate.
     * @throws IllegalArgumentException if any form is invalid or missing required fields.
     */
    public static void validateForms(JsonObject forms) {
        if (forms == null) {
            ServerForms.LOGGER.error("The 'forms' configuration is null. Please check your forms_config.json file.");
            throw new IllegalArgumentException("The 'forms' configuration is null. Ensure forms_config.json is properly configured.");
        }

        for (String key : forms.keySet()) {
            if (!forms.has(key) || forms.get(key).isJsonNull()) {
                ServerForms.LOGGER.error("Form '{}' is null or missing. JSON: {}", key, forms);
                throw new IllegalArgumentException("Form '" + key + "' is null or missing.");
            }

            JsonObject form = forms.getAsJsonObject(key);

            validateFormName(key, form);
            validateAllowMultipleResponses(key, form);
            validateQuestions(key, form);
        }
    }

    /**
     * Validates the 'name' field of a form.
     *
     * @param key  The key of the form being validated.
     * @param form The JSON object representing the form.
     * @throws IllegalArgumentException if the 'name' field is missing or invalid.
     */
    private static void validateFormName(String key, JsonObject form) {
        if (!form.has("name") || form.get("name").isJsonNull()) {
            ServerForms.LOGGER.error("Form '{}' is missing the 'name' field. JSON: {}", key, form);
            throw new IllegalArgumentException("Form '" + key + "' is missing the 'name' field.");
        }
        if (!form.get("name").isJsonPrimitive() || !form.get("name").getAsJsonPrimitive().isString()) {
            ServerForms.LOGGER.error("The 'name' field in form '{}' must be a string. JSON: {}", key, form);
            throw new IllegalArgumentException("The 'name' field in form '" + key + "' must be a string.");
        }
    }

    /**
     * Validates the 'allowMultipleResponses' field of a form.
     *
     * @param key  The key of the form being validated.
     * @param form The JSON object representing the form.
     * @throws IllegalArgumentException if the 'allowMultipleResponses' field is invalid.
     */
    private static void validateAllowMultipleResponses(String key, JsonObject form) {
        if (form.has("allowMultipleResponses") && !form.get("allowMultipleResponses").isJsonPrimitive()) {
            ServerForms.LOGGER.error("The 'allowMultipleResponses' field in form '{}' must be a boolean. JSON: {}", key, form);
            throw new IllegalArgumentException("The 'allowMultipleResponses' field in form '" + key + "' must be a boolean.");
        }
        if (form.has("allowMultipleResponses") && !form.get("allowMultipleResponses").getAsJsonPrimitive().isBoolean()) {
            ServerForms.LOGGER.error("The 'allowMultipleResponses' field in form '{}' must be a boolean. JSON: {}", key, form);
            throw new IllegalArgumentException("The 'allowMultipleResponses' field in form '" + key + "' must be a boolean.");
        }
    }

    /**
     * Validates the 'questions' field of a form.
     *
     * @param key  The key of the form being validated.
     * @param form The JSON object representing the form.
     * @throws IllegalArgumentException if the 'questions' field is missing or invalid.
     */
    private static void validateQuestions(String key, JsonObject form) {
        if (!form.has("questions") || form.get("questions").isJsonNull()) {
            ServerForms.LOGGER.error("Form '{}' is missing the 'questions' field. JSON: {}", key, form);
            throw new IllegalArgumentException("Form '" + key + "' is missing the 'questions' field.");
        }
        if (!form.get("questions").isJsonArray()) {
            ServerForms.LOGGER.error("The 'questions' field in form '{}' must be a JSON array. JSON: {}", key, form);
            throw new IllegalArgumentException("The 'questions' field in form '" + key + "' must be a JSON array.");
        }
    }
}
