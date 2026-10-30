package com.amairk.claude;

import com.amairk.claude.examples.BasicMessage;
import com.amairk.claude.examples.ChainOfThought;
import com.amairk.claude.examples.Example;
import com.amairk.claude.examples.FewShot;
import com.amairk.claude.examples.GenerationParams;
import com.amairk.claude.examples.MultiTurn;
import com.amairk.claude.examples.PromptCaching;
import com.amairk.claude.examples.SystemPrompt;
import com.amairk.claude.examples.TokenCounting;
import com.amairk.claude.examples.XmlTags;
import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.BadRequestException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entry point. Run one example by name:
 *   mvn -q compile exec:java -Dexec.args="basic"
 * or with no argument to list them.
 */
public class App {

    private static final Map<String, Example> EXAMPLES = new LinkedHashMap<>();

    static {
        EXAMPLES.put("basic", new BasicMessage());
        EXAMPLES.put("multiturn", new MultiTurn());
        EXAMPLES.put("system", new SystemPrompt());
        EXAMPLES.put("fewshot", new FewShot());
        EXAMPLES.put("xml", new XmlTags());
        EXAMPLES.put("cot", new ChainOfThought());
        EXAMPLES.put("params", new GenerationParams());
        EXAMPLES.put("tokens", new TokenCounting());
        EXAMPLES.put("caching", new PromptCaching());
    }

    public static void main(String[] args) {
        if (args.length == 0 || !EXAMPLES.containsKey(args[0])) {
            printUsage();
            return;
        }

        AnthropicClient client;
        try {
            client = Config.client();
        } catch (IllegalStateException e) {
            Printer.error(e.getMessage());
            return;
        }
        try {
            EXAMPLES.get(args[0]).run(client);
        } catch (UnauthorizedException e) {
            Printer.error("Authentication failed (401): check ANTHROPIC_API_KEY in .env");
        } catch (BadRequestException e) {
            Printer.error("Bad request (400), the API rejected a parameter: " + e.getMessage());
        } catch (RateLimitException e) {
            Printer.error("Rate limited (429): wait a bit and retry");
        } catch (AnthropicServiceException e) {
            Printer.error("API error " + e.statusCode() + ": " + e.getMessage());
        } finally {
            client.close();
        }
    }

    private static void printUsage() {
        System.out.println("Usage: mvn -q compile exec:java -Dexec.args=\"<example>\"");
        System.out.println();
        System.out.println("Examples:");
        EXAMPLES.forEach((name, ex) -> System.out.printf("  %-10s %s%n", name, ex.description()));
        System.out.println();
        System.out.println("Model: " + Config.MODEL + "  (change CLAUDE_MODEL in .env)");
    }
}
