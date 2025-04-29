package nl.jumpypanter.utils;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for formatting text with Minecraft color codes.
 */
public class TextFormatter {

    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("&([0-9a-fk-or])");
    private static final Map<String, Formatting> FORMATTING_MAP = Map.ofEntries(
        Map.entry("0", Formatting.BLACK),
        Map.entry("1", Formatting.DARK_BLUE),
        Map.entry("2", Formatting.DARK_GREEN),
        Map.entry("3", Formatting.DARK_AQUA),
        Map.entry("4", Formatting.DARK_RED),
        Map.entry("5", Formatting.DARK_PURPLE),
        Map.entry("6", Formatting.GOLD),
        Map.entry("7", Formatting.GRAY),
        Map.entry("8", Formatting.DARK_GRAY),
        Map.entry("9", Formatting.BLUE),
        Map.entry("a", Formatting.GREEN),
        Map.entry("b", Formatting.AQUA),
        Map.entry("c", Formatting.RED),
        Map.entry("d", Formatting.LIGHT_PURPLE),
        Map.entry("e", Formatting.YELLOW),
        Map.entry("f", Formatting.WHITE),
        Map.entry("k", Formatting.OBFUSCATED),
        Map.entry("l", Formatting.BOLD),
        Map.entry("m", Formatting.STRIKETHROUGH),
        Map.entry("n", Formatting.UNDERLINE),
        Map.entry("o", Formatting.ITALIC),
        Map.entry("r", Formatting.RESET)
    );

    public static Text formatColor(String input) {
        String formatted = input;
        Matcher matcher = COLOR_CODE_PATTERN.matcher(input);

        while (matcher.find()) {
            String code = matcher.group(1).toLowerCase();
            Formatting formatting = FORMATTING_MAP.get(code);
            if (formatting != null) {
                formatted = formatted.replace("&" + code, formatting.toString());
            }
        }

        return Text.literal(formatted);
    }
}
