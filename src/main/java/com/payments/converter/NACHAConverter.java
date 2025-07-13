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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Converter for NACHA (National Automated Clearing House Association) ACH files.
 * <p>
 * This class handles the parsing of NACHA formatted ACH files and their conversion
 * to ISO 20022 pain.001.001.03 XML format. NACHA files are fixed-width record-based
 * files used for electronic payments in the United States banking system.
 * </p>
 * 
 * <h3>Supported NACHA Record Types:</h3>
 * <ul>
 *   <li><strong>Type 1:</strong> File Header Record - Contains file-level information</li>
 *   <li><strong>Type 5:</strong> Batch Header Record - Contains batch-level information</li>
 *   <li><strong>Type 6:</strong> Entry Detail Record - Contains individual transaction details</li>
 *   <li><strong>Type 7:</strong> Addenda Record - Contains additional transaction information</li>
 *   <li><strong>Type 8:</strong> Batch Control Record - Contains batch totals and counts</li>
 *   <li><strong>Type 9:</strong> File Control Record - Contains file totals and counts</li>
 * </ul>
 * 
 * <h3>NACHA File Structure:</h3>
 * <p>
 * NACHA files follow a strict 94-character fixed-width format where each record type
 * has a specific layout. Records are organized hierarchically: File Header → Batch Header →
 * Entry Details (with optional Addenda) → Batch Control → File Control.
 * </p>
 * 
 * <h3>Output Format:</h3>
 * <p>
 * Generates ISO 20022 pain.001.001.03 XML that can contain multiple credit transfer
 * transactions from the NACHA entry detail records. Amounts are converted from
 * cents (NACHA format) to decimal currency format (ISO 20022).
 * </p>
 * 
 * @author Jim Cornacchia
 * @version 1.0.0
 * @since 1.0.0
 */
public class NACHAConverter {

    /**
     * Converts a NACHA ACH file to ISO 20022 XML format.
     * <p>
     * This is the main conversion method that orchestrates the entire process:
     * reading the input file line by line, parsing NACHA records according to
     * their type, generating ISO 20022 XML with multiple transactions, and
     * writing the output file. Progress updates are provided throughout.
     * </p>
     * 
     * @param inputFile the NACHA file to convert (must exist and be readable)
     * @param outputFile the destination XML file (will be created/overwritten)
     * @param progressCallback callback function to receive progress updates
     * @return ConversionResult indicating success or failure with transaction count
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
            SecurityUtils.logSecurityEvent("NACHA conversion started", 
                "Input: " + inputFile.getName() + ", Output: " + outputFile.getName());
            
            progressCallback.accept("Reading NACHA file: " + inputFile.getName());
            List<String> lines = Files.readAllLines(inputPath);
            
            progressCallback.accept("Parsing NACHA records...");
            NACHABatch batch = parseNACHA(lines, progressCallback);
            
            progressCallback.accept("Converting to ISO 20022...");
            String iso20022Xml = convertToISO20022(batch);
            
            progressCallback.accept("Writing output file: " + outputFile.getName());
            Files.writeString(outputPath, iso20022Xml);
            
            // Log successful conversion
            SecurityUtils.logSecurityEvent("NACHA conversion completed successfully", 
                "Output: " + outputFile.getName() + ", Records: " + batch.entryDetails.size());
            
            progressCallback.accept("Conversion completed successfully");
            return ConversionResult.success(outputFile, "NACHA", "pain.001.001.03", batch.entryDetails.size());
            
        } catch (SecurityException e) {
            SecurityUtils.logSecurityEvent("NACHA conversion security violation", e.getMessage());
            return ConversionResult.failure(SecurityUtils.sanitizeErrorMessage(e.getMessage()));
        } catch (Exception e) {
            SecurityUtils.logSecurityEvent("NACHA conversion error", e.getClass().getSimpleName());
            return ConversionResult.failure(SecurityUtils.sanitizeErrorMessage(e.getMessage()));
        }
    }

    /**
     * Parses NACHA file content and extracts record data.
     * <p>
     * Processes each line of the NACHA file according to its record type
     * (identified by the first character). Each record type has a specific
     * format and is parsed accordingly. Invalid or short lines are skipped.
     * </p>
     * 
     * @param lines the complete NACHA file content as a list of lines
     * @param progressCallback callback for progress updates during parsing
     * @return NACHABatch object containing all parsed record data
     */
    private NACHABatch parseNACHA(List<String> lines, Consumer<String> progressCallback) {
        NACHABatch batch = new NACHABatch();
        
        for (String line : lines) {
            if (line.length() < 1) continue;
            
            String recordType = line.substring(0, 1);
            
            switch (recordType) {
                case "1":
                    progressCallback.accept("Processing file header record");
                    parseFileHeader(line, batch);
                    break;
                case "5":
                    progressCallback.accept("Processing batch header record");
                    parseBatchHeader(line, batch);
                    break;
                case "6":
                    progressCallback.accept("Processing entry detail record");
                    parseEntryDetail(line, batch);
                    break;
                case "7":
                    progressCallback.accept("Processing addenda record");
                    parseAddenda(line, batch);
                    break;
                case "8":
                    progressCallback.accept("Processing batch control record");
                    parseBatchControl(line, batch);
                    break;
                case "9":
                    progressCallback.accept("Processing file control record");
                    parseFileControl(line, batch);
                    break;
            }
        }
        
        return batch;
    }

