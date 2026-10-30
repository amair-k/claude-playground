package com.amairk.claude.examples;

import com.amairk.claude.Config;
import com.amairk.claude.Printer;
import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;

/** max_tokens, stop_sequences and effort. */
public class GenerationParams implements Example {

    private static final String LIST_PROMPT = "List the 10 most common HTTP status codes, one per line, as '<code> - <meaning>'.";

    @Override
    public String description() {
        return "max_tokens cut-off, stop_sequences, effort (low vs high)";
    }

    @Override
    public void run(AnthropicClient client) {
        Printer.title("Generation parameters", "Controls that shape length, boundaries and how hard the model works");

        Printer.step("1) max_tokens = 60: a hard ceiling that cuts the reply off");
        Printer.sent("params", "max_tokens: 60");
        Printer.sent("user", LIST_PROMPT);
        Printer.response(client.messages().create(MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(60L)
                .addUserMessage(LIST_PROMPT)
                .build()));
        Printer.note("""
                stop_reason is max_tokens, not end_turn: the reply is truncated.
                Thinking tokens count against max_tokens too, so a tiny limit can leave no room for text.""");

        Printer.step("2) stop_sequences = [\"404\"]: generation halts when that string would be produced");
        Printer.sent("params", "stop_sequences: [\"404\"]");
        Printer.sent("user", LIST_PROMPT);
        Printer.response(client.messages().create(MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .addStopSequence("404")
                .addUserMessage(LIST_PROMPT)
                .build()));
        Printer.note("stop_reason is stop_sequence; the matched string itself is not included in the output.");

        String hardQuestion = "Is 2^31 - 1 prime? Answer yes or no with a one-line justification.";
        for (OutputConfig.Effort effort : new OutputConfig.Effort[] {OutputConfig.Effort.LOW, OutputConfig.Effort.HIGH}) {
            Printer.step("3) effort = " + effort.asString());
            Printer.sent("params", "output_config.effort: " + effort.asString());
            Printer.sent("user", hardQuestion);
            Printer.response(client.messages().create(MessageCreateParams.builder()
                    .model(Config.MODEL)
                    .maxTokens(Config.MAX_TOKENS)
                    .outputConfig(OutputConfig.builder().effort(effort).build())
                    .addUserMessage(hardQuestion)
                    .build()));
        }
        Printer.note("Compare output_tokens: higher effort = more thinking = more tokens, cost and latency.");

        Printer.line();
        Printer.note("""
                Where are temperature / top_p / top_k?
                They have been removed: current models reject them with a 400, and this SDK version has no
                builder method for them. 'effort' is now the main knob for how much work the model does.""");
    }
}
