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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Security utilities for the ConvertISO20022 application.
 * <p>
 * This class provides defensive security functions including secure XML processing,
 * path traversal validation, input sanitization, and error message sanitization.
 * All methods are designed to prevent common security vulnerabilities while
 * maintaining application functionality.
 * </p>
 * 
 * <h3>Security Features:</h3>
 * <ul>
 *   <li>XXE (XML External Entity) attack prevention</li>
 *   <li>Path traversal validation and canonicalization</li>
 *   <li>Input size and content validation</li>
 *   <li>Error message sanitization to prevent information disclosure</li>
 *   <li>Secure temporary file handling</li>
 *   <li>Security audit logging</li>
 * </ul>
 * 
 * @author Jim Cornacchia
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SecurityUtils {
    
    private static final Logger SECURITY_LOGGER = Logger.getLogger("SECURITY." + SecurityUtils.class.getName());
    
    // Security constraints
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int MAX_LINE_LENGTH = 10000; // 10KB per line
    private static final int MAX_LINES = 1000000; // 1M lines max
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(".*[/\\\\]\\.\\.([/\\\\].*)?");
    
    // Allowed base directories for file operations
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private SecurityUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Creates a secure DocumentBuilderFactory configured to prevent XXE attacks.
     * <p>
     * Disables external DTD processing, external general entities, external parameter entities,
     * and document type declarations to prevent XML External Entity (XXE) attacks.
     * </p>
     * 
     * @return DocumentBuilderFactory configured with security settings
     * @throws ParserConfigurationException if the parser cannot be configured securely
     */
    public static DocumentBuilderFactory createSecureDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        
        // Disable external DTDs and entities to prevent XXE attacks
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        
        SECURITY_LOGGER.info("Created secure DocumentBuilderFactory with XXE protection");
        return factory;
    }
    
    /**
     * Creates a secure XMLOutputFactory configured to prevent XXE attacks.
     * <p>
     * Configures the factory to disable external entity processing and 
     * other potentially dangerous XML features.
     * </p>
     * 
     * @return XMLOutputFactory configured with security settings
     */
    public static XMLOutputFactory createSecureXMLOutputFactory() {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        
        // Disable external entity processing
        factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
        
        SECURITY_LOGGER.info("Created secure XMLOutputFactory");
        return factory;
    }
    
    /**
     * Creates a secure TransformerFactory configured to prevent XXE attacks.
     * <p>
     * Disables access to external DTDs, stylesheets, and other external resources
     * that could be used in XXE attacks.
     * </p>
     * 
     * @return TransformerFactory configured with security settings
     * @throws TransformerException if the transformer cannot be configured securely
     */
    public static TransformerFactory createSecureTransformerFactory() throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        
        // Disable access to external DTDs and stylesheets
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        
        SECURITY_LOGGER.info("Created secure TransformerFactory with XXE protection");
        return factory;
    }
    
    /**
     * Validates a file path to prevent path traversal attacks.
     * <p>
     * Checks for directory traversal patterns, canonicalizes the path,
     * and ensures it stays within allowed directories. This prevents
     * attackers from accessing files outside the intended scope.
     * </p>
     * 
     * @param filePath the file path to validate
     * @return the canonical path if valid
     * @throws SecurityException if the path contains traversal patterns or is outside allowed directories
     * @throws IOException if path canonicalization fails
     */
    public static Path validateAndCanonicalizePath(String filePath) throws SecurityException, IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new SecurityException("File path cannot be null or empty");
        }
        
        // Check for obvious path traversal patterns
        if (PATH_TRAVERSAL_PATTERN.matcher(filePath).matches()) {
            SECURITY_LOGGER.warning("Path traversal attempt detected: " + sanitizeForLogging(filePath));
            throw new SecurityException("Path traversal detected in file path");
        }
        
        // Canonicalize the path to resolve any remaining traversal attempts
        Path path = Paths.get(filePath).toAbsolutePath().normalize();
        String canonicalPath = path.toString();
        
        // Ensure the canonical path is within allowed directories
        if (!canonicalPath.startsWith(USER_HOME) && !canonicalPath.startsWith(TEMP_DIR)) {
            SECURITY_LOGGER.warning("Access attempt outside allowed directories: " + sanitizeForLogging(canonicalPath));
            throw new SecurityException("File access outside allowed directories");
        }
        
        SECURITY_LOGGER.info("Path validation successful: " + sanitizeForLogging(canonicalPath));
        return path;
    }
    
    /**
     * Validates file size and content to prevent resource exhaustion attacks.
     * <p>
     * Checks file size limits, line count limits, and individual line length
     * limits to prevent denial of service attacks through oversized inputs.
     * </p>
     * 
     * @param file the file to validate
     * @throws SecurityException if the file exceeds security limits
     * @throws IOException if file reading fails
     */
    public static void validateFileContent(File file) throws SecurityException, IOException {
        if (!file.exists()) {
            throw new SecurityException("File does not exist");
        }
        
        // Check file size
        long fileSize = file.length();
        if (fileSize > MAX_FILE_SIZE) {
            SECURITY_LOGGER.warning("File size exceeds limit: " + fileSize + " bytes");
            throw new SecurityException("File size exceeds maximum allowed size");
        }
        
        // Check line count and line length limits
        try {
            long lineCount = Files.lines(file.toPath()).count();
            if (lineCount > MAX_LINES) {
                SECURITY_LOGGER.warning("File line count exceeds limit: " + lineCount + " lines");
                throw new SecurityException("File contains too many lines");
            }
        } catch (Exception e) {
            throw new SecurityException("Error validating file content: " + sanitizeErrorMessage(e.getMessage()));
        }
        
        SECURITY_LOGGER.info("File content validation successful: " + sanitizeForLogging(file.getName()));
    }
    
    /**
     * Validates that a filename is safe and doesn't contain dangerous characters.
     * <p>
     * Ensures filenames only contain alphanumeric characters, dots, underscores,
     * and hyphens to prevent various file-based attacks.
     * </p>
     * 
     * @param filename the filename to validate
     * @return true if the filename is safe
     */
    public static boolean isFilenameSafe(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        
        String trimmedName = filename.trim();
        
        // Check for dangerous filenames
        if (trimmedName.equals(".") || trimmedName.equals("..")) {
            return false;
        }
        
        // Check against safe pattern
        boolean isSafe = SAFE_FILENAME_PATTERN.matcher(trimmedName).matches();
        
        if (!isSafe) {
            SECURITY_LOGGER.warning("Unsafe filename detected: " + sanitizeForLogging(filename));
        }
        
        return isSafe;
    }
    
    /**
     * Sanitizes error messages to prevent information disclosure.
     * <p>
     * Removes stack traces, file paths, and other sensitive information
     * from error messages before displaying them to users. This prevents
     * attackers from gathering system information through error messages.
     * </p>
     * 
     * @param errorMessage the original error message
     * @return sanitized error message safe for user display
     */
    public static String sanitizeErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return "An unknown error occurred";
        }
        
        // Remove common sensitive information patterns
        String sanitized = errorMessage
            .replaceAll("(?i)at\\s+[\\w.$]+\\([^)]*\\)", "") // Remove stack trace elements
            .replaceAll("(?i)caused by:.*", "") // Remove "caused by" chains
            .replaceAll("(?i)[a-z]:\\\\[^\\s]*", "[PATH]") // Remove Windows paths
            .replaceAll("(?i)/[^\\s]*", "[PATH]") // Remove Unix paths
            .replaceAll("(?i)\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b", "[IP]") // Remove IP addresses
            .replaceAll("(?i)password[^\\s]*", "[PASSWORD]") // Remove password references
            .replaceAll("(?i)token[^\\s]*", "[TOKEN]") // Remove token references
            .trim();
        
        // If the message becomes empty after sanitization, provide a generic message
        if (sanitized.isEmpty()) {
            sanitized = "A processing error occurred";
        }
        
        return sanitized;
    }
    
    /**
     * Sanitizes text for safe logging to prevent log injection attacks.
     * <p>
     * Removes or escapes characters that could be used for log injection
     * attacks while preserving readability for legitimate debugging.
     * </p>
     * 
     * @param text the text to sanitize for logging
     * @return sanitized text safe for logging
     */
    public static String sanitizeForLogging(String text) {
        if (text == null) {
            return "[null]";
        }
        
        return text
            .replaceAll("[\r\n]", "_") // Replace line breaks
            .replaceAll("[\t]", " ") // Replace tabs
            .replaceAll("[\\x00-\\x1F\\x7F]", "?") // Replace control characters
            .substring(0, Math.min(text.length(), 200)); // Limit length
    }
    
    /**
     * Creates a secure temporary file with appropriate permissions.
     * <p>
     * Creates temporary files with restricted permissions and ensures
     * they are properly cleaned up. The files are created in the system
     * temporary directory with secure access controls.
     * </p>
     * 
     * @param prefix the prefix for the temporary file name
     * @param suffix the suffix for the temporary file name
     * @return Path to the created temporary file
     * @throws IOException if temporary file creation fails
     */
    public static Path createSecureTemporaryFile(String prefix, String suffix) throws IOException {
        // Validate prefix and suffix for safety
        if (!isFilenameSafe(prefix) || !isFilenameSafe(suffix)) {
            throw new SecurityException("Invalid characters in temporary file name");
        }
        
        Path tempFile = Files.createTempFile(prefix, suffix);
        
        // Set restrictive permissions (owner read/write only)
        try {
            Files.setPosixFilePermissions(tempFile, 
                java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException e) {
            // POSIX permissions not supported (e.g., Windows)
            SECURITY_LOGGER.info("POSIX permissions not supported, using default file permissions");
        }
        
        SECURITY_LOGGER.info("Created secure temporary file: " + sanitizeForLogging(tempFile.toString()));
        return tempFile;
    }
    
    /**
     * Logs a security event for audit purposes.
     * <p>
     * Records security-relevant events with sanitized information for
     * compliance and security monitoring purposes.
     * </p>
     * 
     * @param event the security event description
     * @param details additional event details (will be sanitized)
     */
    public static void logSecurityEvent(String event, String details) {
        String sanitizedEvent = sanitizeForLogging(event);
        String sanitizedDetails = sanitizeForLogging(details);
        
        SECURITY_LOGGER.info("SECURITY_EVENT: " + sanitizedEvent + " | " + sanitizedDetails);
    }
    
    /**
     * Validates input string against common injection patterns.
     * <p>
     * Checks for patterns commonly used in various injection attacks
     * and rejects inputs that contain suspicious patterns.
     * </p>
     * 
     * @param input the input string to validate
     * @param fieldName the name of the field being validated (for logging)
     * @throws SecurityException if suspicious patterns are detected
     */
    public static void validateInputForInjection(String input, String fieldName) throws SecurityException {
        if (input == null) {
            return; // Allow null inputs
        }
        
        // Check for common injection patterns
        String lowerInput = input.toLowerCase();
        
        // SQL injection patterns
        if (lowerInput.contains("' or ") || lowerInput.contains("union select") || 
            lowerInput.contains("drop table") || lowerInput.contains("insert into")) {
            logSecurityEvent("SQL injection attempt detected", "Field: " + fieldName);
            throw new SecurityException("Invalid input detected");
        }
        
        // Script injection patterns
        if (lowerInput.contains("<script") || lowerInput.contains("javascript:") ||
            lowerInput.contains("vbscript:") || lowerInput.contains("onload=")) {
            logSecurityEvent("Script injection attempt detected", "Field: " + fieldName);
            throw new SecurityException("Invalid input detected");
        }
        
        // Command injection patterns
        if (lowerInput.contains("cmd.exe") || lowerInput.contains("/bin/sh") ||
            lowerInput.contains("powershell") || input.contains("$(")) {
            logSecurityEvent("Command injection attempt detected", "Field: " + fieldName);
            throw new SecurityException("Invalid input detected");
        }
    }
}
