package com.amairk.claude.examples;

import com.amairk.claude.Config;
import com.amairk.claude.Printer;
import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

/** The smallest possible call: one user message in, content blocks + stop_reason + usage out. */
public class BasicMessage implements Example {

    @Override
    public String description() {
        return "One request, one response: content blocks, stop_reason, usage";
    }

    @Override
    public void run(AnthropicClient client) {
        Printer.title("Basic message", "POST /v1/messages with a single user turn");

        String question = "In two sentences, what is the difference between a process and a thread?";
        Printer.sent("user", question);

        MessageCreateParams params = MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .addUserMessage(question)
                .build();

        Message response = client.messages().create(params);
        Printer.response(response);

        Printer.note("""
                content is a LIST of blocks, not one string. On current Opus models thinking is always on,
                so you will usually see a [thinking] block before the text block.
                input_tokens/output_tokens are what you are billed for.""");
    }
}
