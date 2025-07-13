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

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Logger;

/**
 * Secure XML builder for generating ISO 20022 XML documents.
 * <p>
 * This class provides a secure way to build XML documents using streaming
 * XML processing to prevent XXE attacks and reduce memory consumption.
 * All text content is automatically escaped to prevent XML injection.
 * </p>
 * 
 * <h3>Security Features:</h3>
 * <ul>
 *   <li>Uses secure XMLOutputFactory configuration</li>
 *   <li>Automatic XML character escaping</li>
 *   <li>Streaming processing to reduce memory usage</li>
 *   <li>Input validation for element names and content</li>
 *   <li>Protection against XML injection attacks</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * SecureXMLBuilder builder = new SecureXMLBuilder();
 * builder.startDocument()
 *        .startElement("Document")
 *        .addAttribute("xmlns", "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03")
 *        .startElement("CstmrCdtTrfInitn")
 *        .addElement("MsgId", "MSG123")
 *        .endElement()
 *        .endElement()
 *        .endDocument();
 * String xml = builder.toString();
 * }</pre>
 * 
 * @author Jim Cornacchia
 * @version 1.0.0
 * @since 1.0.0
 */
public class SecureXMLBuilder implements AutoCloseable {
    
    private static final Logger LOGGER = Logger.getLogger(SecureXMLBuilder.class.getName());
    
    private final XMLStreamWriter xmlWriter;
    private final Writer stringWriter;
    private boolean documentStarted = false;
    private boolean documentEnded = false;
    
    /**
     * Creates a new SecureXMLBuilder instance.
     * <p>
     * Initializes the builder with a secure XMLOutputFactory configuration
     * that prevents XXE attacks and other XML-based vulnerabilities.
     * </p>
     * 
     * @throws XMLStreamException if the XML writer cannot be created
     */
    public SecureXMLBuilder() throws XMLStreamException {
        this.stringWriter = new StringWriter();
        
        XMLOutputFactory factory = SecurityUtils.createSecureXMLOutputFactory();
        this.xmlWriter = factory.createXMLStreamWriter(stringWriter);
        
        LOGGER.fine("Created secure XML builder");
    }
    
    /**
     * Starts the XML document with standard declaration.
     * <p>
     * Writes the XML declaration with UTF-8 encoding. This method
     * must be called before adding any elements.
     * </p>
     * 
     * @return this builder for method chaining
     * @throws XMLStreamException if document cannot be started
     * @throws IllegalStateException if document has already been started
     */
    public SecureXMLBuilder startDocument() throws XMLStreamException {
        if (documentStarted) {
            throw new IllegalStateException("Document has already been started");
        }
        
        xmlWriter.writeStartDocument("UTF-8", "1.0");
        xmlWriter.writeCharacters("\n");
        documentStarted = true;
        
        LOGGER.fine("Started XML document");
        return this;
    }
    
    /**
     * Starts an XML element with the specified name.
     * <p>
     * Validates the element name for safety and starts the element.
     * Element names are validated to prevent XML injection attacks.
     * </p>
     * 
     * @param elementName the name of the element to start
     * @return this builder for method chaining
     * @throws XMLStreamException if element cannot be started
     * @throws SecurityException if element name contains invalid characters
     */
    public SecureXMLBuilder startElement(String elementName) throws XMLStreamException {
        validateElementName(elementName);
        
        xmlWriter.writeStartElement(elementName);
        
        LOGGER.fine("Started element: " + SecurityUtils.sanitizeForLogging(elementName));
        return this;
    }
    
    /**
     * Ends the current XML element.
     * <p>
     * Closes the most recently opened element. Elements must be
     * properly nested and closed in reverse order of opening.
     * </p>
     * 
     * @return this builder for method chaining
     * @throws XMLStreamException if element cannot be ended
     */
    public SecureXMLBuilder endElement() throws XMLStreamException {
        xmlWriter.writeEndElement();
        
        LOGGER.fine("Ended element");
        return this;
    }
    
    /**
     * Adds an attribute to the current element.
     * <p>
     * Validates and adds an attribute to the currently open element.
     * Both attribute name and value are validated for security.
     * </p>
     * 
     * @param name the attribute name
     * @param value the attribute value
     * @return this builder for method chaining
     * @throws XMLStreamException if attribute cannot be added
     * @throws SecurityException if name or value contains invalid characters
     */
    public SecureXMLBuilder addAttribute(String name, String value) throws XMLStreamException {
        validateElementName(name);
        validateTextContent(value, "attribute value");
        
        xmlWriter.writeAttribute(name, value);
        
        LOGGER.fine("Added attribute: " + SecurityUtils.sanitizeForLogging(name));
        return this;
    }
    
