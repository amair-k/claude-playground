package com.amairk.claude.examples;

import com.anthropic.client.AnthropicClient;

/** One runnable learning example. */
public interface Example {

    String description();

    void run(AnthropicClient client);
}
