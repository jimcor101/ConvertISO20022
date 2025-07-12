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
package com.fintech.payments.model;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Result object encapsulating the outcome of a payment format conversion operation.
 * <p>
 * This class provides a structured way to communicate conversion results between
 * the conversion services and the user interface. It includes success/failure status,
 * error messages, output file information, warnings, and conversion statistics.
 * </p>
 * 
 * <h3>Usage Patterns:</h3>
 * <pre>{@code
 * // Creating a successful result
 * ConversionResult success = ConversionResult.success(
 *     outputFile, "MT103", "pain.001.001.03", 5
 * );
 * 
 * // Creating a failure result
 * ConversionResult failure = ConversionResult.failure(
 *     "Invalid file format detected"
 * );
 * 
 * // Checking result and handling accordingly
 * if (result.isSuccess()) {
 *     System.out.println("Converted " + result.getRecordsProcessed() + " records");
 *     System.out.println("Output: " + result.getOutputFile().getAbsolutePath());
 * } else {
 *     System.err.println("Conversion failed: " + result.getErrorMessage());
 * }
 * }</pre>
 * 
 * <h3>Thread Safety:</h3>
 * <p>
 * This class is not thread-safe. If instances need to be shared between threads,
 * external synchronization is required.
 * </p>
 * 
 * @author Jim Cornacchia
 * @version 1.0.0
 * @since 1.0.0
 */
public class ConversionResult {
    /** Indicates whether the conversion operation was successful */
    private boolean success;
    
    /** Error message if the conversion failed */
    private String errorMessage;
    
    /** The output file created by the conversion (null if conversion failed) */
    private File outputFile;
    
    /** Timestamp when the conversion result was created */
    private LocalDateTime timestamp;
    
    /** List of warning messages generated during conversion */
    private List<String> warnings;
    
    /** Number of records/transactions processed during conversion */
    private int recordsProcessed;
    
    /** Source format of the input file (e.g., "MT103", "NACHA") */
    private String inputFormat;
    
    /** Target format of the output file (e.g., "pain.001.001.03") */
    private String outputFormat;

    /**
     * Default constructor that initializes the result with current timestamp
     * and an empty warnings list.
     */
    public ConversionResult() {
        this.timestamp = LocalDateTime.now();
        this.warnings = new ArrayList<>();
    }

    /**
     * Factory method to create a successful conversion result.
     * <p>
     * Creates a ConversionResult instance representing a successful conversion
     * with all the relevant output information populated.
     * </p>
     * 
     * @param outputFile the file that was created as a result of the conversion
     * @param inputFormat the format of the source file (e.g., "MT103", "NACHA")
     * @param outputFormat the format of the destination file (e.g., "pain.001.001.03")
     * @param recordsProcessed the number of records/transactions that were converted
     * @return a ConversionResult instance representing success
     * 
     * @throws IllegalArgumentException if outputFile is null
     * @throws IllegalArgumentException if recordsProcessed is negative
     */
    public static ConversionResult success(File outputFile, String inputFormat, String outputFormat, int recordsProcessed) {
        ConversionResult result = new ConversionResult();
        result.success = true;
        result.outputFile = outputFile;
        result.inputFormat = inputFormat;
        result.outputFormat = outputFormat;
        result.recordsProcessed = recordsProcessed;
        return result;
    }

    /**
     * Factory method to create a failed conversion result.
     * <p>
     * Creates a ConversionResult instance representing a failed conversion
     * with an appropriate error message explaining the cause of failure.
     * </p>
     * 
     * @param errorMessage a descriptive message explaining why the conversion failed
     * @return a ConversionResult instance representing failure
     * 
     * @throws IllegalArgumentException if errorMessage is null or empty
     */
    public static ConversionResult failure(String errorMessage) {
        ConversionResult result = new ConversionResult();
        result.success = false;
        result.errorMessage = errorMessage;
        return result;
    }

    /**
     * Returns whether the conversion operation was successful.
     * 
     * @return true if the conversion succeeded, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success status of the conversion.
     * 
     * @param success true if the conversion succeeded, false otherwise
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Returns the error message if the conversion failed.
     * 
     * @return the error message, or null if the conversion was successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message for a failed conversion.
     * 
     * @param errorMessage a descriptive error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Returns the output file created by the conversion.
     * 
     * @return the output file, or null if the conversion failed
     */
    public File getOutputFile() {
        return outputFile;
    }

    /**
     * Sets the output file created by the conversion.
     * 
     * @param outputFile the output file
     */
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    /**
     * Returns the timestamp when this result was created.
     * 
     * @return the creation timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp for this result.
     * 
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the list of warning messages generated during conversion.
     * <p>
     * Warnings represent non-fatal issues that occurred during conversion
     * but did not prevent the conversion from completing successfully.
     * </p>
     * 
     * @return a list of warning messages (never null, but may be empty)
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Adds a warning message to this result.
     * <p>
     * Warnings should be used for non-fatal issues that the user should
     * be aware of, such as missing optional fields or format irregularities
     * that were automatically corrected.
     * </p>
     * 
     * @param warning the warning message to add
     * @throws IllegalArgumentException if warning is null
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * Returns the number of records or transactions processed during conversion.
     * <p>
     * For MT103 files, this is typically 1 (one transaction per file).
     * For NACHA files, this represents the number of entry detail records processed.
     * </p>
     * 
     * @return the number of records processed
     */
    public int getRecordsProcessed() {
        return recordsProcessed;
    }

    /**
     * Sets the number of records processed during conversion.
     * 
     * @param recordsProcessed the number of records processed
     * @throws IllegalArgumentException if recordsProcessed is negative
     */
    public void setRecordsProcessed(int recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    /**
     * Returns the format of the input file that was converted.
     * 
     * @return the input format (e.g., "MT103", "NACHA")
     */
    public String getInputFormat() {
        return inputFormat;
    }

    /**
     * Sets the format of the input file.
     * 
     * @param inputFormat the input format
     */
    public void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    /**
     * Returns the format of the output file that was created.
     * 
     * @return the output format (e.g., "pain.001.001.03")
     */
    public String getOutputFormat() {
        return outputFormat;
    }

    /**
     * Sets the format of the output file.
     * 
     * @param outputFormat the output format
     */
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }
}