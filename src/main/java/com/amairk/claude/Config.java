package com.amairk.claude;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * Everything that changes between machines/runs comes from the environment:
 * a real environment variable wins, otherwise the value from the .env file.
 */
public final class Config {

    private static final Dotenv ENV = Dotenv.configure().ignoreIfMissing().load();

    public static final String MODEL = get("CLAUDE_MODEL", "claude-opus-5-5");
    public static final long MAX_TOKENS = Long.parseLong(get("CLAUDE_MAX_TOKENS", "4000"));

    private Config() {}

    public static AnthropicClient client() {
        String apiKey = get("ANTHROPIC_API_KEY", null);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "ANTHROPIC_API_KEY is not set. Copy .env.example to .env and put your key there.");
        }
        return AnthropicOkHttpClient.builder().apiKey(apiKey).build();
    }

    private static String get(String name, String fallback) {
        String value = ENV.get(name); // Dotenv checks real env vars first, then .env
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
