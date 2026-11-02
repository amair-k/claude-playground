package com.amairk.claude.examples;

import com.amairk.claude.Config;
import com.amairk.claude.Printer;
import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** XML tags separate parts of the prompt, and give code a reliable place to extract the answer from. */
public class XmlTags implements Example {

    private static final String CONTRACT = """
            This Service Agreement starts on 1 March 2026 and runs for 24 months.
            The client pays a fixed monthly fee of EUR 12,000, invoiced on the 1st of each month.
            Late payments accrue 1.5% interest per month. Either party may terminate with 90 days notice.
            The provider guarantees 99.9% uptime; each 0.1% below that earns a 5% fee credit.""";

    private static final String PROMPT = """
            <document>
            %s
            </document>

            <instructions>
            List only the financial terms from the document as short bullet points.
            Put the bullet list inside <answer></answer> tags and nothing else inside those tags.
            </instructions>""".formatted(CONTRACT);

    @Override
    public String description() {
        return "<document>/<instructions> in, <answer> out, extracted by code";
    }

    @Override
    public void run(AnthropicClient client) {
        Printer.title("XML tags", "Clear section boundaries in, a parseable <answer> out");

        Message response = Call.create(client, MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .addUserMessage(PROMPT)
                .build());

        Printer.step("What our code extracts with a regex on <answer>...</answer>");
        String text = BasicText.of(response);
        Matcher m = Pattern.compile("<answer>(.*?)</answer>", Pattern.DOTALL).matcher(text);
        if (m.find()) {
            Printer.sent("extracted answer", m.group(1).strip());
        } else {
            Printer.error("No <answer> tag found in the response");
        }

        Printer.note("Anything outside <answer> (preamble, explanations) is ignored by the program.");
    }
}
