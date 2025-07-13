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
package com.payments.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.time.LocalDateTime;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for ConversionResult class.
 * Tests all aspects of result creation, data management, and factory methods.
 */
@DisplayName("ConversionResult Tests")
class ConversionResultTest {

    @TempDir
    Path tempDir;

    private File testOutputFile;
    private ConversionResult result;

    @BeforeEach
    void setUp() {
        testOutputFile = tempDir.resolve("test-output.xml").toFile();
        result = new ConversionResult();
    }

    @Nested
    @DisplayName("Constructor and Initialization Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should initialize with default values")
        void shouldInitializeWithDefaultValues() {
            ConversionResult newResult = new ConversionResult();
            
            assertThat(newResult.isSuccess()).isFalse();
            assertThat(newResult.getErrorMessage()).isNull();
            assertThat(newResult.getOutputFile()).isNull();
            assertThat(newResult.getTimestamp()).isNotNull();
            assertThat(newResult.getWarnings()).isNotNull().isEmpty();
            assertThat(newResult.getRecordsProcessed()).isZero();
            assertThat(newResult.getInputFormat()).isNull();
            assertThat(newResult.getOutputFormat()).isNull();
        }

        @Test
        @DisplayName("Should set timestamp close to current time")
        void shouldSetTimestampCloseToCurrentTime() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            ConversionResult newResult = new ConversionResult();
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);
            
            assertThat(newResult.getTimestamp())
                .isAfter(before)
                .isBefore(after);
        }

        @Test
        @DisplayName("Should initialize warnings list as empty but modifiable")
        void shouldInitializeWarningsListAsEmptyButModifiable() {
            ConversionResult newResult = new ConversionResult();
            
            assertThat(newResult.getWarnings()).isEmpty();
            
            // Should be able to add warnings
            assertThatCode(() -> newResult.addWarning("Test warning"))
                .doesNotThrowAnyException();
            
            assertThat(newResult.getWarnings()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Success Factory Method Tests")
    class SuccessFactoryTests {

        @Test
        @DisplayName("Should create successful result with all parameters")
        void shouldCreateSuccessfulResultWithAllParameters() {
            ConversionResult successResult = ConversionResult.success(
                testOutputFile, "MT103", "pain.001.001.03", 5
            );
            
            assertThat(successResult.isSuccess()).isTrue();
            assertThat(successResult.getOutputFile()).isEqualTo(testOutputFile);
            assertThat(successResult.getInputFormat()).isEqualTo("MT103");
            assertThat(successResult.getOutputFormat()).isEqualTo("pain.001.001.03");
            assertThat(successResult.getRecordsProcessed()).isEqualTo(5);
            assertThat(successResult.getErrorMessage()).isNull();
            assertThat(successResult.getTimestamp()).isNotNull();
            assertThat(successResult.getWarnings()).isEmpty();
        }

        @Test
        @DisplayName("Should create successful result for NACHA conversion")
        void shouldCreateSuccessfulResultForNACHAConversion() {
            ConversionResult successResult = ConversionResult.success(
                testOutputFile, "NACHA", "pain.001.001.03", 25
            );
            
            assertThat(successResult.isSuccess()).isTrue();
            assertThat(successResult.getInputFormat()).isEqualTo("NACHA");
            assertThat(successResult.getRecordsProcessed()).isEqualTo(25);
        }

        @Test
        @DisplayName("Should handle zero records processed")
        void shouldHandleZeroRecordsProcessed() {
            ConversionResult successResult = ConversionResult.success(
                testOutputFile, "MT103", "pain.001.001.03", 0
            );
            
            assertThat(successResult.isSuccess()).isTrue();
            assertThat(successResult.getRecordsProcessed()).isZero();
        }

        @Test
        @DisplayName("Should handle null format parameters")
        void shouldHandleNullFormatParameters() {
            ConversionResult successResult = ConversionResult.success(
                testOutputFile, null, null, 1
            );
            
            assertThat(successResult.isSuccess()).isTrue();
            assertThat(successResult.getInputFormat()).isNull();
            assertThat(successResult.getOutputFormat()).isNull();
        }
    }

    @Nested
    @DisplayName("Failure Factory Method Tests")
    class FailureFactoryTests {

        @Test
        @DisplayName("Should create failure result with error message")
        void shouldCreateFailureResultWithErrorMessage() {
            String errorMessage = "File not found: input.mt103";
            ConversionResult failureResult = ConversionResult.failure(errorMessage);
            
            assertThat(failureResult.isSuccess()).isFalse();
            assertThat(failureResult.getErrorMessage()).isEqualTo(errorMessage);
            assertThat(failureResult.getOutputFile()).isNull();
            assertThat(failureResult.getRecordsProcessed()).isZero();
            assertThat(failureResult.getInputFormat()).isNull();
            assertThat(failureResult.getOutputFormat()).isNull();
            assertThat(failureResult.getTimestamp()).isNotNull();
            assertThat(failureResult.getWarnings()).isEmpty();
        }

        @Test
        @DisplayName("Should create failure result with complex error message")
        void shouldCreateFailureResultWithComplexErrorMessage() {
            String complexError = "Parsing failed at line 15: Invalid field format in MT103 message";
            ConversionResult failureResult = ConversionResult.failure(complexError);
            
            assertThat(failureResult.isSuccess()).isFalse();
            assertThat(failureResult.getErrorMessage()).isEqualTo(complexError);
        }

        @Test
        @DisplayName("Should handle null error message")
        void shouldHandleNullErrorMessage() {
            ConversionResult failureResult = ConversionResult.failure(null);
            
            assertThat(failureResult.isSuccess()).isFalse();
            assertThat(failureResult.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("Should handle empty error message")
        void shouldHandleEmptyErrorMessage() {
            ConversionResult failureResult = ConversionResult.failure("");
            
            assertThat(failureResult.isSuccess()).isFalse();
            assertThat(failureResult.getErrorMessage()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Property Getter and Setter Tests")
    class PropertyTests {

        @Test
        @DisplayName("Should set and get success status")
        void shouldSetAndGetSuccessStatus() {
            assertThat(result.isSuccess()).isFalse();
            
            result.setSuccess(true);
            assertThat(result.isSuccess()).isTrue();
            
            result.setSuccess(false);
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("Should set and get error message")
        void shouldSetAndGetErrorMessage() {
            String errorMessage = "Test error message";
            
            result.setErrorMessage(errorMessage);
            assertThat(result.getErrorMessage()).isEqualTo(errorMessage);
            
            result.setErrorMessage(null);
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("Should set and get output file")
        void shouldSetAndGetOutputFile() {
            result.setOutputFile(testOutputFile);
            assertThat(result.getOutputFile()).isEqualTo(testOutputFile);
            
            result.setOutputFile(null);
            assertThat(result.getOutputFile()).isNull();
        }

        @Test
        @DisplayName("Should set and get timestamp")
        void shouldSetAndGetTimestamp() {
            LocalDateTime customTimestamp = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
            
            result.setTimestamp(customTimestamp);
            assertThat(result.getTimestamp()).isEqualTo(customTimestamp);
        }

        @Test
        @DisplayName("Should set and get records processed")
        void shouldSetAndGetRecordsProcessed() {
            result.setRecordsProcessed(42);
            assertThat(result.getRecordsProcessed()).isEqualTo(42);
            
            result.setRecordsProcessed(0);
            assertThat(result.getRecordsProcessed()).isZero();
        }

        @Test
        @DisplayName("Should set and get input format")
        void shouldSetAndGetInputFormat() {
            result.setInputFormat("MT103");
            assertThat(result.getInputFormat()).isEqualTo("MT103");
            
            result.setInputFormat("NACHA");
            assertThat(result.getInputFormat()).isEqualTo("NACHA");
            
            result.setInputFormat(null);
            assertThat(result.getInputFormat()).isNull();
        }

        @Test
        @DisplayName("Should set and get output format")
        void shouldSetAndGetOutputFormat() {
            result.setOutputFormat("pain.001.001.03");
            assertThat(result.getOutputFormat()).isEqualTo("pain.001.001.03");
            
            result.setOutputFormat("pain.008.001.02");
            assertThat(result.getOutputFormat()).isEqualTo("pain.008.001.02");
            
            result.setOutputFormat(null);
            assertThat(result.getOutputFormat()).isNull();
        }
    }

    @Nested
    @DisplayName("Warning Management Tests")
    class WarningTests {

        @Test
        @DisplayName("Should add single warning")
        void shouldAddSingleWarning() {
            String warning = "Missing optional field 70: Remittance Information";
            
            result.addWarning(warning);
            
            assertThat(result.getWarnings())
                .hasSize(1)
                .contains(warning);
        }

        @Test
        @DisplayName("Should add multiple warnings")
        void shouldAddMultipleWarnings() {
            String warning1 = "Missing optional field 70";
            String warning2 = "Using default currency USD";
            String warning3 = "Amount precision reduced";
            
            result.addWarning(warning1);
            result.addWarning(warning2);
            result.addWarning(warning3);
            
            assertThat(result.getWarnings())
                .hasSize(3)
                .containsExactly(warning1, warning2, warning3);
        }

        @Test
        @DisplayName("Should maintain warning order")
        void shouldMaintainWarningOrder() {
            for (int i = 0; i < 5; i++) {
                result.addWarning("Warning " + i);
            }
            
            assertThat(result.getWarnings())
                .hasSize(5)
                .containsExactly("Warning 0", "Warning 1", "Warning 2", "Warning 3", "Warning 4");
        }

        @Test
        @DisplayName("Should handle null warning")
        void shouldHandleNullWarning() {
            // Implementation may accept null or throw exception
            // Testing current behavior - adjust if implementation changes
            assertThatCode(() -> result.addWarning(null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle empty warning")
        void shouldHandleEmptyWarning() {
            result.addWarning("");
            
            assertThat(result.getWarnings())
                .hasSize(1)
                .contains("");
        }

        @Test
        @DisplayName("Should return non-null warnings list even when empty")
        void shouldReturnNonNullWarningsListEvenWhenEmpty() {
            ConversionResult newResult = new ConversionResult();
            
            assertThat(newResult.getWarnings())
                .isNotNull()
                .isEmpty();
        }
    }

    @Nested
    @DisplayName("Complex Scenario Tests")
    class ComplexScenarioTests {

        @Test
        @DisplayName("Should handle successful conversion with warnings")
        void shouldHandleSuccessfulConversionWithWarnings() {
            ConversionResult successWithWarnings = ConversionResult.success(
                testOutputFile, "MT103", "pain.001.001.03", 1
            );
            
            successWithWarnings.addWarning("Missing optional field 70: Remittance Information");
            successWithWarnings.addWarning("Using default charge bearer OUR");
            
            assertThat(successWithWarnings.isSuccess()).isTrue();
            assertThat(successWithWarnings.getWarnings())
                .hasSize(2)
                .contains("Missing optional field 70: Remittance Information")
                .contains("Using default charge bearer OUR");
            assertThat(successWithWarnings.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("Should handle failure with additional context")
        void shouldHandleFailureWithAdditionalContext() {
            ConversionResult failure = ConversionResult.failure("Invalid MT103 format");
            
            // Even failures can have warnings for context
            failure.addWarning("File appeared to be NACHA format instead");
            failure.setInputFormat("MT103"); // What was expected
            
            assertThat(failure.isSuccess()).isFalse();
            assertThat(failure.getErrorMessage()).isEqualTo("Invalid MT103 format");
            assertThat(failure.getWarnings()).contains("File appeared to be NACHA format instead");
            assertThat(failure.getInputFormat()).isEqualTo("MT103");
            assertThat(failure.getOutputFile()).isNull();
        }

        @Test
        @DisplayName("Should handle NACHA conversion with multiple warnings")
        void shouldHandleNACHAConversionWithMultipleWarnings() {
            ConversionResult nachaResult = ConversionResult.success(
                testOutputFile, "NACHA", "pain.001.001.03", 150
            );
            
            nachaResult.addWarning("Batch control totals do not match - using calculated values");
            nachaResult.addWarning("Some addenda records were truncated due to length limits");
            nachaResult.addWarning("Converted 2 return entries to regular entries");
            
            assertThat(nachaResult.isSuccess()).isTrue();
            assertThat(nachaResult.getRecordsProcessed()).isEqualTo(150);
            assertThat(nachaResult.getWarnings()).hasSize(3);
            assertThat(nachaResult.getInputFormat()).isEqualTo("NACHA");
        }

        @Test
        @DisplayName("Should maintain immutability of factory-created results")
        void shouldMaintainImmutabilityOfFactoryCreatedResults() {
            ConversionResult original = ConversionResult.success(
                testOutputFile, "MT103", "pain.001.001.03", 1
            );
            
            LocalDateTime originalTimestamp = original.getTimestamp();
            
            // Modifying the result should not affect the original factory-set values
            original.setSuccess(false);
            original.setRecordsProcessed(999);
            
            // But these should be changeable
            assertThat(original.isSuccess()).isFalse();
            assertThat(original.getRecordsProcessed()).isEqualTo(999);
            
            // These should remain unchanged from factory method
            assertThat(original.getOutputFile()).isEqualTo(testOutputFile);
            assertThat(original.getInputFormat()).isEqualTo("MT103");
            assertThat(original.getOutputFormat()).isEqualTo("pain.001.001.03");
            assertThat(original.getTimestamp()).isEqualTo(originalTimestamp);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very large number of records")
        void shouldHandleVeryLargeNumberOfRecords() {
            int largeRecordCount = Integer.MAX_VALUE;
            
            ConversionResult result = ConversionResult.success(
                testOutputFile, "NACHA", "pain.001.001.03", largeRecordCount
            );
            
            assertThat(result.getRecordsProcessed()).isEqualTo(largeRecordCount);
        }

        @Test
        @DisplayName("Should handle very long format strings")
        void shouldHandleVeryLongFormatStrings() {
            String longFormat = "very.long.format.name.with.many.dots.and.extensions.v1.2.3.final";
            
            ConversionResult result = ConversionResult.success(
                testOutputFile, longFormat, longFormat, 1
            );
            
            assertThat(result.getInputFormat()).isEqualTo(longFormat);
            assertThat(result.getOutputFormat()).isEqualTo(longFormat);
        }

        @Test
        @DisplayName("Should handle very long error messages")
        void shouldHandleVeryLongErrorMessages() {
            String longError = "A very long error message that describes in great detail " +
                "exactly what went wrong during the conversion process and provides " +
                "extensive context about the failure conditions and potential solutions " +
                "for debugging purposes and troubleshooting guidance.".repeat(10);
            
            ConversionResult result = ConversionResult.failure(longError);
            
            assertThat(result.getErrorMessage()).isEqualTo(longError);
        }

        @Test
        @DisplayName("Should handle negative records processed")
        void shouldHandleNegativeRecordsProcessed() {
            ConversionResult result = new ConversionResult();
            
            // Should accept negative values (implementation decision)
            result.setRecordsProcessed(-1);
            assertThat(result.getRecordsProcessed()).isEqualTo(-1);
        }
    }
}