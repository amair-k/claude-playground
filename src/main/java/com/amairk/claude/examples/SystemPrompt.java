package com.amairk.claude.examples;

import com.amairk.claude.Config;
import com.amairk.claude.Printer;
import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;

/** Same user message, with and without a top-level `system` field. */
public class SystemPrompt implements Example {

    private static final String CODE = """
            public int average(int[] values) {
                int sum = 0;
                for (int v : values) sum += v;
                return sum / values.length;
            }""";

    private static final String SYSTEM = """
            You are a senior Java code reviewer.
            Report only correctness bugs, as a numbered list, max 3 items, one line each.
            No praise, no style comments, no rewritten code.""";

    @Override
    public String description() {
        return "Top-level system prompt vs none: role, tone, format";
    }

    @Override
    public void run(AnthropicClient client) {
        Printer.title("System prompt", "system is a separate top-level field, not a message in the list");

        String user = "Review this method:\n\n" + CODE;

        Printer.step("Run 1: no system prompt");
        Call.create(client, MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .addUserMessage(user)
                .build());

        Printer.step("Run 2: with a system prompt (see the top-level \"system\" field)");
        Call.create(client, MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .system(SYSTEM)
                .addUserMessage(user)
                .build());

        Printer.note("Same question; the system prompt changes role, length and format of the answer.");
    }
}