    /**
     * Parses NACHA File Header Record (Type 1).
     * <p>
     * Extracts file-level information including creation date/time,
     * immediate destination, and immediate origin from fixed positions
     * in the 94-character record.
     * </p>
     * 
     * @param line the file header record line
     * @param batch the NACHABatch to populate with header data
     */
    private void parseFileHeader(String line, NACHABatch batch) {
        if (line.length() >= 94) {
            batch.fileCreationDate = line.substring(23, 29);
            batch.fileCreationTime = line.substring(29, 33);
            batch.immediateDestination = line.substring(3, 13).trim();
            batch.immediateOrigin = line.substring(13, 23).trim();
        }
    }

    /**
     * Parses NACHA Batch Header Record (Type 5).
     * <p>
     * Extracts batch-level information including service class code,
     * company information, standard entry class code, and effective date
     * from fixed positions in the record.
     * </p>
     * 
     * @param line the batch header record line
     * @param batch the NACHABatch to populate with batch header data
     */
    private void parseBatchHeader(String line, NACHABatch batch) {
        if (line.length() >= 94) {
            batch.serviceClassCode = line.substring(1, 4);
            batch.companyName = line.substring(4, 20).trim();
            batch.companyIdentification = line.substring(40, 50).trim();
            batch.standardEntryClassCode = line.substring(50, 53);
            batch.companyEntryDescription = line.substring(53, 63).trim();
            batch.effectiveEntryDate = line.substring(69, 75);
            batch.originatingDFIIdentification = line.substring(79, 87);
        }
    }

    /**
     * Parses NACHA Entry Detail Record (Type 6).
     * <p>
     * Extracts individual transaction details including transaction code,
     * receiving DFI information, account number, amount, and individual
     * name from fixed positions. Creates a new NACHAEntryDetail object
     * and adds it to the batch.
     * </p>
     * 
     * @param line the entry detail record line
     * @param batch the NACHABatch to add the entry detail to
     */
    private void parseEntryDetail(String line, NACHABatch batch) {
        if (line.length() >= 94) {
            NACHAEntryDetail entry = new NACHAEntryDetail();
            entry.transactionCode = line.substring(1, 3);
            entry.receivingDFIIdentification = line.substring(3, 11);
            entry.checkDigit = line.substring(11, 12);
            entry.dfiAccountNumber = line.substring(12, 29).trim();
            entry.amount = line.substring(29, 39);
            entry.individualIdentificationNumber = line.substring(39, 54).trim();
            entry.individualName = line.substring(54, 76).trim();
            entry.discretionaryData = line.substring(76, 78);
            entry.addendaRecordIndicator = line.substring(78, 79);
            entry.traceNumber = line.substring(79, 94);
            
            batch.entryDetails.add(entry);
        }
    }

