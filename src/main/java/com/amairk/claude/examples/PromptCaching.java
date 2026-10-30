package com.amairk.claude.examples;

import com.amairk.claude.Config;
import com.amairk.claude.Printer;
import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;

import java.util.List;

/** Same large system prompt sent twice: first call writes the cache, second reads it. */
public class PromptCaching implements Example {

    /**
     * A long, fixed "handbook" so the prefix is above the minimum cacheable size.
     * Below that minimum nothing is cached and no error is raised.
     */
    private static final String HANDBOOK = buildHandbook();

    @Override
    public String description() {
        return "cache_control on a large system prompt: write, then read";
    }

    @Override
    public void run(AnthropicClient client) {
        Printer.title("Prompt caching", "Reuse the processed prefix: cache write once, cheap reads after");

        long handbookTokens = client.messages().countTokens(com.anthropic.models.messages.MessageCountTokensParams.builder()
                .model(Config.MODEL)
                .system(HANDBOOK)
                .addUserMessage("x")
                .build()).inputTokens();
        Printer.sent("system (cached block)", "Engineering handbook, ~" + handbookTokens + " tokens\n"
                + "cache_control: {type: ephemeral}  (5-minute TTL)");

        String[] questions = {
                "According to the handbook, what is rule 7 about? One sentence.",
                "According to the handbook, what is rule 42 about? One sentence.",
        };

        for (int i = 0; i < questions.length; i++) {
            Printer.step("Call " + (i + 1) + (i == 0 ? ": expect a cache WRITE" : ": same prefix, expect a cache READ"));
            Printer.sent("user (not cached, changes every call)", questions[i]);
            Message response = client.messages().create(MessageCreateParams.builder()
                    .model(Config.MODEL)
                    .maxTokens(Config.MAX_TOKENS)
                    .systemOfTextBlockParams(List.of(TextBlockParam.builder()
                            .text(HANDBOOK)
                            .cacheControl(CacheControlEphemeral.builder().build())
                            .build()))
                    .addUserMessage(questions[i])
                    .build());
            Printer.response(response);
        }

        Printer.note("""
                Call 1: cache_creation_input_tokens ~ handbook size (billed ~1.25x).
                Call 2: cache_read_input_tokens ~ handbook size (billed ~0.1x); input_tokens is only the question.
                If both show 0: the prefix is below the model's minimum, or something in it changed.
                Run the example again within 5 minutes and even call 1 becomes a cache read.""");
    }

    private static String buildHandbook() {
        StringBuilder sb = new StringBuilder("""
                You answer questions about the ACME engineering handbook below. Answer only from it.

                <handbook>
                """);
        String[] topics = {"code review", "logging", "database migrations", "API versioning", "on-call",
                "incident reports", "feature flags", "dependency upgrades", "secrets handling", "load testing"};
        for (int i = 1; i <= 120; i++) {
            String topic = topics[i % topics.length];
            sb.append("Rule ").append(i).append(" (").append(topic).append("): ")
                    .append("Every change related to ").append(topic)
                    .append(" must be documented in the team wiki, reviewed by at least one engineer from another team, ")
                    .append("and recorded with its ticket number, the date, the owner, and a short rollback plan.\n");
        }
        return sb.append("</handbook>").toString();
    }
}
