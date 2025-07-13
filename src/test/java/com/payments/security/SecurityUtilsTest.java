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
package com.payments.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.transform.TransformerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for SecurityUtils class.
 * Tests all security validation functions including XML factory creation,
 * path validation, file content validation, input sanitization, and error handling.
 */
@DisplayName("SecurityUtils Tests")
class SecurityUtilsTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("XML Factory Creation Tests")
    class XMLFactoryTests {

        @Test
        @DisplayName("Should create secure DocumentBuilderFactory")
        void shouldCreateSecureDocumentBuilderFactory() throws Exception {
            DocumentBuilderFactory factory = SecurityUtils.createSecureDocumentBuilderFactory();
            
            assertThat(factory).isNotNull();
            // Verify XXE protection features are enabled
            assertThat(factory.getFeature("http://apache.org/xml/features/disallow-doctype-decl")).isTrue();
            assertThat(factory.getFeature("http://xml.org/sax/features/external-general-entities")).isFalse();
            assertThat(factory.getFeature("http://xml.org/sax/features/external-parameter-entities")).isFalse();
            assertThat(factory.getFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd")).isFalse();
            assertThat(factory.isXIncludeAware()).isFalse();
            assertThat(factory.isExpandEntityReferences()).isFalse();
        }

        @Test
        @DisplayName("Should create secure XMLOutputFactory")
        void shouldCreateSecureXMLOutputFactory() {
            XMLOutputFactory factory = SecurityUtils.createSecureXMLOutputFactory();
            
            assertThat(factory).isNotNull();
            assertThat(factory.getProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES)).isEqualTo(true);
        }

        @Test
        @DisplayName("Should create secure TransformerFactory")
        void shouldCreateSecureTransformerFactory() throws Exception {
            TransformerFactory factory = SecurityUtils.createSecureTransformerFactory();
            
            assertThat(factory).isNotNull();
            // Verify secure processing is enabled
            assertThat(factory.getFeature("http://javax.xml.XMLConstants/feature/secure-processing")).isTrue();
        }
    }

    @Nested
    @DisplayName("Path Validation Tests")
    class PathValidationTests {

        @Test
        @DisplayName("Should validate and canonicalize valid path")
        void shouldValidateValidPath() throws Exception {
            Path testFile = tempDir.resolve("test.txt");
            Files.createFile(testFile);
            
            Path result = SecurityUtils.validateAndCanonicalizePath(testFile.toString());
            
            assertThat(result).isNotNull();
            assertThat(result.toAbsolutePath().normalize()).isEqualTo(testFile.toAbsolutePath().normalize());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", "   "})
        @DisplayName("Should reject null or empty paths")
        void shouldRejectNullOrEmptyPaths(String path) {
            assertThatThrownBy(() -> SecurityUtils.validateAndCanonicalizePath(path))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("File path cannot be null or empty");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "../../../etc/passwd",
            "..\\..\\..\\windows\\system32\\config\\sam",
            "/test/../../../etc/shadow",
            "C:\\test\\..\\..\\..\\windows\\system32"
        })
        @DisplayName("Should detect path traversal attempts")
        void shouldDetectPathTraversalAttempts(String maliciousPath) {
            assertThatThrownBy(() -> SecurityUtils.validateAndCanonicalizePath(maliciousPath))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Path traversal detected");
        }
    }

    @Nested
    @DisplayName("File Content Validation Tests")
    class FileContentValidationTests {

        @Test
        @DisplayName("Should validate normal file successfully")
        void shouldValidateNormalFile() throws Exception {
            Path testFile = tempDir.resolve("normal.txt");
            Files.write(testFile, "This is a normal file with reasonable content.".getBytes());
            
            assertThatCode(() -> SecurityUtils.validateFileContent(testFile.toFile()))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject non-existent file")
        void shouldRejectNonExistentFile() {
            File nonExistentFile = new File(tempDir.toFile(), "does-not-exist.txt");
            
            assertThatThrownBy(() -> SecurityUtils.validateFileContent(nonExistentFile))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("File does not exist");
        }

        @Test
        @DisplayName("Should reject file exceeding size limit")
        void shouldRejectOversizedFile() throws Exception {
            Path largeFile = tempDir.resolve("large.txt");
            // Create a file larger than 50MB limit
            byte[] largeContent = new byte[51 * 1024 * 1024]; // 51MB
            Files.write(largeFile, largeContent);
            
            assertThatThrownBy(() -> SecurityUtils.validateFileContent(largeFile.toFile()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("File size exceeds maximum allowed size");
        }
    }

    @Nested
    @DisplayName("Filename Safety Tests")
    class FilenameSafetyTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "normal-file.txt",
            "file_name.xml",
            "test123.json",
            "my-file-2024.csv"
        })
        @DisplayName("Should accept safe filenames")
        void shouldAcceptSafeFilenames(String filename) {
            assertThat(SecurityUtils.isFilenameSafe(filename)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "file with spaces.txt",
            "file/with/slashes.txt",
            "file\\with\\backslashes.txt",
            "file<with>brackets.txt",
            "file|with|pipes.txt",
            "file\"with\"quotes.txt",
            "file:with:colons.txt",
            "file*with*asterisks.txt",
            "file?with?questions.txt"
        })
        @DisplayName("Should reject unsafe filenames")
        void shouldRejectUnsafeFilenames(String filename) {
            assertThat(SecurityUtils.isFilenameSafe(filename)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {".", ".."})
        @DisplayName("Should reject dangerous directory references")
        void shouldRejectDangerousDirectoryReferences(String filename) {
            assertThat(SecurityUtils.isFilenameSafe(filename)).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Should reject null, empty, or whitespace-only filenames")
        void shouldRejectNullOrEmptyFilenames(String filename) {
            assertThat(SecurityUtils.isFilenameSafe(filename)).isFalse();
        }
    }

    @Nested
    @DisplayName("Error Message Sanitization Tests")
    class ErrorSanitizationTests {

        @Test
        @DisplayName("Should sanitize stack traces from error messages")
        void shouldSanitizeStackTraces() {
            String errorWithStackTrace = "Error occurred at com.example.Class.method(Class.java:123) " +
                "at com.example.Other.otherMethod(Other.java:456)";
            
            String sanitized = SecurityUtils.sanitizeErrorMessage(errorWithStackTrace);
            
            assertThat(sanitized).doesNotContain("at com.example");
            assertThat(sanitized).doesNotContain("Class.java:123");
        }

        @Test
        @DisplayName("Should sanitize file paths from error messages")
        void shouldSanitizeFilePaths() {
            String errorWithPaths = "Could not read file /home/user/secret/file.txt or C:\\Users\\User\\Documents\\sensitive.doc";
            
            String sanitized = SecurityUtils.sanitizeErrorMessage(errorWithPaths);
            
            assertThat(sanitized).contains("[PATH]");
            assertThat(sanitized).doesNotContain("/home/user/secret");
            assertThat(sanitized).doesNotContain("C:\\Users\\User");
        }

        @Test
        @DisplayName("Should sanitize IP addresses from error messages")
        void shouldSanitizeIPAddresses() {
            String errorWithIP = "Connection failed to 192.168.1.100 and 10.0.0.1";
            
            String sanitized = SecurityUtils.sanitizeErrorMessage(errorWithIP);
            
            assertThat(sanitized).contains("[IP]");
            assertThat(sanitized).doesNotContain("192.168.1.100");
            assertThat(sanitized).doesNotContain("10.0.0.1");
        }

        @Test
        @DisplayName("Should sanitize password references from error messages")
        void shouldSanitizePasswordReferences() {
            String errorWithPassword = "Authentication failed for password123 and passwordABC";
            
            String sanitized = SecurityUtils.sanitizeErrorMessage(errorWithPassword);
            
            assertThat(sanitized).contains("[PASSWORD]");
            assertThat(sanitized).doesNotContain("password123");
        }

        @Test
        @DisplayName("Should handle null error message")
        void shouldHandleNullErrorMessage() {
            String sanitized = SecurityUtils.sanitizeErrorMessage(null);
            
            assertThat(sanitized).isEqualTo("An unknown error occurred");
        }

        @Test
        @DisplayName("Should provide generic message for empty sanitized result")
        void shouldProvideGenericMessageForEmptyResult() {
            String errorWithOnlyStackTrace = "at com.example.Class.method(Class.java:123)";
            
            String sanitized = SecurityUtils.sanitizeErrorMessage(errorWithOnlyStackTrace);
            
            assertThat(sanitized).isEqualTo("A processing error occurred");
        }
    }

    @Nested
    @DisplayName("Logging Sanitization Tests")
    class LoggingSanitizationTests {

        @Test
        @DisplayName("Should sanitize text for safe logging")
        void shouldSanitizeTextForLogging() {
            String textWithControlChars = "Normal text\nwith\tnewlines\rand\u0001control\u007fchars";
            
            String sanitized = SecurityUtils.sanitizeForLogging(textWithControlChars);
            
            assertThat(sanitized).doesNotContain("\n");
            assertThat(sanitized).doesNotContain("\r");
            assertThat(sanitized).doesNotContain("\t");
            assertThat(sanitized).doesNotContain("\u0001");
            assertThat(sanitized).doesNotContain("\u007f");
        }

        @Test
        @DisplayName("Should limit text length for logging")
        void shouldLimitTextLengthForLogging() {
            String longText = "a".repeat(300);
            
            String sanitized = SecurityUtils.sanitizeForLogging(longText);
            
            assertThat(sanitized).hasSizeLessThanOrEqualTo(200);
        }

        @Test
        @DisplayName("Should handle null text for logging")
        void shouldHandleNullTextForLogging() {
            String sanitized = SecurityUtils.sanitizeForLogging(null);
            
            assertThat(sanitized).isEqualTo("[null]");
        }
    }

    @Nested
    @DisplayName("Input Injection Validation Tests")
    class InputInjectionValidationTests {

        @Test
        @DisplayName("Should accept clean input")
        void shouldAcceptCleanInput() {
            String cleanInput = "This is normal payment data with amounts 123.45";
            
            assertThatCode(() -> SecurityUtils.validateInputForInjection(cleanInput, "test-field"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow null input")
        void shouldAllowNullInput() {
            assertThatCode(() -> SecurityUtils.validateInputForInjection(null, "test-field"))
                .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "'; DROP TABLE users; --",
            "' OR '1'='1",
            "UNION SELECT * FROM passwords",
            "INSERT INTO users VALUES",
            "DROP TABLE payments"
        })
        @DisplayName("Should detect SQL injection attempts")
        void shouldDetectSQLInjectionAttempts(String maliciousInput) {
            assertThatThrownBy(() -> SecurityUtils.validateInputForInjection(maliciousInput, "test-field"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Invalid input detected");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "<script>alert('xss')</script>",
            "javascript:alert('xss')",
            "vbscript:msgbox('xss')",
            "<img onload=alert('xss')>"
        })
        @DisplayName("Should detect script injection attempts")
        void shouldDetectScriptInjectionAttempts(String maliciousInput) {
            assertThatThrownBy(() -> SecurityUtils.validateInputForInjection(maliciousInput, "test-field"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Invalid input detected");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "cmd.exe /c dir",
            "/bin/sh -c ls",
            "powershell.exe Get-Process",
            "$(rm -rf /)"
        })
        @DisplayName("Should detect command injection attempts")
        void shouldDetectCommandInjectionAttempts(String maliciousInput) {
            assertThatThrownBy(() -> SecurityUtils.validateInputForInjection(maliciousInput, "test-field"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Invalid input detected");
        }
    }

    @Nested
    @DisplayName("Temporary File Creation Tests")
    class TemporaryFileTests {

        @Test
        @DisplayName("Should create secure temporary file")
        void shouldCreateSecureTemporaryFile() throws Exception {
            Path tempFile = SecurityUtils.createSecureTemporaryFile("test", ".tmp");
            
            assertThat(tempFile).exists();
            assertThat(tempFile.getFileName().toString()).startsWith("test");
            assertThat(tempFile.getFileName().toString()).endsWith(".tmp");
            
            // Clean up
            Files.deleteIfExists(tempFile);
        }

        @ParameterizedTest
        @ValueSource(strings = {"file with spaces", "file/with/slashes", "file<>bad"})
        @DisplayName("Should reject unsafe temporary file names")
        void shouldRejectUnsafeTemporaryFileNames(String unsafeName) {
            assertThatThrownBy(() -> SecurityUtils.createSecureTemporaryFile(unsafeName, ".tmp"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Invalid characters in temporary file name");
        }
    }

    @Nested
    @DisplayName("Security Event Logging Tests")
    class SecurityEventLoggingTests {

        @Test
        @DisplayName("Should log security event without throwing exception")
        void shouldLogSecurityEvent() {
            assertThatCode(() -> SecurityUtils.logSecurityEvent("Test security event", "Additional details"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null parameters in security logging")
        void shouldHandleNullParametersInSecurityLogging() {
            assertThatCode(() -> SecurityUtils.logSecurityEvent(null, null))
                .doesNotThrowAnyException();
        }
    }
}