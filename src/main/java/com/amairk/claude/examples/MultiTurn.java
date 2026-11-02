package com.amairk.claude.examples;

import com.amairk.claude.Config;
import com.amairk.claude.Printer;
import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

/** The API is stateless: to "continue" a conversation you resend the whole history every call. */
public class MultiTurn implements Example {

    @Override
    public String description() {
        return "Stateless API: resend history to continue a conversation";
    }

    @Override
    public void run(AnthropicClient client) {
        Printer.title("Multi-turn conversation", "The model remembers nothing; the history you send IS the memory");

        String first = "My name is Mohammed and I write Java backends. Suggest one AI topic for me to learn.";
        Printer.step("Turn 1");
        MessageCreateParams.Builder conversation = MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .addUserMessage(first);
        Message reply1 = Call.create(client, conversation.build());

        String second = "What is my name, and what did you just suggest?";
        Printer.step("Turn 2: history (turn 1 + reply) is sent again; look at the messages array");
        // addMessage(Message) appends the full assistant reply, all blocks included (thinking + text)
        conversation.addMessage(reply1).addUserMessage(second);
        Call.create(client, conversation.build());

        Printer.step("Turn 2 again, but WITHOUT the history");
        Call.create(client, MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .addUserMessage(second)
                .build());

        Printer.note("""
                Compare input_tokens: turn 2 with history costs more because everything is resent.
                Without history the model has no idea who you are.""");
    }
}