    /**
     * Parses NACHA Addenda Record (Type 7).
     * <p>
     * Extracts additional information associated with the most recent
     * entry detail record. Addenda records provide supplementary data
     * such as remittance information or additional transaction details.
     * Multiple addenda records are concatenated.
     * </p>
     * 
     * @param line the addenda record line
     * @param batch the NACHABatch containing entry details to augment
     */
    private void parseAddenda(String line, NACHABatch batch) {
        if (!batch.entryDetails.isEmpty() && line.length() >= 94) {
            NACHAEntryDetail lastEntry = batch.entryDetails.get(batch.entryDetails.size() - 1);
            if (lastEntry.addendaInformation == null) {
                lastEntry.addendaInformation = "";
            }
            lastEntry.addendaInformation += line.substring(4, 84).trim() + " ";
        }
    }

    /**
     * Parses NACHA Batch Control Record (Type 8).
     * <p>
     * Extracts batch totals and counts including entry/addenda count,
     * entry hash, and total debit/credit amounts. These values are used
     * for file integrity verification and reconciliation.
     * </p>
     * 
     * @param line the batch control record line
     * @param batch the NACHABatch to populate with control totals
     */
    private void parseBatchControl(String line, NACHABatch batch) {
        if (line.length() >= 94) {
            batch.entryAddendaCount = line.substring(4, 10);
            batch.entryHash = line.substring(10, 20);
            batch.totalDebitEntryDollarAmount = line.substring(20, 32);
            batch.totalCreditEntryDollarAmount = line.substring(32, 44);
        }
    }

    /**
     * Parses NACHA File Control Record (Type 9).
     * <p>
     * Extracts file-level totals including batch count and block count.
     * This is the final record in a NACHA file and provides overall
     * file statistics for validation.
     * </p>
     * 
     * @param line the file control record line
     * @param batch the NACHABatch to populate with file control data
     */
    private void parseFileControl(String line, NACHABatch batch) {
        if (line.length() >= 94) {
            batch.batchCount = line.substring(1, 7);
            batch.blockCount = line.substring(7, 13);
        }
    }

    /**
     * Converts a parsed NACHA batch to ISO 20022 pain.001.001.03 XML format.
     * <p>
     * Generates a complete ISO 20022 XML document with group header containing
     * batch summary information and individual credit transfer transactions
     * for each entry detail record. Amounts are converted from NACHA cent
     * format to decimal currency format.
     * </p>
     * 
     * @param batch the parsed NACHA batch data
     * @return complete ISO 20022 XML as a string
     */
    private String convertToISO20022(NACHABatch batch) {
        String msgId = "NACHA" + System.currentTimeMillis();
        String creationDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.001.001.03\">\n");
        xml.append("  <CstmrCdtTrfInitn>\n");
        xml.append("    <GrpHdr>\n");
        xml.append("      <MsgId>").append(msgId).append("</MsgId>\n");
        xml.append("      <CreDtTm>").append(creationDateTime).append("</CreDtTm>\n");
        xml.append("      <NbOfTxs>").append(batch.entryDetails.size()).append("</NbOfTxs>\n");
        xml.append("      <CtrlSum>").append(calculateTotalAmount(batch)).append("</CtrlSum>\n");
        xml.append("      <InitgPty>\n");
        xml.append("        <Nm>").append(escapeXml(batch.companyName != null ? batch.companyName : "Unknown Company")).append("</Nm>\n");
        xml.append("        <Id>\n");
        xml.append("          <OrgId>\n");
        xml.append("            <Othr>\n");
        xml.append("              <Id>").append(batch.companyIdentification != null ? batch.companyIdentification : "UNKNOWN").append("</Id>\n");
        xml.append("            </Othr>\n");
        xml.append("          </OrgId>\n");
        xml.append("        </Id>\n");
        xml.append("      </InitgPty>\n");
        xml.append("    </GrpHdr>\n");
        xml.append("    <PmtInf>\n");
        xml.append("      <PmtInfId>").append(batch.companyIdentification != null ? batch.companyIdentification : "PMT001").append("</PmtInfId>\n");
        xml.append("      <PmtMtd>TRF</PmtMtd>\n");
        xml.append("      <NbOfTxs>").append(batch.entryDetails.size()).append("</NbOfTxs>\n");
        xml.append("      <CtrlSum>").append(calculateTotalAmount(batch)).append("</CtrlSum>\n");
        xml.append("      <ReqdExctnDt>").append(formatNACHADate(batch.effectiveEntryDate)).append("</ReqdExctnDt>\n");
        xml.append("      <Dbtr>\n");
        xml.append("        <Nm>").append(escapeXml(batch.companyName != null ? batch.companyName : "Unknown Debtor")).append("</Nm>\n");
        xml.append("      </Dbtr>\n");
        xml.append("      <DbtrAcct>\n");
        xml.append("        <Id>\n");
        xml.append("          <Othr>\n");
        xml.append("            <Id>").append(batch.originatingDFIIdentification != null ? batch.originatingDFIIdentification : "UNKNOWN").append("</Id>\n");
        xml.append("          </Othr>\n");
        xml.append("        </Id>\n");
        xml.append("      </DbtrAcct>\n");

