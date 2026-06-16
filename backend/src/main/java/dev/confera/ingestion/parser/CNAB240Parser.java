package dev.confera.ingestion.parser;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses CNAB 240 fixed-width files (240 chars per line).
 * Segment T layout (1-indexed positions):
 *   - Record type:  position 8  → '3' (detail)
 *   - Segment type: position 14 → 'T'
 *   - Date:         positions 13-20  → DDMMAAAA
 *   - Document:     positions 73-92
 *   - Value:        positions 93-107 → 13 digits, 2 implicit decimals
 */
@Component
public class CNAB240Parser {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("ddMMyyyy");

    public List<BankStatementData> parse(InputStream input) throws IOException {
        List<BankStatementData> results = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(input, StandardCharsets.ISO_8859_1))) {

            String bankCode = "";
            String agency   = "";
            String account  = "";
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.length() < 240) continue;

                char recordType = line.charAt(7);

                if (recordType == '0' && bankCode.isEmpty()) {
                    // File header — extract bank identifiers
                    bankCode = line.substring(0, 3).trim();
                    agency   = line.substring(52, 57).trim();
                    account  = line.substring(57, 69).trim();
                    continue;
                }

                if (recordType != '3') continue;
                if (line.charAt(13) != 'T') continue;

                try {
                    // positions 13-20 (1-indexed) = substring(12, 20) — DDMMAAAA
                    String dateStr  = line.substring(12, 20).trim();
                    String docNum   = line.substring(72, 92).trim();   // positions 73-92
                    String valueStr = line.substring(92, 107).trim();  // positions 93-107

                    if (dateStr.isEmpty() || valueStr.isBlank()) continue;

                    LocalDate date       = LocalDate.parse(dateStr, DATE_FMT);
                    long amountCents     = Long.parseLong(valueStr);   // 2 implicit decimal places

                    results.add(new BankStatementData(
                        bankCode, agency, account, date, date, amountCents, "", docNum));
                } catch (Exception ignored) {
                    // Malformed line — skip
                }
            }
        }
        return results;
    }
}