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

import java.io.File;
import java.util.function.Consumer;

/**
 * Central service for orchestrating payment format conversions.
 * <p>
 * This service acts as a facade for the various format-specific converters,
 * implementing the Strategy pattern to delegate conversion tasks to appropriate
 * converter implementations based on the input format.
 * </p>
 * 
 * <h3>Supported Formats:</h3>
 * <ul>
 *   <li><strong>MT103:</strong> SWIFT Customer Credit Transfer messages</li>
 *   <li><strong>NACHA:</strong> Automated Clearing House (ACH) files</li>
 * </ul>
 * 
 * <h3>Output Format:</h3>
 * <p>
 * All conversions produce ISO 20022 XML in the pain.001.001.03 format
 * (Customer Credit Transfer Initiation).
 * </p>
 * 
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * ConversionService service = new ConversionService();
 * File inputFile = new File("payment.mt103");
 * File outputFile = new File("converted.xml");
 * 
 * ConversionResult result = service.convertFile(
 *     inputFile, "MT103", outputFile, 
 *     progress -> System.out.println(progress)
 * );
 * 
 * if (result.isSuccess()) {
 *     System.out.println("Conversion completed: " + result.getOutputFile());
 * } else {
 *     System.err.println("Conversion failed: " + result.getErrorMessage());
 * }
 * }</pre>
 * 
 * @author Jim Cornacchia
 * @version 1.0.0
 * @since 1.0.0
 */
public class ConversionService {

    /**
     * Converts a payment file from legacy format to ISO 20022 XML.
     * <p>
     * This is the main entry point for file conversion. The method determines
     * the appropriate converter based on the input format and delegates the
     * conversion process while providing real-time progress updates.
     * </p>
     * 
     * @param inputFile the source file to convert (must exist and be readable)
     * @param inputFormat the format of the input file ("MT103" or "NACHA")
     * @param outputFile the destination file for the converted XML (will be created/overwritten)
     * @param progressCallback callback function to receive progress updates during conversion
     * @return ConversionResult containing success/failure status and details
     * 
     * @throws IllegalArgumentException if inputFile is null or doesn't exist
     * @throws IllegalArgumentException if outputFile is null
     * @throws IllegalArgumentException if inputFormat is null or unsupported
     * @throws IllegalArgumentException if progressCallback is null
     */
    public ConversionResult convertFile(File inputFile, String inputFormat, File outputFile, Consumer<String> progressCallback) {
        try {
            progressCallback.accept("Starting conversion from " + inputFormat + " to ISO 20022");
            
            if ("MT103".equals(inputFormat)) {
                return convertMT103(inputFile, outputFile, progressCallback);
            } else if ("NACHA".equals(inputFormat)) {
                return convertNACHA(inputFile, outputFile, progressCallback);
            } else {
                return ConversionResult.failure("Unsupported input format: " + inputFormat);
            }
            
        } catch (Exception e) {
            return ConversionResult.failure("Conversion failed: " + e.getMessage());
        }
    }

    /**
     * Handles MT103 SWIFT message conversion.
     * <p>
     * Delegates the conversion to the specialized MT103Converter while
     * providing error handling and progress reporting.
     * </p>
     * 
     * @param inputFile the MT103 file to convert
     * @param outputFile the destination XML file
     * @param progressCallback callback for progress updates
     * @return ConversionResult with conversion outcome
     */
    private ConversionResult convertMT103(File inputFile, File outputFile, Consumer<String> progressCallback) {
        try {
            progressCallback.accept("Parsing MT103 file...");
            
            MT103Converter converter = new MT103Converter();
            return converter.convert(inputFile, outputFile, progressCallback);
            
        } catch (Exception e) {
            return ConversionResult.failure("MT103 conversion failed: " + e.getMessage());
        }
    }

    /**
     * Handles NACHA ACH file conversion.
     * <p>
     * Delegates the conversion to the specialized NACHAConverter while
     * providing error handling and progress reporting.
     * </p>
     * 
     * @param inputFile the NACHA file to convert
     * @param outputFile the destination XML file
     * @param progressCallback callback for progress updates
     * @return ConversionResult with conversion outcome
     */
    private ConversionResult convertNACHA(File inputFile, File outputFile, Consumer<String> progressCallback) {
        try {
            progressCallback.accept("Parsing NACHA file...");
            
            NACHAConverter converter = new NACHAConverter();
            return converter.convert(inputFile, outputFile, progressCallback);
            
        } catch (Exception e) {
            return ConversionResult.failure("NACHA conversion failed: " + e.getMessage());
        }
    }
}