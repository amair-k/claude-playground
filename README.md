# claude-playground

Plain Java 21 + Maven project for trying out the Claude API and prompting techniques covered in `../prompting.md`.
No Spring: each example is a few lines of SDK code so nothing is hidden. Spring Boot comes later in the plan.

## Setup
1. Get an API key at https://platform.claude.com (Settings → API keys) and add credits under Billing.
   A claude.ai Pro/Max subscription does **not** include API usage; the API is billed separately.
2. `cp .env.example .env` and paste the key into `ANTHROPIC_API_KEY`.
3. Run from this folder:

```bash
mvn -q compile exec:java                          # list examples
mvn -q compile exec:java -Dexec.args="basic"      # run one
```

Real environment variables override values in `.env`. Set `ANTHROPIC_BASE_URL` to route calls through a gateway/proxy instead of api.anthropic.com.

## Examples
| name | notes topic | what it shows |
|---|---|---|
| `basic` | API shape | raw request/response JSON, content blocks, `stop_reason`, `usage` |
| `multiturn` | API shape | stateless API: history is resent every call |
| `system` | system prompts | same question with and without `system` |
| `fewshot` | few-shot | zero-shot vs 3 `<example>`s |
| `xml` | XML tags | `<document>`/`<instructions>` in, `<answer>` extracted by regex |
| `cot` | chain-of-thought | prompted working + `<answer>` vs built-in thinking |
| `params` | generation params | `max_tokens` cut-off, `stop_sequences`, `effort` |
| `tokens` | tokens | `count_tokens` before sending vs `usage` after |
| `caching` | prompt caching | cache write on call 1, cache read on call 2 |
| `structured` | structured output | invoice → Java record: prompt-only JSON vs `outputConfig(Invoice.class)`, then business validation |
| `streaming` | streaming | raw SSE event log, live text, `MessageAccumulator`, time to first text |

## Layout
```
src/main/java/com/amairk/claude/
  App.java          picks an example by name, handles API errors
  Config.java       reads ANTHROPIC_API_KEY, CLAUDE_MODEL, CLAUDE_MAX_TOKENS
  Printer.java      formatted console output
  examples/         one class per concept
```

Every example prints the raw JSON request and response of each call (via `examples/Call.java`); long request strings and thinking signatures are shortened for readability.

Adding an example: create a class implementing `Example`, make its API calls through `Call`, register it in `App`.