    /**
     * Adds a complete element with text content.
     * <p>
     * Creates an element with the specified name and text content,
     * then immediately closes it. This is a convenience method for
     * simple text elements.
     * </p>
     * 
     * @param elementName the name of the element
     * @param textContent the text content of the element
     * @return this builder for method chaining
     * @throws XMLStreamException if element cannot be created
     * @throws SecurityException if element name or content contains invalid characters
     */
    public SecureXMLBuilder addElement(String elementName, String textContent) throws XMLStreamException {
        validateElementName(elementName);
        validateTextContent(textContent, elementName);
        
        xmlWriter.writeStartElement(elementName);
        if (textContent != null && !textContent.trim().isEmpty()) {
            xmlWriter.writeCharacters(textContent);
        }
        xmlWriter.writeEndElement();
        
        LOGGER.fine("Added element: " + SecurityUtils.sanitizeForLogging(elementName));
        return this;
    }
    
    /**
     * Adds text content to the current element.
     * <p>
     * Validates and adds text content to the currently open element.
     * The text is automatically escaped for XML safety.
     * </p>
     * 
     * @param text the text content to add
     * @return this builder for method chaining
     * @throws XMLStreamException if text cannot be added
     * @throws SecurityException if text contains invalid characters
     */
    public SecureXMLBuilder addText(String text) throws XMLStreamException {
        validateTextContent(text, "text content");
        
        if (text != null && !text.trim().isEmpty()) {
            xmlWriter.writeCharacters(text);
        }
        
        LOGGER.fine("Added text content");
        return this;
    }
    
    /**
     * Ends the XML document.
     * <p>
     * Finalizes the XML document and flushes all content. This method
     * must be called to complete the document generation.
     * </p>
     * 
     * @return this builder for method chaining
     * @throws XMLStreamException if document cannot be ended
     * @throws IllegalStateException if document was not started or already ended
     */
    public SecureXMLBuilder endDocument() throws XMLStreamException {
        if (!documentStarted) {
            throw new IllegalStateException("Document has not been started");
        }
        if (documentEnded) {
            throw new IllegalStateException("Document has already been ended");
        }
        
        xmlWriter.writeEndDocument();
        xmlWriter.flush();
        documentEnded = true;
        
        LOGGER.fine("Ended XML document");
        return this;
    }
    
    /**
     * Returns the generated XML as a string.
     * <p>
     * Returns the complete XML document as a string. The document
     * should be ended before calling this method.
     * </p>
     * 
     * @return the generated XML document
     * @throws IllegalStateException if document has not been ended
     */
    @Override
    public String toString() {
        if (!documentEnded) {
            LOGGER.warning("Getting XML string before document was properly ended");
        }
        
        return stringWriter.toString();
    }
    
    /**
     * Closes the XML writer and releases resources.
     * <p>
     * Implements AutoCloseable to ensure proper resource cleanup.
     * This method should be called when the builder is no longer needed.
     * </p>
     * 
     * @throws XMLStreamException if the writer cannot be closed
     */
    @Override
    public void close() throws XMLStreamException {
        try {
            if (xmlWriter != null) {
                xmlWriter.close();
            }
        } finally {
            try {
                if (stringWriter != null) {
                    stringWriter.close();
                }
            } catch (Exception e) {
                LOGGER.warning("Error closing string writer: " + e.getMessage());
            }
        }
        
        LOGGER.fine("Closed XML builder");
    }
    
    /**
     * Validates an XML element name for security.
     * <p>
     * Ensures element names follow XML naming rules and don't contain
     * potentially dangerous characters that could lead to injection attacks.
     * </p>
     * 
     * @param elementName the element name to validate
     * @throws SecurityException if the element name is invalid
     */
    private void validateElementName(String elementName) throws SecurityException {
        if (elementName == null || elementName.trim().isEmpty()) {
            throw new SecurityException("Element name cannot be null or empty");
        }
        
        String trimmed = elementName.trim();
        
        // Check for reserved XML names first (per XML 1.0 specification)
        String lower = trimmed.toLowerCase();
        if ((lower.equals("xml") || lower.equals("XML")) || lower.contains(":")) {
            SecurityUtils.logSecurityEvent("Reserved XML element name", elementName);
            throw new SecurityException("Reserved XML element name");
        }
        
        // Check for basic XML naming rules
        if (!trimmed.matches("^[a-zA-Z_][a-zA-Z0-9._-]*$")) {
            SecurityUtils.logSecurityEvent("Invalid XML element name", elementName);
            throw new SecurityException("Invalid XML element name");
        }
    }
    
    /**
     * Validates text content for security.
     * <p>
     * Ensures text content doesn't contain characters or patterns
     * that could be used for injection attacks or cause parsing issues.
     * </p>
     * 
     * @param text the text to validate
     * @param context the context where this text is used (for logging)
     * @throws SecurityException if the text contains invalid content
     */
    private void validateTextContent(String text, String context) throws SecurityException {
        if (text == null) {
            return; // Allow null text
        }
        
        // Check for control characters that could cause issues
        if (text.matches(".*[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F].*")) {
            SecurityUtils.logSecurityEvent("Invalid control characters in text", context);
            throw new SecurityException("Text contains invalid control characters");
        }
        
        // Check for excessively long content
        if (text.length() > 10000) {
            SecurityUtils.logSecurityEvent("Excessively long text content", context);
            throw new SecurityException("Text content exceeds maximum length");
        }
        
        // Validate for common injection patterns
        SecurityUtils.validateInputForInjection(text, context);
    }
}
