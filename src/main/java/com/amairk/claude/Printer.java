package com.amairk.claude;

import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.Usage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/** Pretty console output so each request/response is easy to read. */
public final class Printer {

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String RED = "\u001B[31m";

    private static final String LINE = "─".repeat(78);

    private Printer() {}

    /** Big banner at the start of an example. */
    public static void title(String title, String whatItShows) {
        System.out.println();
        System.out.println(BOLD + CYAN + "═".repeat(78) + RESET);
        System.out.println(BOLD + CYAN + "  " + title + RESET);
        System.out.println(DIM + "  " + whatItShows + RESET);
        System.out.println(BOLD + CYAN + "═".repeat(78) + RESET);
    }

    /** Sub-heading inside an example (e.g. "Run 1: without system prompt"). */
    public static void step(String text) {
        System.out.println();
        System.out.println(BOLD + YELLOW + "▶ " + text + RESET);
    }

    /** A labelled block of text we are SENDING (system prompt, user message, params). */
    public static void sent(String label, String text) {
        System.out.println(MAGENTA + "┌─ " + label + " " + RESET);
        text.lines().forEach(l -> System.out.println(MAGENTA + "│ " + RESET + l));
        System.out.println(MAGENTA + "└" + RESET);
    }

    /**
     * Pretty-prints an SDK object as the JSON that goes over the wire (request body or response).
     * Thinking signatures, and long strings in requests (e.g. a big system prompt), are shortened
     * so the structure stays readable; response text is never shortened.
     */
    public static void json(String label, Object value, boolean outgoing) {
        String color = outgoing ? MAGENTA : GREEN;
        String json;
        try {
            JsonNode tree = ObjectMappers.jsonMapper().valueToTree(value);
            shorten(tree, outgoing);
            json = ObjectMappers.jsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(tree);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            json = String.valueOf(value);
        }
        System.out.println(color + "┌─ " + label + " " + RESET);
        json.lines().forEach(l -> System.out.println(color + "│ " + RESET + l));
        System.out.println(color + "└" + RESET);
    }

    private static void shorten(JsonNode node, boolean outgoing) {
        if (node instanceof ArrayNode array) {
            array.forEach(child -> shorten(child, outgoing));
        } else if (node instanceof ObjectNode object) {
            object.properties().forEach(field -> {
                JsonNode child = field.getValue();
                if (child.isTextual()) {
                    int keep = field.getKey().equals("signature") ? 20 : outgoing ? 300 : Integer.MAX_VALUE;
                    String text = child.asText();
                    if (text.length() - 50 > keep) {
                        field.setValue(TextNode.valueOf(
                                text.substring(0, keep) + "… [+" + (text.length() - keep) + " chars]"));
                    }
                } else {
                    shorten(child, outgoing);
                }
            });
        }
    }

    /** Explanatory note printed in dim text. */
    public static void note(String text) {
        text.lines().forEach(l -> System.out.println(DIM + "  ℹ " + l + RESET));
    }

    public static void error(String text) {
        System.out.println(RED + BOLD + "✖ " + text + RESET);
    }

    /** Prints every content block, the stop reason, and token usage of a response. */
    public static void response(Message message) {
        System.out.println(GREEN + "┌─ content, rendered (" + message.model().asString() + ")" + RESET);
        for (ContentBlock block : message.content()) {
            block.thinking().ifPresent(t -> {
                String text = t.thinking().isBlank() ? "(thinking happened; text omitted by default)" : t.thinking();
                System.out.println(GREEN + "│ " + DIM + "[thinking] " + RESET);
                text.lines().forEach(l -> System.out.println(GREEN + "│ " + DIM + "  " + l + RESET));
            });
            block.text().ifPresent(t -> t.text().lines().forEach(l -> System.out.println(GREEN + "│ " + RESET + l)));
        }
        System.out.println(GREEN + "└" + RESET);
        stopReason(message);
        usage(message.usage());
    }

    public static void stopReason(Message message) {
        String reason = message.stopReason().map(r -> r.asString()).orElse("?");
        String extra = message.stopSequence().map(s -> "  (matched: \"" + s + "\")").orElse("");
        System.out.println("  " + BOLD + "stop_reason: " + RESET + reason + extra);
        if (message.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
            String category = message.stopDetails().flatMap(d -> d.category()).map(c -> c.asString()).orElse("unknown");
            String why = message.stopDetails().flatMap(d -> d.explanation()).orElse("");
            System.out.println("  " + RED + BOLD + "REFUSED by a safety classifier (category: " + category + ")" + RESET);
            System.out.println("  " + RED + "HTTP status was still 200, content is empty, input tokens are still billed." + RESET);
            System.out.println("  " + RED + "Always check stop_reason before reading content." + RESET);
            if (!why.isBlank()) {
                System.out.println("  " + DIM + why + RESET);
            }
        }
    }

    public static void usage(Usage u) {
        long cacheWrite = u.cacheCreationInputTokens().orElse(0L);
        long cacheRead = u.cacheReadInputTokens().orElse(0L);
        System.out.println("  " + DIM + LINE.substring(0, 40) + RESET);
        System.out.printf("  %-28s %8d%n", "input_tokens (uncached)", u.inputTokens());
        System.out.printf("  %-28s %8d%n", "cache_creation_input_tokens", cacheWrite);
        System.out.printf("  %-28s %8d%n", "cache_read_input_tokens", cacheRead);
        System.out.printf("  %-28s %8d%n", "output_tokens", u.outputTokens());
    }

    public static void line() {
        System.out.println(DIM + LINE + RESET);
    }
}
