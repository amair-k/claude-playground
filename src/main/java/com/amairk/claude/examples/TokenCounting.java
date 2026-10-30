package com.amairk.claude.examples;

import com.amairk.claude.Config;
import com.amairk.claude.Printer;
import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCountTokensParams;
import com.anthropic.models.messages.MessageCreateParams;

/** Count tokens before sending (free endpoint), then compare with what usage reports. */
public class TokenCounting implements Example {

    private static final String SYSTEM = "You are a concise assistant for Java developers.";

    private static final String[] INPUTS = {
            "Hello",
            "tokenization",
            "Explain the difference between HashMap and ConcurrentHashMap in one paragraph.",
    };

    @Override
    public String description() {
        return "count_tokens before sending vs usage after";
    }

    @Override
    public void run(AnthropicClient client) {
        Printer.title("Token counting", "Tokens, not characters, are what limits and billing are measured in");

        Printer.step("count_tokens for a few inputs (no generation, not billed)");
        System.out.printf("  %-82s %6s %6s%n", "input", "chars", "tokens");
        for (String input : INPUTS) {
            long tokens = client.messages().countTokens(MessageCountTokensParams.builder()
                    .model(Config.MODEL)
                    .system(SYSTEM)
                    .addUserMessage(input)
                    .build()).inputTokens();
            System.out.printf("  %-82s %6d %6d%n", '"' + input + '"', input.length(), tokens);
        }
        Printer.note("""
                Counts include the system prompt and the message framing, so even "Hello" is more than 1.
                The difference between rows is the cost of the text itself.""");

        String last = INPUTS[INPUTS.length - 1];
        Printer.step("Now actually send the last one and compare with usage.input_tokens");
        Printer.sent("user", last);
        Message response = client.messages().create(MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .system(SYSTEM)
                .addUserMessage(last)
                .build());
        Printer.response(response);
        Printer.note("usage.input_tokens should match (or be very close to) the count_tokens result above.");
    }
}
