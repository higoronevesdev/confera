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
 * Parses CNAB 400 fixed-width files (400 chars per line).
 * Detail record type '1' layout (1-indexed positions):
 *   - Record type:  position 1       → '1'
 *   - Document:     positions 63-72
 *   - Date:         positions 110-115 → DDMMAA (2-digit year, prefix 20)
 *   - Value:        positions 152-164 → 13 digits, 2 implicit decimals
 */
@Component
public class CNAB400Parser {

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
                if (line.length() < 400) continue;

                char recordType = line.charAt(0);

                if (recordType == '0' && bankCode.isEmpty()) {
                    // File header
                    bankCode = line.substring(76, 79).trim();
                    agency   = line.substring(17, 21).trim();
                    account  = line.substring(22, 30).trim();
                    continue;
                }

                if (recordType != '1') continue;

                try {
                    String docNum   = line.substring(62, 72).trim();    // positions 63-72
                    String dateStr6 = line.substring(109, 115).trim();  // positions 110-115, DDMMAA
                    String valueStr = line.substring(151, 164).trim();  // positions 152-164

                    if (dateStr6.isEmpty() || valueStr.isBlank()) continue;

                    // Expand 2-digit year to 4-digit (assume 21st century)
                    String dateStr8 = dateStr6.substring(0, 4) + "20" + dateStr6.substring(4);
                    LocalDate date  = LocalDate.parse(dateStr8, DATE_FMT);
                    long amountCents = Long.parseLong(valueStr); // 2 implicit decimal places

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