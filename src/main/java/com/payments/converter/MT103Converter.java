/*
 * ConvertISO20022 - Legacy Payment Format Converter
 * Copyright (C) 2025 Cornacchia Development, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.payments.converter;

import com.payments.model.ConversionResult;
import com.payments.security.SecurityUtils;
import com.payments.security.SecureXMLBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converter for MT103 SWIFT Customer Credit Transfer messages.
 * <p>
 * This class handles the parsing of MT103 SWIFT messages and their conversion
 * to ISO 20022 pain.001.001.03 XML format. MT103 is the standard SWIFT message
 * type used for cross-border wire transfers and customer credit transfers.
 * </p>
 * 
 * <h3>Supported MT103 Fields:</h3>
 * <ul>
 *   <li><strong>Field 20:</strong> Transaction Reference Number</li>
 *   <li><strong>Field 23B:</strong> Bank Operation Code</li>
 *   <li><strong>Field 32A:</strong> Value Date, Currency Code, Amount</li>
 *   <li><strong>Field 50K:</strong> Ordering Customer (Debtor)</li>
 *   <li><strong>Field 59:</strong> Beneficiary Customer (Creditor)</li>
 *   <li><strong>Field 70:</strong> Remittance Information</li>
 *   <li><strong>Field 71A:</strong> Details of Charges</li>
 * </ul>
 * 
 * <h3>MT103 Message Format:</h3>
 * <p>
 * MT103 messages use a structured text format where each field is prefixed
 * with a colon and field number (e.g., :20:, :32A:). This converter uses
 * regular expressions to parse these structured fields.
 * </p>
 * 
 * <h3>Output Format:</h3>
 * <p>
 * Generates ISO 20022 pain.001.001.03 XML with proper namespace declarations
 * and escaping of special characters. The output includes group header,
 * payment information, and credit transfer transaction information.
 * </p>
 * 
 * @author Jim Cornacchia
 * @version 1.0.0
 * @since 1.0.0
 */
public class MT103Converter {

    /**
     * Regular expression pattern for parsing MT103 field structures.
     * <p>
     * Matches field patterns like :20:value, :32A:value, etc.
     * The pattern captures the field code and field value, accounting for
     * multi-line field values using DOTALL flag.
     * </p>
     */
    private static final Pattern FIELD_PATTERN = Pattern.compile(":(\\d+[A-Z]?):(.*?)(?=:\\d+[A-Z]?:|$)", Pattern.DOTALL);

    /**
     * Converts an MT103 SWIFT message file to ISO 20022 XML format.
     * <p>
     * This is the main conversion method that orchestrates the entire process:
     * reading the input file, parsing MT103 fields, generating ISO 20022 XML,
     * and writing the output file. Progress updates are provided throughout
     * the conversion process.
     * </p>
     * 
     * @param inputFile the MT103 file to convert (must exist and be readable)
     * @param outputFile the destination XML file (will be created/overwritten)
     * @param progressCallback callback function to receive progress updates
     * @return ConversionResult indicating success or failure with details
     * 
     * @throws IOException if file reading/writing fails
     * @throws IllegalArgumentException if files are null or invalid
     */
    public ConversionResult convert(File inputFile, File outputFile, Consumer<String> progressCallback) {
        try {
            progressCallback.accept("Validating input file security...");
            
            // Validate input file path and content for security
            Path inputPath = SecurityUtils.validateAndCanonicalizePath(inputFile.getAbsolutePath());
            SecurityUtils.validateFileContent(inputFile);
            
            // Validate output file path
            Path outputPath = SecurityUtils.validateAndCanonicalizePath(outputFile.getAbsolutePath());
            
            // Log security event
            SecurityUtils.logSecurityEvent("MT103 conversion started", 
                "Input: " + inputFile.getName() + ", Output: " + outputFile.getName());
            
            progressCallback.accept("Reading MT103 file: " + inputFile.getName());
            String mt103Content = Files.readString(inputPath);
            
            progressCallback.accept("Parsing MT103 fields...");
            MT103Message mt103 = parseMT103(mt103Content);
            
            progressCallback.accept("Converting to ISO 20022 pain.001.001.03...");
            String iso20022Xml = convertToISO20022(mt103);
            
            progressCallback.accept("Writing output file: " + outputFile.getName());
            Files.writeString(outputPath, iso20022Xml);
            
            // Log successful conversion
            SecurityUtils.logSecurityEvent("MT103 conversion completed successfully", 
                "Output: " + outputFile.getName());
            
            progressCallback.accept("Conversion completed successfully");
            return ConversionResult.success(outputFile, "MT103", "pain.001.001.03", 1);
            
        } catch (SecurityException e) {
            SecurityUtils.logSecurityEvent("MT103 conversion security violation", e.getMessage());
            return ConversionResult.failure(SecurityUtils.sanitizeErrorMessage(e.getMessage()));
        } catch (Exception e) {
            SecurityUtils.logSecurityEvent("MT103 conversion error", e.getClass().getSimpleName());
            return ConversionResult.failure(SecurityUtils.sanitizeErrorMessage(e.getMessage()));
        }
    }

