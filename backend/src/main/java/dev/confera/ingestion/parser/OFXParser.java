package dev.confera.ingestion.parser;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class OFXParser {

    private static final DateTimeFormatter OFX_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<BankStatementData> parse(InputStream input) throws IOException {
        List<String> lines = new BufferedReader(new InputStreamReader(input, StandardCharsets.ISO_8859_1))
            .lines().map(String::trim).toList();

        String bankCode     = extractTag(lines, "BANKID");
        String agency       = extractTag(lines, "BRANCHID");
        String accountNumber = extractTag(lines, "ACCTID");

        List<BankStatementData> results = new ArrayList<>();
        int i = 0;
        while (i < lines.size()) {
            if (lines.get(i).equalsIgnoreCase("<STMTTRN>")) {
                Map<String, String> fields = new LinkedHashMap<>();
                int j = i + 1;
                while (j < lines.size() && !lines.get(j).equalsIgnoreCase("</STMTTRN>")) {
                    parseInlineTag(lines.get(j), fields);
                    j++;
                }
                BankStatementData record = buildRecord(bankCode, agency, accountNumber, fields);
                if (record != null) results.add(record);
                i = j + 1;
            } else {
                i++;
            }
        }
        return results;
    }

    private String extractTag(List<String> lines, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        for (String line : lines) {
            if (line.startsWith(open)) {
                String value = line.substring(open.length());
                if (value.contains(close)) value = value.substring(0, value.indexOf(close));
                return value.trim();
            }
        }
        return "";
    }

    private void parseInlineTag(String line, Map<String, String> fields) {
        for (String tag : List.of("DTPOSTED", "TRNAMT", "FITID", "NAME", "MEMO", "DTAVAIL")) {
            String open = "<" + tag + ">";
            String close = "</" + tag + ">";
            if (line.startsWith(open)) {
                String value = line.substring(open.length());
                if (value.contains(close)) value = value.substring(0, value.indexOf(close));
                // Strip timezone suffix e.g. [-3:BRT]
                int bracketIdx = value.indexOf('[');
                if (bracketIdx > 0) value = value.substring(0, bracketIdx);
                fields.put(tag, value.trim());
                return;
            }
        }
    }

    private BankStatementData buildRecord(String bankCode, String agency, String accountNumber,
                                          Map<String, String> fields) {
        String dtposted = fields.get("DTPOSTED");
        String trnamt   = fields.get("TRNAMT");
        if (dtposted == null || dtposted.length() < 8 || trnamt == null) return null;

        LocalDate txDate = LocalDate.parse(dtposted.substring(0, 8), OFX_DATE);
        String dtavail   = fields.get("DTAVAIL");
        LocalDate valueDate = (dtavail != null && dtavail.length() >= 8)
            ? LocalDate.parse(dtavail.substring(0, 8), OFX_DATE)
            : txDate;

        long amountCents = new BigDecimal(trnamt)
            .multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValue();

        String description  = Optional.ofNullable(fields.get("NAME")).orElse(fields.getOrDefault("MEMO", ""));
        String documentNumber = fields.getOrDefault("FITID", "");

        return new BankStatementData(bankCode, agency, accountNumber,
            txDate, valueDate, amountCents, description, documentNumber);
    }
}