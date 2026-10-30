package com.amairk.claude.examples;

import com.anthropic.models.messages.Message;

import java.util.stream.Collectors;

/** Joins only the text blocks of a response (skips thinking blocks). */
final class BasicText {

    private BasicText() {}

    static String of(Message message) {
        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(t -> t.text())
                .collect(Collectors.joining("\n"));
    }
}
