/*
 * Copyright (c) 2024 FinTech Payments
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fintech.payments.converter;

import com.fintech.payments.model.ConversionResult;

import java.io.*;
import java.nio.file.Files;
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
            progressCallback.accept("Reading MT103 file: " + inputFile.getName());
            
            String mt103Content = Files.readString(inputFile.toPath());
            
            progressCallback.accept("Parsing MT103 fields...");
            MT103Message mt103 = parseMT103(mt103Content);
            
            progressCallback.accept("Converting to ISO 20022 pain.001.001.03...");
            String iso20022Xml = convertToISO20022(mt103);
            
            progressCallback.accept("Writing output file: " + outputFile.getName());
            Files.writeString(outputFile.toPath(), iso20022Xml);
            
            progressCallback.accept("Conversion completed successfully");
            return ConversionResult.success(outputFile, "MT103", "pain.001.001.03", 1);
            
        } catch (Exception e) {
            return ConversionResult.failure("MT103 conversion error: " + e.getMessage());
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
    private MT103Message parseMT103(String content) {
        MT103Message message = new MT103Message();
        
        Matcher matcher = FIELD_PATTERN.matcher(content);
        while (matcher.find()) {
            String fieldCode = matcher.group(1);
            String fieldValue = matcher.group(2).trim();
            
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
    private String convertToISO20022(MT103Message mt103) {
        String msgId = "MSG" + System.currentTimeMillis();
        String creationDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.001.001.03\">\n");
        xml.append("  <CstmrCdtTrfInitn>\n");
        xml.append("    <GrpHdr>\n");
        xml.append("      <MsgId>").append(msgId).append("</MsgId>\n");
        xml.append("      <CreDtTm>").append(creationDateTime).append("</CreDtTm>\n");
        xml.append("      <NbOfTxs>1</NbOfTxs>\n");
        xml.append("      <CtrlSum>").append(mt103.amount != null ? mt103.amount : "0").append("</CtrlSum>\n");
        xml.append("      <InitgPty>\n");
        xml.append("        <Nm>ConvertISO20022</Nm>\n");
        xml.append("      </InitgPty>\n");
        xml.append("    </GrpHdr>\n");
        xml.append("    <PmtInf>\n");
        xml.append("      <PmtInfId>").append(mt103.transactionReference != null ? mt103.transactionReference : "PMT001").append("</PmtInfId>\n");
        xml.append("      <PmtMtd>TRF</PmtMtd>\n");
        xml.append("      <NbOfTxs>1</NbOfTxs>\n");
        xml.append("      <CtrlSum>").append(mt103.amount != null ? mt103.amount : "0").append("</CtrlSum>\n");
        xml.append("      <ReqdExctnDt>").append(formatDate(mt103.valueDate)).append("</ReqdExctnDt>\n");
        xml.append("      <Dbtr>\n");
        xml.append("        <Nm>").append(escapeXml(mt103.orderingCustomer != null ? mt103.orderingCustomer : "Unknown Debtor")).append("</Nm>\n");
        xml.append("      </Dbtr>\n");
        xml.append("      <DbtrAcct>\n");
        xml.append("        <Id>\n");
        xml.append("          <Othr>\n");
        xml.append("            <Id>UNKNOWN</Id>\n");
        xml.append("          </Othr>\n");
        xml.append("        </Id>\n");
        xml.append("      </DbtrAcct>\n");
        xml.append("      <CdtTrfTxInf>\n");
        xml.append("        <PmtId>\n");
        xml.append("          <EndToEndId>").append(mt103.transactionReference != null ? mt103.transactionReference : "E2E001").append("</EndToEndId>\n");
        xml.append("        </PmtId>\n");
        xml.append("        <Amt>\n");
        xml.append("          <InstdAmt Ccy=\"").append(mt103.currency != null ? mt103.currency : "USD").append("\">").append(mt103.amount != null ? mt103.amount : "0").append("</InstdAmt>\n");
        xml.append("        </Amt>\n");
        xml.append("        <Cdtr>\n");
        xml.append("          <Nm>").append(escapeXml(mt103.beneficiaryCustomer != null ? mt103.beneficiaryCustomer : "Unknown Creditor")).append("</Nm>\n");
        xml.append("        </Cdtr>\n");
        xml.append("        <CdtrAcct>\n");
        xml.append("          <Id>\n");
        xml.append("            <Othr>\n");
        xml.append("              <Id>UNKNOWN</Id>\n");
        xml.append("            </Othr>\n");
        xml.append("          </Id>\n");
        xml.append("        </CdtrAcct>\n");
        
        if (mt103.remittanceInformation != null && !mt103.remittanceInformation.trim().isEmpty()) {
            xml.append("        <RmtInf>\n");
            xml.append("          <Ustrd>").append(escapeXml(mt103.remittanceInformation)).append("</Ustrd>\n");
            xml.append("        </RmtInf>\n");
        }
        
        xml.append("      </CdtTrfTxInf>\n");
        xml.append("    </PmtInf>\n");
        xml.append("  </CstmrCdtTrfInitn>\n");
        xml.append("</Document>");
        
        return xml.toString();
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
     * Escapes special characters for XML content.
     * <p>
     * Replaces XML special characters with their corresponding entity references
     * to ensure valid XML output. This is essential for preventing XML parsing
     * errors when the MT103 data contains characters that have special meaning in XML.
     * </p>
     * 
     * @param text the text to escape (may be null)
     * @return escaped text safe for XML content, or empty string if input is null
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&apos;");
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