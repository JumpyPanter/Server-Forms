package nl.jumpypanter.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a session for a player interacting with a form.
 * Tracks the player's progress, answers, and the current question.
 */
public class PlayerFormSession {
    private final JsonObject form;
    private final String playerName;
    private final Map<String, String> answers = new HashMap<>();
    private int currentQuestionIndex = 0;

    /**
     * Constructs a new PlayerFormSession.
     *
     * @param playerName The name of the player.
     * @param form       The form being interacted with.
     */
    public PlayerFormSession(String playerName, JsonObject form) {
        this.playerName = playerName;
        this.form = form;
    }

    /**
     * Checks if there are more questions in the form.
     *
     * @return true if there are more questions, false otherwise.
     */
    public boolean hasNextQuestion() {
        JsonArray questions = getQuestions();
        return questions != null && currentQuestionIndex < questions.size();
    }

    /**
     * Retrieves the current question in the form.
     *
     * @return The current question as a JsonObject.
     * @throws IllegalStateException if there is no current question available.
     */
    public JsonObject getCurrentQuestion() {
        JsonArray questions = getQuestions();
        if (questions == null || currentQuestionIndex >= questions.size()) {
            throw new IllegalStateException("No current question available. Ensure hasNextQuestion() is true before calling this method.");
        }
        return questions.get(currentQuestionIndex).getAsJsonObject();
    }

    /**
     * Records the player's answer to the current question and moves to the next question.
     *
     * @param questionId The ID of the question being answered.
     * @param answer     The player's answer.
     */
    public void recordAnswer(String questionId, String answer) {
        answers.put(questionId, answer);
        currentQuestionIndex++;
    }

    /**
     * Retrieves all answers recorded in the session.
     *
     * @return A map of question IDs to answers.
     */
    public Map<String, String> getAnswers() {
        return answers;
    }

    /**
     * Retrieves the name of the player associated with this session.
     *
     * @return The player's name.
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Retrieves the name of the form being interacted with.
     *
     * @return The form name, or "unknown_form" if the name is not specified.
     */
    public String getFormName() {
        return form.has("name") && !form.get("name").isJsonNull() ? form.get("name").getAsString() : "unknown_form";
    }

    /**
     * Retrieves the list of questions in the form.
     *
     * @return A JsonArray of questions, or an empty JsonArray if none are defined.
     */
    private JsonArray getQuestions() {
        return form.has("questions") && !form.get("questions").isJsonNull() ? form.getAsJsonArray("questions") : new JsonArray();
    }
}