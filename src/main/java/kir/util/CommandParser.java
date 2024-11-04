package kir.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class CommandParser {

    public static Map.Entry<String, List<String>> parse(String command) {
        var tokens = new ArrayList<>();
        var matcher = Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(command);

        // Extract all tokens (command and arguments)
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                // Quoted argument without quotes
                tokens.add(matcher.group(1));
            } else {
                // Unquoted argument
                tokens.add(matcher.group(2));
            }
        }

        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Command cannot be empty.");
        }

        // The first token is the command; the rest are arguments
        var cmd = tokens.get(0).toString();
        var args = tokens.subList(1, tokens.size()).stream().map(Object::toString).toList();

        return new AbstractMap.SimpleEntry<>(cmd, args);
    }

}
