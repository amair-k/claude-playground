package com.amairk.claude.examples;

import com.amairk.claude.Config;
import com.amairk.claude.Printer;
import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;

/** The smallest possible call, shown as the raw JSON request and response, then the token count. */
public class BasicMessage implements Example {

    @Override
    public String description() {
        return "One request, one response: raw JSON both ways, then token usage";
    }

    @Override
    public void run(AnthropicClient client) {
        Printer.title("Basic message", "POST /v1/messages with a single user turn");

        String question = "In two sentences, what is the difference between a process and a thread?";
        Call.create(client, MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .addUserMessage(question)
                .build());



        Printer.note("""
                content is a LIST of blocks, not one string. On current Opus models thinking is always on,
                so you will usually see a "thinking" block before the "text" block.
                input_tokens = everything you sent (your prompt + the API's own message formatting).
                output_tokens = everything generated, thinking included, even when its text is omitted.""");
    }
}
