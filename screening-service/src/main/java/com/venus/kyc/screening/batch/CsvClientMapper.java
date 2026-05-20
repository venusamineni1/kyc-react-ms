package com.venus.kyc.screening.batch;

import com.venus.kyc.screening.batch.model.Client;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Maps a single Apache Commons CSV {@link CSVRecord} (header-based) to a {@link Client} record.
 *
 * <p>Expected CSV headers (case-sensitive, matching Client field names):
 * <pre>
 * clientId, titlePrefix, firstName, middleName, lastName, titleSuffix,
 * citizenship1, citizenship2, onboardingDate, status, nameAtBirth, nickName,
 * gender, dateOfBirth, language, occupation, countryOfTax, sourceOfFundsCountry,
 * fatcaStatus, crsStatus, addressLine1, city, zipCode, province, country,
 * nationality, legDocType, idNumber, placeOfBirth, cityOfBirth, countryOfBirth
 * </pre>
 * Date fields accept ISO-8601 format: {@code yyyy-MM-dd}.
 */
@Component
public class CsvClientMapper {

    private static final Logger log = LoggerFactory.getLogger(CsvClientMapper.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    public Client toClient(CSVRecord record) {
        return new Client(
                parseLong(record, "clientId"),
                get(record, "titlePrefix"),
                get(record, "firstName"),
                get(record, "middleName"),
                get(record, "lastName"),
                get(record, "titleSuffix"),
                get(record, "citizenship1"),
                get(record, "citizenship2"),
                parseDate(record, "onboardingDate"),
                get(record, "status"),
                get(record, "nameAtBirth"),
                get(record, "nickName"),
                get(record, "gender"),
                parseDate(record, "dateOfBirth"),
                get(record, "language"),
                get(record, "occupation"),
                get(record, "countryOfTax"),
                get(record, "sourceOfFundsCountry"),
                get(record, "fatcaStatus"),
                get(record, "crsStatus"),
                get(record, "addressLine1"),
                get(record, "city"),
                get(record, "zipCode"),
                get(record, "province"),
                get(record, "country"),
                get(record, "nationality"),
                get(record, "legDocType"),
                get(record, "idNumber"),
                get(record, "placeOfBirth"),
                get(record, "cityOfBirth"),
                get(record, "countryOfBirth")
        );
    }

    private String get(CSVRecord record, String header) {
        try {
            String val = record.get(header);
            return (val == null || val.isBlank()) ? null : val.trim();
        } catch (IllegalArgumentException e) {
            // Column not present in this CSV — return null gracefully
            log.debug("CSV column '{}' not found in record #{}", header, record.getRecordNumber());
            return null;
        }
    }

    private Long parseLong(CSVRecord record, String header) {
        String val = get(record, header);
        if (val == null) return null;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            log.warn("Invalid long value '{}' for column '{}' at record #{}", val, header, record.getRecordNumber());
            return null;
        }
    }

    private LocalDate parseDate(CSVRecord record, String header) {
        String val = get(record, header);
        if (val == null) return null;
        try {
            return LocalDate.parse(val, DATE_FMT);
        } catch (DateTimeParseException e) {
            log.warn("Invalid date value '{}' for column '{}' at record #{}", val, header, record.getRecordNumber());
            return null;
        }
    }
}
