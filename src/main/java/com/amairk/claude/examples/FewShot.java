package com.amairk.claude.examples;

import com.amairk.claude.Config;
import com.amairk.claude.Printer;
import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;

/** Zero-shot vs few-shot: examples steer the output format without writing rules. */
public class FewShot implements Example {

    private static final String TICKET = "Checkout page takes 40 seconds to load during sales";

    private static final String ZERO_SHOT = """
            Classify this support ticket by severity and category.

            Ticket: "%s"
            """.formatted(TICKET);

    private static final String FEW_SHOT = """
            Classify each support ticket by severity and category.

            <example>
            Ticket: "The server crashed at 2am"
            Output: {"severity": "high", "category": "infra"}
            </example>
            <example>
            Ticket: "Typo in the footer"
            Output: {"severity": "low", "category": "ui"}
            </example>
            <example>
            Ticket: "Users can see other users' invoices"
            Output: {"severity": "critical", "category": "security"}
            </example>

            Ticket: "%s"
            Output:""".formatted(TICKET);

    @Override
    public String description() {
        return "Zero-shot vs few-shot classification";
    }

    @Override
    public void run(AnthropicClient client) {
        Printer.title("Few-shot prompting", "Show input -> output pairs; the model copies the pattern");

        Printer.step("Zero-shot: instruction only");
        Printer.sent("user", ZERO_SHOT);
        Printer.response(ask(client, ZERO_SHOT));

        Printer.step("Few-shot: 3 examples wrapped in <example> tags");
        Printer.sent("user", FEW_SHOT);
        Printer.response(ask(client, FEW_SHOT));

        Printer.note("""
                Zero-shot gives free-form prose. Few-shot returns the exact JSON shape and labels shown.
                Few-shot costs more input tokens on every call (examples are resent each time).""");
    }

    private static com.anthropic.models.messages.Message ask(AnthropicClient client, String prompt) {
        return client.messages().create(MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .addUserMessage(prompt)
                .build());
    }
}
