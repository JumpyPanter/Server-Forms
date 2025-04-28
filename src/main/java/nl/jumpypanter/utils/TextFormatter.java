package nl.jumpypanter.utils;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for formatting text with Minecraft color codes.
 */
public class TextFormatter {

    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&([0-9a-fk-or])");

    /**
     * Converts a string with color codes into a Minecraft Text object.
     *
     * @param input The string containing color codes.
     * @return A Text object with the appropriate color formatting.
     */
    public static Text formatColor(String input) {
        String formatted = input;
        Matcher matcher = COLOR_CODE_PATTERN.matcher(input);

        // Replace color codes with Minecraft's Formatting enums
        while (matcher.find()) {
            String code = matcher.group(1);
            Formatting formatting = getFormattingFromCode(code);
            if (formatting != null) {
                formatted = formatted.replace("&" + code, formatting.toString());
            }
        }

        // Return the formatted text as a Text object
        return Text.literal(formatted);
    }

    /**
     * Retrieves the corresponding Formatting enum based on a color code.
     *
     * @param code The color code (e.g., 'a' or 'b').
     * @return The associated Formatting enum, or null if the code is invalid.
     */
    private static Formatting getFormattingFromCode(String code) {
        switch (code.toLowerCase()) {
            case "0": return Formatting.BLACK;
            case "1": return Formatting.DARK_BLUE;
            case "2": return Formatting.DARK_GREEN;
            case "3": return Formatting.DARK_AQUA;
            case "4": return Formatting.DARK_RED;
            case "5": return Formatting.DARK_PURPLE;
            case "6": return Formatting.GOLD;
            case "7": return Formatting.GRAY;
            case "8": return Formatting.DARK_GRAY;
            case "9": return Formatting.BLUE;
            case "a": return Formatting.GREEN;
            case "b": return Formatting.AQUA;
            case "c": return Formatting.RED;
            case "d": return Formatting.LIGHT_PURPLE;
            case "e": return Formatting.YELLOW;
            case "f": return Formatting.WHITE;
            case "k": return Formatting.OBFUSCATED;
            case "l": return Formatting.BOLD;
            case "m": return Formatting.STRIKETHROUGH;
            case "n": return Formatting.UNDERLINE;
            case "o": return Formatting.ITALIC;
            case "r": return Formatting.RESET;
            default: return null;
        }
    }
}