        for (int i = 0; i < batch.entryDetails.size(); i++) {
            NACHAEntryDetail entry = batch.entryDetails.get(i);
            xml.append("      <CdtTrfTxInf>\n");
            xml.append("        <PmtId>\n");
            xml.append("          <EndToEndId>").append(entry.traceNumber != null ? entry.traceNumber : "E2E" + String.format("%03d", i + 1)).append("</EndToEndId>\n");
            xml.append("        </PmtId>\n");
            xml.append("        <Amt>\n");
            xml.append("          <InstdAmt Ccy=\"USD\">").append(formatAmount(entry.amount)).append("</InstdAmt>\n");
            xml.append("        </Amt>\n");
            xml.append("        <Cdtr>\n");
            xml.append("          <Nm>").append(escapeXml(entry.individualName != null ? entry.individualName : "Unknown Creditor")).append("</Nm>\n");
            xml.append("        </Cdtr>\n");
            xml.append("        <CdtrAcct>\n");
            xml.append("          <Id>\n");
            xml.append("            <Othr>\n");
            xml.append("              <Id>").append(entry.dfiAccountNumber != null ? entry.dfiAccountNumber : "UNKNOWN").append("</Id>\n");
            xml.append("            </Othr>\n");
            xml.append("          </Id>\n");
            xml.append("        </CdtrAcct>\n");
            
            if (entry.addendaInformation != null && !entry.addendaInformation.trim().isEmpty()) {
                xml.append("        <RmtInf>\n");
                xml.append("          <Ustrd>").append(escapeXml(entry.addendaInformation.trim())).append("</Ustrd>\n");
                xml.append("        </RmtInf>\n");
            }
            
            xml.append("      </CdtTrfTxInf>\n");
        }

        xml.append("    </PmtInf>\n");
        xml.append("  </CstmrCdtTrfInitn>\n");
        xml.append("</Document>");
        
