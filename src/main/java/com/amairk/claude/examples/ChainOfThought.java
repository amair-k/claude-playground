package com.amairk.claude.examples;

import com.amairk.claude.Config;
import com.amairk.claude.Printer;
import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.ThinkingConfigAdaptive;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** DIY chain-of-thought with tags vs the API's built-in thinking. */
public class ChainOfThought implements Example {

    private static final String PUZZLE = """
            A batch job processes 1,200 records per minute. It starts at 09:00.
            At 09:25 it slows to half speed because of DB contention, and at 09:40 it returns to full speed.
            How many records are processed by 10:00?""";

    private static final String COT_PROMPT = PUZZLE + """


            Show your calculation step by step as part of your answer,
            then give only the final number inside <answer></answer> tags.""";

    @Override
    public String description() {
        return "Prompted CoT (visible working + <answer>) vs built-in API thinking";
    }

    @Override
    public void run(AnthropicClient client) {
        Printer.title("Chain-of-thought", "Reasoning tokens first, answer after: each token conditions the next");

        Printer.step("1) Prompted CoT: visible calculation in the answer, final value in <answer>");
        Message prompted = Call.create(client, MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .addUserMessage(COT_PROMPT)
                .build());
        Matcher m = Pattern.compile("<answer>(.*?)</answer>", Pattern.DOTALL).matcher(BasicText.of(prompted));
        Printer.sent("extracted <answer>", m.find() ? m.group(1).strip() : "(not found)");

        Printer.step("2) Built-in thinking with display=summarized (no CoT instructions in the prompt)");
        Call.create(client, MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .thinking(ThinkingConfigAdaptive.builder()
                        .display(ThinkingConfigAdaptive.Display.SUMMARIZED)
                        .build())
                .addUserMessage(PUZZLE)
                .build());

        Printer.note("""
                Prompted CoT works on any model; the reasoning is ordinary text you pay for as output.
                Built-in thinking is a separate block type. On current models it is always on; 'summarized'
                just makes a readable summary visible (by default the thinking text is omitted).
                Expected answer: 25*1200 + 15*600 + 20*1200 = 63,000.
                Don't ask models that already think to put reasoning in <thinking> tags: that looks like
                extracting the hidden thinking and can be refused (stop_reason: refusal, reasoning_extraction).""");
    }
}