    /**
     * Parses MT103 message content and extracts field values.
     * <p>
     * Uses regular expressions to identify and extract values from MT103 fields.
     * The parser handles the most common MT103 fields required for basic
     * customer credit transfers. Unknown or unsupported fields are ignored.
     * </p>
     * 
     * @param content the complete MT103 message text
     * @return MT103Message object containing parsed field values
     */
    private MT103Message parseMT103(String content) throws SecurityException {
        if (content == null || content.trim().isEmpty()) {
            throw new SecurityException("MT103 content cannot be null or empty");
        }
        
        // Validate input content for security
        SecurityUtils.validateInputForInjection(content, "MT103 content");
        
        MT103Message message = new MT103Message();
        
        Matcher matcher = FIELD_PATTERN.matcher(content);
        while (matcher.find()) {
            String fieldCode = matcher.group(1);
            String fieldValue = matcher.group(2) != null ? matcher.group(2).trim() : "";
            
            // Validate each field for security
            SecurityUtils.validateInputForInjection(fieldValue, "MT103 field " + fieldCode);
            
            switch (fieldCode) {
                case "20":
                    message.transactionReference = fieldValue;
                    break;
                case "23B":
                    message.bankOperationCode = fieldValue;
                    break;
                case "32A":
                    parseValueDateCurrencyAmount(fieldValue, message);
                    break;
                case "50K":
                    message.orderingCustomer = fieldValue;
                    break;
                case "59":
                    message.beneficiaryCustomer = fieldValue;
                    break;
                case "70":
                    message.remittanceInformation = fieldValue;
                    break;
                case "71A":
                    message.detailsOfCharges = fieldValue;
                    break;
            }
        }
        
        return message;
    }

    /**
     * Parses MT103 Field 32A (Value Date/Currency Code/Amount).
     * <p>
     * Field 32A contains three components in a fixed format:
     * - Positions 1-6: Value date (YYMMDD)
     * - Positions 7-9: Currency code (3 characters)
     * - Positions 10+: Amount (variable length decimal)
     * </p>
     * 
     * @param field32A the raw field 32A value
     * @param message the MT103Message to populate with parsed values
     */
    private void parseValueDateCurrencyAmount(String field32A, MT103Message message) {
        if (field32A.length() >= 9) {
            message.valueDate = field32A.substring(0, 6);
            message.currency = field32A.substring(6, 9);
            message.amount = field32A.substring(9);
        }
    }