        return xml.toString();
    }

    /**
     * Calculates the total amount for all entry details in the batch.
     * <p>
     * Sums all individual transaction amounts, converting from NACHA
     * cent format to decimal currency format. Invalid amounts are
     * skipped to prevent processing errors.
     * </p>
     * 
     * @param batch the NACHA batch containing entry details
     * @return total amount as a decimal currency string (e.g., "1234.56")
     */
    private String calculateTotalAmount(NACHABatch batch) {
        long totalCents = 0;
        for (NACHAEntryDetail entry : batch.entryDetails) {
            if (entry.amount != null) {
                try {
                    totalCents += Long.parseLong(entry.amount);
                } catch (NumberFormatException e) {
                    // Skip invalid amounts
                }
            }
        }
        return String.format("%.2f", totalCents / 100.0);
    }

    /**
     * Formats a NACHA amount (in cents) to decimal currency format.
     * <p>
     * NACHA amounts are stored as integers representing cents.
     * This method converts them to decimal format with two decimal places
     * (e.g., 12345 becomes "123.45"). Invalid amounts default to "0.00".
     * </p>
     * 
     * @param nachaAmount the NACHA amount string in cents
     * @return formatted amount as decimal currency string
     */
    private String formatAmount(String nachaAmount) {
        if (nachaAmount == null || nachaAmount.trim().isEmpty()) {
            return "0.00";
        }
        try {
            long cents = Long.parseLong(nachaAmount.trim());
            return String.format("%.2f", cents / 100.0);
        } catch (NumberFormatException e) {
            return "0.00";
        }
    }

    /**
     * Converts NACHA date format (YYMMDD) to ISO 8601 format (YYYY-MM-DD).
     * <p>
     * NACHA dates are in YYMMDD format where YY represents a 2-digit year.
     * This method assumes years 00-99 represent 2000-2099. If the input
     * date is invalid or null, the current date is returned.
     * </p>
     * 
     * @param nachaDate the NACHA date string in YYMMDD format
     * @return ISO 8601 formatted date string (YYYY-MM-DD)
     */
    private String formatNACHADate(String nachaDate) {
        if (nachaDate == null || nachaDate.length() != 6) {
            return LocalDateTime.now().toLocalDate().toString();
        }
        
        try {
            String year = "20" + nachaDate.substring(0, 2);
            String month = nachaDate.substring(2, 4);
            String day = nachaDate.substring(4, 6);
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
     * errors when the NACHA data contains characters that have special meaning in XML.
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
     * Internal data structure representing a parsed NACHA batch.
     * <p>
     * This class holds the extracted data from all NACHA record types
     * in a structured format that can be easily converted to ISO 20022 XML.
     * Contains both header/control information and individual entry details.
     * </p>
     */
    private static class NACHABatch {
        // File Header Record (Type 1) fields
        /** File creation date (YYMMDD) */
        String fileCreationDate;
        /** File creation time (HHMM) */
        String fileCreationTime;
        /** Immediate destination (receiving institution) */
        String immediateDestination;
        /** Immediate origin (sending institution) */
        String immediateOrigin;
        
        // Batch Header Record (Type 5) fields
        /** Service class code (e.g., 200, 220, 225) */
        String serviceClassCode;
        /** Company name */
        String companyName;
        /** Company identification number */
        String companyIdentification;
        /** Standard entry class code (e.g., PPD, CCD, WEB) */
        String standardEntryClassCode;
        /** Company entry description */
        String companyEntryDescription;
        /** Effective entry date (YYMMDD) */
        String effectiveEntryDate;
        /** Originating DFI identification */
        String originatingDFIIdentification;
        
        // Batch Control Record (Type 8) fields
        /** Entry and addenda count */
        String entryAddendaCount;
        /** Entry hash (sum of receiving DFI identification numbers) */
        String entryHash;
        /** Total debit entry dollar amount */
        String totalDebitEntryDollarAmount;
        /** Total credit entry dollar amount */
        String totalCreditEntryDollarAmount;
        
        // File Control Record (Type 9) fields
        /** Batch count */
        String batchCount;
        /** Block count */
        String blockCount;
        
        /** List of individual entry detail records */
        List<NACHAEntryDetail> entryDetails = new ArrayList<>();
    }

    /**
     * Internal data structure representing a NACHA entry detail record.
     * <p>
     * This class holds the data from Type 6 (Entry Detail) records along
     * with any associated addenda information from Type 7 records.
     * Each entry detail represents an individual ACH transaction.
     * </p>
     */
    private static class NACHAEntryDetail {
        /** Transaction code (e.g., 22=credit, 27=debit) */
        String transactionCode;
        /** Receiving DFI (bank) identification number */
        String receivingDFIIdentification;
        /** Check digit for receiving DFI identification */
        String checkDigit;
        /** DFI account number (customer account) */
        String dfiAccountNumber;
        /** Transaction amount in cents */
        String amount;
        /** Individual identification number */
        String individualIdentificationNumber;
        /** Individual/company name */
        String individualName;
        /** Discretionary data */
        String discretionaryData;
        /** Addenda record indicator (0=no addenda, 1=addenda follows) */
        String addendaRecordIndicator;
        /** Trace number (unique transaction identifier) */
        String traceNumber;
        /** Additional information from addenda records */
        String addendaInformation;
    }
}
