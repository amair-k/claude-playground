package com.amairk.claude.examples;

import com.amairk.claude.Config;
import com.amairk.claude.Printer;
import com.anthropic.client.AnthropicClient;
import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.ArrayList;
import java.util.List;

/** Invoice text in, a typed Java record out: prompt-only JSON vs schema-enforced structured output. */
public class StructuredOutput implements Example {

    /** What we want back. The SDK turns this record into the JSON schema sent to the API. */
    public record Invoice(
            @JsonPropertyDescription("Company that issued the invoice") String vendor,
            @JsonPropertyDescription("Invoice number exactly as printed") String invoiceNumber,
            @JsonPropertyDescription("Due date in ISO format YYYY-MM-DD") String dueDate,
            @JsonPropertyDescription("ISO 4217 currency code, e.g. EUR") String currency,
            List<LineItem> lineItems,
            @JsonPropertyDescription("Total amount due exactly as stated on the invoice") double total) {}

    public record LineItem(String description, int quantity, double unitPrice, double amount) {}

    // The stated total (1,250.00) deliberately does not match the line items (1,200.00).
    private static final String INVOICE_TEXT = """
            NORTHWIND CLOUD SERVICES GmbH
            Invoice No.: NW-2026-0457        Issued: 3 October 2026
            Payment due within 30 days of issue.

            Item                          Qty   Unit price    Amount
            Managed Kubernetes cluster      2      400.00     800.00
            Support plan (monthly)          1      250.00     250.00
            Backup storage (TB)             3       50.00     150.00

            TOTAL DUE: EUR 1,250.00""";

    private static final String INSTRUCTION = "Extract the invoice data from this document.\n\n<document>\n"
            + INVOICE_TEXT + "\n</document>";

    @Override
    public String description() {
        return "Invoice -> Java record: prompt-only JSON vs schema-enforced output";
    }

    @Override
    public void run(AnthropicClient client) {
        Printer.title("Structured output", "Reliable, typed JSON your code can parse: invoice text -> Invoice record");

        Printer.step("1) Prompt-only: ask for JSON in words, then try to parse it ourselves");
        Message loose = Call.create(client, MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .addUserMessage(INSTRUCTION + """


                        Return JSON with fields vendor, invoiceNumber, dueDate, currency,
                        lineItems (description, quantity, unitPrice, amount) and total.""")
                .build());
        tryParse(BasicText.of(loose));
        Printer.note("""
                Nothing guarantees the shape: the model may add prose or ```json fences, rename a field,
                or return "1,250.00" as a string. It usually works, which is exactly why it fails in production.""");

        Printer.step("2) Structured output: outputConfig(Invoice.class), the API enforces the schema");
        StructuredMessage<Invoice> structured = Call.create(client, MessageCreateParams.builder()
                .model(Config.MODEL)
                .maxTokens(Config.MAX_TOKENS)
                .addUserMessage(INSTRUCTION)
                .outputConfig(Invoice.class)
                .build());
        Printer.note("""
                Look at output_config.format.schema in the request: generated from the Invoice record,
                including the @JsonPropertyDescription texts. The text block is guaranteed to match it.""");

        if (structured.stopReason().filter(StopReason.END_TURN::equals).isEmpty()) {
            Printer.error("No usable result: stop_reason is not end_turn (refusal or max_tokens). Check before parsing.");
            return;
        }
        Invoice invoice = structured.content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .findFirst()
                .orElseThrow();
        StringBuilder view = new StringBuilder()
                .append("invoice.vendor()        = ").append(invoice.vendor()).append('\n')
                .append("invoice.invoiceNumber() = ").append(invoice.invoiceNumber()).append('\n')
                .append("invoice.dueDate()       = ").append(invoice.dueDate()).append('\n')
                .append("invoice.currency()      = ").append(invoice.currency()).append('\n')
                .append("invoice.total()         = ").append(invoice.total()).append('\n')
                .append("invoice.lineItems():\n");
        invoice.lineItems().forEach(i -> view.append(String.format("  %-28s %3d x %8.2f = %8.2f%n",
                i.description(), i.quantity(), i.unitPrice(), i.amount())));
        Printer.sent("typed Java object, Invoice record (no parsing code written)", view.toString());

        Printer.step("3) Our own validation: the schema guarantees the SHAPE, not that the values make sense");
        List<String> problems = validate(invoice);
        if (problems.isEmpty()) {
            Printer.sent("validation", "OK");
        } else {
            problems.forEach(Printer::error);
        }
        Printer.note("""
                The document itself is inconsistent (line items add up to 1,200.00, total says 1,250.00).
                The model extracted what is written; catching that is business logic, so it lives in your code.
                Typical next step: reject, send to a human, or ask the model again with the problem stated.""");
    }

    private static void tryParse(String text) {
        try {
            Invoice parsed = ObjectMappers.jsonMapper().readValue(text, Invoice.class);
            Printer.sent("Jackson parse of the raw text", "OK: " + parsed);
        } catch (Exception e) {
            Printer.error("Jackson could not parse the raw text: " + e.getMessage().lines().findFirst().orElse(""));
        }
    }

    private static List<String> validate(Invoice invoice) {
        List<String> problems = new ArrayList<>();
        double sum = 0;
        for (LineItem item : invoice.lineItems()) {
            if (Math.abs(item.quantity() * item.unitPrice() - item.amount()) > 0.005) {
                problems.add("Line '" + item.description() + "': quantity x unit price != amount");
            }
            sum += item.amount();
        }
        if (Math.abs(sum - invoice.total()) > 0.005) {
            problems.add(String.format("Line items add up to %.2f but total is %.2f", sum, invoice.total()));
        }
        if (!invoice.dueDate().matches("\\d{4}-\\d{2}-\\d{2}")) {
            problems.add("dueDate is not YYYY-MM-DD: " + invoice.dueDate());
        }
        return problems;
    }
}