    /**
     * Converts a parsed MT103 message to ISO 20022 pain.001.001.03 XML format.
     * <p>
     * Generates a complete ISO 20022 XML document with proper namespace
     * declarations, group header, payment information, and credit transfer
     * transaction details. The output conforms to the pain.001.001.03 schema.
     * </p>
     * 
     * @param mt103 the parsed MT103 message data
     * @return complete ISO 20022 XML as a string
     */
    private String convertToISO20022(MT103Message mt103) throws Exception {
        String msgId = "MSG" + System.currentTimeMillis();
        String creationDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        try (SecureXMLBuilder builder = new SecureXMLBuilder()) {
            builder.startDocument()
                   .startElement("Document")
                   .addAttribute("xmlns", "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03")
                   .startElement("CstmrCdtTrfInitn")
                   
                   // Group Header
                   .startElement("GrpHdr")
                   .addElement("MsgId", msgId)
                   .addElement("CreDtTm", creationDateTime)
                   .addElement("NbOfTxs", "1")
                   .addElement("CtrlSum", mt103.amount != null ? mt103.amount : "0")
                   .startElement("InitgPty")
                   .addElement("Nm", "ConvertISO20022")
                   .endElement() // InitgPty
                   .endElement() // GrpHdr
                   
                   // Payment Information
                   .startElement("PmtInf")
                   .addElement("PmtInfId", mt103.transactionReference != null ? mt103.transactionReference : "PMT001")
                   .addElement("PmtMtd", "TRF")
                   .addElement("NbOfTxs", "1")
                   .addElement("CtrlSum", mt103.amount != null ? mt103.amount : "0")
                   .addElement("ReqdExctnDt", formatDate(mt103.valueDate))
                   
                   // Debtor
                   .startElement("Dbtr")
                   .addElement("Nm", mt103.orderingCustomer != null ? mt103.orderingCustomer : "Unknown Debtor")
                   .endElement() // Dbtr
                   
                   // Debtor Account
                   .startElement("DbtrAcct")
                   .startElement("Id")
                   .startElement("Othr")
                   .addElement("Id", "UNKNOWN")
                   .endElement() // Othr
                   .endElement() // Id
                   .endElement() // DbtrAcct
                   
                   // Credit Transfer Transaction Information
                   .startElement("CdtTrfTxInf")
                   .startElement("PmtId")
                   .addElement("EndToEndId", mt103.transactionReference != null ? mt103.transactionReference : "E2E001")
                   .endElement() // PmtId
                   
                   // Amount
                   .startElement("Amt")
                   .startElement("InstdAmt")
                   .addAttribute("Ccy", mt103.currency != null ? mt103.currency : "USD")
                   .addText(mt103.amount != null ? mt103.amount : "0")
                   .endElement() // InstdAmt
                   .endElement() // Amt
                   
                   // Creditor
                   .startElement("Cdtr")
                   .addElement("Nm", mt103.beneficiaryCustomer != null ? mt103.beneficiaryCustomer : "Unknown Creditor")
                   .endElement() // Cdtr
                   
                   // Creditor Account
                   .startElement("CdtrAcct")
                   .startElement("Id")
                   .startElement("Othr")
                   .addElement("Id", "UNKNOWN")
                   .endElement() // Othr
                   .endElement() // Id
                   .endElement(); // CdtrAcct
            
            // Add remittance information if present
            if (mt103.remittanceInformation != null && !mt103.remittanceInformation.trim().isEmpty()) {
                builder.startElement("RmtInf")
                       .addElement("Ustrd", mt103.remittanceInformation)
                       .endElement(); // RmtInf
            }
            
            builder.endElement() // CdtTrfTxInf
                   .endElement() // PmtInf
                   .endElement() // CstmrCdtTrfInitn
                   .endElement() // Document
                   .endDocument();
            
            return builder.toString();
        }
    }

    /**
     * Converts MT103 date format (YYMMDD) to ISO 8601 format (YYYY-MM-DD).
     * <p>
     * MT103 dates are in YYMMDD format where YY represents a 2-digit year.
     * This method assumes years 00-99 represent 2000-2099. If the input
     * date is invalid or null, the current date is returned.
     * </p>
     * 
     * @param mt103Date the MT103 date string in YYMMDD format
     * @return ISO 8601 formatted date string (YYYY-MM-DD)
     */
    private String formatDate(String mt103Date) {
        if (mt103Date == null || mt103Date.length() != 6) {
            return LocalDateTime.now().toLocalDate().toString();
        }
        
        try {
            String year = "20" + mt103Date.substring(0, 2);
            String month = mt103Date.substring(2, 4);
            String day = mt103Date.substring(4, 6);
            return year + "-" + month + "-" + day;
        } catch (Exception e) {
            return LocalDateTime.now().toLocalDate().toString();
        }
    }


    /**
     * Internal data structure representing a parsed MT103 message.
     * <p>
     * This class holds the extracted field values from an MT103 SWIFT message
     * in a structured format that can be easily converted to ISO 20022 XML.
     * All fields are optional as not every MT103 message contains all fields.
     * </p>
     */
    private static class MT103Message {
        /** Field 20: Transaction Reference Number */
        String transactionReference;
        
        /** Field 23B: Bank Operation Code */
        String bankOperationCode;
        
        /** Field 32A: Value Date (YYMMDD format) */
        String valueDate;
        
        /** Field 32A: Currency Code (3 characters) */
        String currency;
        
        /** Field 32A: Transaction Amount */
        String amount;
        
        /** Field 50K: Ordering Customer information */
        String orderingCustomer;
        
        /** Field 59: Beneficiary Customer information */
        String beneficiaryCustomer;
        
        /** Field 70: Remittance Information */
        String remittanceInformation;
        
        /** Field 71A: Details of Charges */
        String detailsOfCharges;
    }
}
