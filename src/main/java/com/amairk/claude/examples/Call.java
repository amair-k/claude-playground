package com.amairk.claude.examples;

import com.amairk.claude.Printer;
import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCountTokensParams;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageTokensCount;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;

/** Every API call goes through here so each example shows the JSON request and response. */
final class Call {

    private Call() {}

    /** POST /v1/messages: request JSON, response JSON, then the readable text, stop_reason and usage. */
    static Message create(AnthropicClient client, MessageCreateParams params) {
        Printer.json("request  POST /v1/messages", params._body(), true);
        Message response = client.messages().create(params);
        Printer.json("response 200 OK", response, false);
        Printer.response(response);
        return response;
    }

    /** Structured output: same as create(), but the SDK parses the JSON text block into a T. */
    static <T> StructuredMessage<T> create(AnthropicClient client, StructuredMessageCreateParams<T> params) {
        Printer.json("request  POST /v1/messages", params.rawParams()._body(), true);
        StructuredMessage<T> response = client.messages().create(params);
        Printer.json("response 200 OK", response.rawMessage(), false);
        Printer.response(response.rawMessage());
        return response;
    }

    /** POST /v1/messages/count_tokens: request JSON and response JSON. */
    static long countTokens(AnthropicClient client, MessageCountTokensParams params) {
        Printer.json("request  POST /v1/messages/count_tokens", params._body(), true);
        MessageTokensCount count = client.messages().countTokens(params);
        Printer.json("response 200 OK", count, false);
        return count.inputTokens();
    }
}
