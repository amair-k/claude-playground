package com.amairk.claude;

import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.Usage;

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

    /** Explanatory note printed in dim text. */
    public static void note(String text) {
        text.lines().forEach(l -> System.out.println(DIM + "  ℹ " + l + RESET));
    }

    public static void error(String text) {
        System.out.println(RED + BOLD + "✖ " + text + RESET);
    }

    /** Prints every content block, the stop reason, and token usage of a response. */
    public static void response(Message message) {
        System.out.println(GREEN + "┌─ response (" + message.model().asString() + ")" + RESET);
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
        message.stopDetails().ifPresent(d -> System.out.println("  " + RED + "refusal details: " + d + RESET));
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
