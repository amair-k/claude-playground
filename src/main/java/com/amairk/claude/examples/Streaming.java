package com.amairk.claude.examples;

import com.amairk.claude.Config;
import com.amairk.claude.Printer;
import com.anthropic.client.AnthropicClient;
import com.anthropic.core.ObjectMappers;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.helpers.MessageAccumulator;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.ThinkingConfigAdaptive;

/** stream: true -> the reply arrives as server-sent events while it is being generated. */
public class Streaming implements Example {

    @Override
    public String description() {
        return "Server-sent events: raw event log, then live typing + time to first token";
    }

    @Override
    public void run(AnthropicClient client) {
        Printer.title("Streaming", "Same endpoint with stream: true; the reply arrives as SSE events while it is generated");

        Printer.step("1) Raw event log: every SSE event, in order, with time since the request was sent");
        MessageCreateParams small = MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .thinking(ThinkingConfigAdaptive.builder()
                        .display(ThinkingConfigAdaptive.Display.SUMMARIZED)
                        .build())
                .addUserMessage("Name three HTTP methods that are idempotent, one per line, no explanation.")
                .build();
        Printer.json("request  POST /v1/messages  (the SDK adds \"stream\": true)", small._body(), true);

        long start = System.nanoTime();
        try (StreamResponse<RawMessageStreamEvent> stream = client.messages().createStreaming(small)) {
            stream.stream().forEach(event -> System.out.printf("  %6.2fs  %-20s %s%n",
                    seconds(start), event.type().asString(), compact(event)));
        }
        Printer.note("""
                message_start        envelope: id, model, input usage (content still empty)
                content_block_start  a new block begins at index N (a thinking block first if the model thinks, then text)
                content_block_delta  a piece of that block: thinking_delta / signature_delta / text_delta
                content_block_stop   block N is complete
                message_delta        stop_reason + final output_tokens
                message_stop         done (ping events may also appear: keep-alives, ignore them)""");

        Printer.step("2) Live: print text as it arrives, and rebuild the full Message at the end");
        MessageCreateParams longer = MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .addUserMessage("Explain Kafka consumer groups to a Java backend developer in about 150 words. Plain text, no markdown.")
                .build();
        Printer.json("request  POST /v1/messages  (the SDK adds \"stream\": true)", longer._body(), true);

        MessageAccumulator accumulator = MessageAccumulator.create();
        long sent = System.nanoTime();
        double[] firstText = {-1};
        System.out.println();
        try (StreamResponse<RawMessageStreamEvent> stream = client.messages().createStreaming(longer)) {
            stream.stream()
                    .peek(accumulator::accumulate) // collects every event into the final Message
                    .flatMap(event -> event.contentBlockDelta().stream())
                    .flatMap(delta -> delta.delta().text().stream())
                    .forEach(text -> {
                        if (firstText[0] < 0) {
                            firstText[0] = seconds(sent);
                        }
                        System.out.print(text.text());
                        System.out.flush();
                    });
        }
        double total = seconds(sent);
        System.out.println();
        System.out.println();

        Message message = accumulator.message();
        Printer.json("accumulated Message (same shape as a non-streaming response)", message, false);
        Printer.stopReason(message);
        Printer.usage(message.usage());
        System.out.printf("  %-28s %7.2fs%n", "time to first text", firstText[0]);
        System.out.printf("  %-28s %7.2fs%n", "time to complete reply", total);

        Printer.note("""
                Without streaming the user stares at nothing for the full time above; with it, text appears
                after the first number. Most of the wait before it is thinking (see thinking_tokens): with the
                default display "omitted" nothing is visible during that time.
                Same tokens, same price: streaming changes latency as perceived, not cost.
                Long outputs / high max_tokens should stream anyway: one long silent HTTP request can hit timeouts.
                Check stop_reason at the end (message_delta), exactly as with a normal response.""");
    }

    private static double seconds(long since) {
        return (System.nanoTime() - since) / 1_000_000_000.0;
    }

    /** One-line JSON of an event, cut to fit the terminal. */
    private static String compact(RawMessageStreamEvent event) {
        String json;
        try {
            json = ObjectMappers.jsonMapper().writeValueAsString(event);
        } catch (Exception e) {
            json = event.toString();
        }
        return json.length() > 140 ? json.substring(0, 140) + "…" : json;
    }
}
