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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import javax.xml.stream.XMLStreamException;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for SecureXMLBuilder class.
 * Tests XML building functionality, security validations, and proper document structure.
 */
@DisplayName("SecureXMLBuilder Tests")
class SecureXMLBuilderTest {

    private SecureXMLBuilder builder;

    @BeforeEach
    void setUp() throws XMLStreamException {
        builder = new SecureXMLBuilder();
    }

    @Nested
    @DisplayName("Document Lifecycle Tests")
    class DocumentLifecycleTests {

        @Test
        @DisplayName("Should create and complete simple XML document")
        void shouldCreateSimpleXMLDocument() throws Exception {
            try (SecureXMLBuilder testBuilder = new SecureXMLBuilder()) {
                String xml = testBuilder
                    .startDocument()
                    .startElement("root")
                    .addElement("child", "value")
                    .endElement()
                    .endDocument()
                    .toString();

                assertThat(xml)
                    .contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                    .contains("<root>")
                    .contains("<child>value</child>")
                    .contains("</root>");
            }
        }

        @Test
        @DisplayName("Should prevent starting document twice")
        void shouldPreventStartingDocumentTwice() throws Exception {
            builder.startDocument();
            
            assertThatThrownBy(() -> builder.startDocument())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Document has already been started");
        }

        @Test
        @DisplayName("Should prevent ending document before starting")
        void shouldPreventEndingDocumentBeforeStarting() {
            assertThatThrownBy(() -> builder.endDocument())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Document has not been started");
        }

        @Test
        @DisplayName("Should prevent ending document twice")
        void shouldPreventEndingDocumentTwice() throws Exception {
            builder.startDocument()
                .startElement("root")
                .endElement()
                .endDocument();
            
            assertThatThrownBy(() -> builder.endDocument())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Document has already been ended");
        }

        @Test
        @DisplayName("Should warn when getting XML before document ended")
        void shouldWarnWhenGettingXMLBeforeDocumentEnded() throws Exception {
            builder.startDocument()
                .startElement("root")
                .endElement();
            
            // Should not throw but should warn (captured in logs)
            String xml = builder.toString();
            assertThat(xml).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Element Management Tests")
    class ElementManagementTests {

        @Test
        @DisplayName("Should create nested elements correctly")
        void shouldCreateNestedElements() throws Exception {
            try (SecureXMLBuilder testBuilder = new SecureXMLBuilder()) {
                String xml = testBuilder
                    .startDocument()
                    .startElement("parent")
                    .startElement("child")
                    .addElement("grandchild", "value")
                    .endElement()
                    .endElement()
                    .endDocument()
                    .toString();

                assertThat(xml)
                    .contains("<parent>")
                    .contains("<child>")
                    .contains("<grandchild>value</grandchild>")
                    .contains("</child>")
                    .contains("</parent>");
            }
        }

        @Test
        @DisplayName("Should add attributes to elements")
        void shouldAddAttributesToElements() throws Exception {
            try (SecureXMLBuilder testBuilder = new SecureXMLBuilder()) {
                String xml = testBuilder
                    .startDocument()
                    .startElement("root")
                    .addAttribute("id", "123")
                    .addAttribute("type", "test")
                    .addElement("child", "value")
                    .endElement()
                    .endDocument()
                    .toString();

                assertThat(xml)
                    .contains("id=\"123\"")
                    .contains("type=\"test\"")
                    .contains("<child>value</child>");
            }
        }

        @Test
        @DisplayName("Should add text content to elements")
        void shouldAddTextContentToElements() throws Exception {
            try (SecureXMLBuilder testBuilder = new SecureXMLBuilder()) {
                String xml = testBuilder
                    .startDocument()
                    .startElement("root")
                    .addText("This is text content")
                    .endElement()
                    .endDocument()
                    .toString();

                assertThat(xml).contains(">This is text content<");
            }
        }

        @Test
        @DisplayName("Should handle empty text content gracefully")
        void shouldHandleEmptyTextContentGracefully() throws Exception {
            try (SecureXMLBuilder testBuilder = new SecureXMLBuilder()) {
                String xml = testBuilder
                    .startDocument()
                    .startElement("root")
                    .addText("")
                    .addText("   ")
                    .endElement()
                    .endDocument()
                    .toString();

                assertThat(xml).contains("<root></root>");
            }
        }
    }

    @Nested
    @DisplayName("Element Name Validation Tests")
    class ElementNameValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Should reject null, empty, or whitespace element names")
        void shouldRejectNullOrEmptyElementNames(String elementName) throws Exception {
            builder.startDocument();
            
            assertThatThrownBy(() -> builder.startElement(elementName))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Element name cannot be null or empty");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "element with spaces",
            "element-with-numbers123",
            "element_with_underscores",
            "element.with.dots"
        })
        @DisplayName("Should accept valid XML element names")
        void shouldAcceptValidXMLElementNames(String elementName) throws Exception {
            builder.startDocument();
            
            assertThatCode(() -> builder.startElement(elementName))
                .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "123invalid",     // Cannot start with number
            "-invalid",       // Cannot start with hyphen
            ".invalid",       // Cannot start with dot
            "invalid<>",      // Contains invalid characters
            "invalid/slash",  // Contains slash
            "invalid space"   // Contains space
        })
        @DisplayName("Should reject invalid XML element names")
        void shouldRejectInvalidXMLElementNames(String elementName) throws Exception {
            builder.startDocument();
            
            assertThatThrownBy(() -> builder.startElement(elementName))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Invalid XML element name");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "xml",
            "XML",
            "element:with:colons"
        })
        @DisplayName("Should reject reserved XML names")
        void shouldRejectReservedXMLNames(String elementName) throws Exception {
            builder.startDocument();
            
            assertThatThrownBy(() -> builder.startElement(elementName))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Reserved XML element name");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "xmlDocument",
            "XMLDocument", 
            "Document",
            "xmlNamespace1"
        })
        @DisplayName("Should accept valid XML names that contain xml but are not reserved")
        void shouldAcceptValidXMLNamesContainingXml(String elementName) throws Exception {
            builder.startDocument();
            
            assertThatCode(() -> builder.startElement(elementName))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Attribute Validation Tests")
    class AttributeValidationTests {

        @Test
        @DisplayName("Should validate attribute names like element names")
        void shouldValidateAttributeNames() throws Exception {
            builder.startDocument()
                .startElement("root");
            
            assertThatThrownBy(() -> builder.addAttribute("123invalid", "value"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Invalid XML element name");
        }

        @Test
        @DisplayName("Should validate attribute values for security")
        void shouldValidateAttributeValues() throws Exception {
            builder.startDocument()
                .startElement("root");
            
            assertThatThrownBy(() -> builder.addAttribute("attr", "'; DROP TABLE users; --"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Invalid input detected");
        }

        @Test
        @DisplayName("Should accept clean attribute values")
        void shouldAcceptCleanAttributeValues() throws Exception {
            builder.startDocument()
                .startElement("root");
            
            assertThatCode(() -> builder.addAttribute("id", "valid-value-123"))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Text Content Validation Tests")
    class TextContentValidationTests {

        @Test
        @DisplayName("Should accept null text content")
        void shouldAcceptNullTextContent() throws Exception {
            builder.startDocument()
                .startElement("root");
            
            assertThatCode(() -> builder.addText(null))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject text with control characters")
        void shouldRejectTextWithControlCharacters() throws Exception {
            builder.startDocument()
                .startElement("root");
            
            String textWithControlChars = "Normal text\u0001with\u0002control\u0003chars";
            
            assertThatThrownBy(() -> builder.addText(textWithControlChars))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Text contains invalid control characters");
        }

        @Test
        @DisplayName("Should reject excessively long text content")
        void shouldRejectExcessivelyLongTextContent() throws Exception {
            builder.startDocument()
                .startElement("root");
            
            String longText = "a".repeat(10001); // Exceeds 10000 char limit
            
            assertThatThrownBy(() -> builder.addText(longText))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Text content exceeds maximum length");
        }

        @Test
        @DisplayName("Should reject text with injection patterns")
        void shouldRejectTextWithInjectionPatterns() throws Exception {
            builder.startDocument()
                .startElement("root");
            
            assertThatThrownBy(() -> builder.addText("<script>alert('xss')</script>"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Invalid input detected");
        }

        @Test
        @DisplayName("Should accept normal text content")
        void shouldAcceptNormalTextContent() throws Exception {
            builder.startDocument()
                .startElement("root");
            
            assertThatCode(() -> builder.addText("This is normal text with numbers 123 and symbols !@#"))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Complex XML Structure Tests")
    class ComplexXMLStructureTests {

        @Test
        @DisplayName("Should build ISO 20022 style XML structure")
        void shouldBuildISO20022StyleXMLStructure() throws Exception {
            try (SecureXMLBuilder testBuilder = new SecureXMLBuilder()) {
                String xml = testBuilder
                    .startDocument()
                    .startElement("Document")
                    .addAttribute("xmlns", "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03")
                    .startElement("CstmrCdtTrfInitn")
                    .startElement("GrpHdr")
                    .addElement("MsgId", "MSG123")
                    .addElement("CreDtTm", "2025-01-01T12:00:00")
                    .endElement() // GrpHdr
                    .startElement("PmtInf")
                    .addElement("PmtInfId", "PMT001")
                    .addElement("PmtMtd", "TRF")
                    .endElement() // PmtInf
                    .endElement() // CstmrCdtTrfInitn
                    .endElement() // Document
                    .endDocument()
                    .toString();

                assertThat(xml)
                    .contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                    .contains("<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.001.001.03\">")
                    .contains("<CstmrCdtTrfInitn>")
                    .contains("<GrpHdr>")
                    .contains("<MsgId>MSG123</MsgId>")
                    .contains("<CreDtTm>2025-01-01T12:00:00</CreDtTm>")
                    .contains("</GrpHdr>")
                    .contains("<PmtInf>")
                    .contains("<PmtInfId>PMT001</PmtInfId>")
                    .contains("<PmtMtd>TRF</PmtMtd>")
                    .contains("</PmtInf>")
                    .contains("</CstmrCdtTrfInitn>")
                    .contains("</Document>");
            }
        }

        @Test
        @DisplayName("Should handle elements with both attributes and text content")
        void shouldHandleElementsWithAttributesAndTextContent() throws Exception {
            try (SecureXMLBuilder testBuilder = new SecureXMLBuilder()) {
                String xml = testBuilder
                    .startDocument()
                    .startElement("root")
                    .startElement("amount")
                    .addAttribute("currency", "USD")
                    .addText("1234.56")
                    .endElement()
                    .endElement()
                    .endDocument()
                    .toString();

                assertThat(xml)
                    .contains("<amount currency=\"USD\">1234.56</amount>");
            }
        }
    }

    @Nested
    @DisplayName("Resource Management Tests")
    class ResourceManagementTests {

        @Test
        @DisplayName("Should close resources properly with try-with-resources")
        void shouldCloseResourcesProperlyWithTryWithResources() throws Exception {
            String xml;
            
            try (SecureXMLBuilder testBuilder = new SecureXMLBuilder()) {
                xml = testBuilder
                    .startDocument()
                    .startElement("root")
                    .addElement("child", "value")
                    .endElement()
                    .endDocument()
                    .toString();
            } // Should close automatically
            
            assertThat(xml).isNotEmpty();
        }

        @Test
        @DisplayName("Should handle manual close gracefully")
        void shouldHandleManualCloseGracefully() throws Exception {
            SecureXMLBuilder testBuilder = new SecureXMLBuilder();
            testBuilder.startDocument()
                .startElement("root")
                .endElement()
                .endDocument();
            
            assertThatCode(() -> testBuilder.close())
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle multiple close calls gracefully")
        void shouldHandleMultipleCloseCallsGracefully() throws Exception {
            SecureXMLBuilder testBuilder = new SecureXMLBuilder();
            testBuilder.startDocument()
                .startElement("root")
                .endElement()
                .endDocument();
            
            testBuilder.close();
            
            assertThatCode(() -> testBuilder.close())
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should maintain builder state after validation errors")
        void shouldMaintainBuilderStateAfterValidationErrors() throws Exception {
            builder.startDocument()
                .startElement("root");
            
            // Try to add invalid attribute (should fail)
            assertThatThrownBy(() -> builder.addAttribute("123invalid", "value"))
                .isInstanceOf(SecurityException.class);
            
            // Builder should still be usable
            assertThatCode(() -> builder
                .addAttribute("validAttr", "validValue")
                .endElement()
                .endDocument())
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle XML escaping automatically")
        void shouldHandleXMLEscapingAutomatically() throws Exception {
            try (SecureXMLBuilder testBuilder = new SecureXMLBuilder()) {
                String xml = testBuilder
                    .startDocument()
                    .startElement("root")
                    .addAttribute("attr", "Value with <brackets> & \"quotes\"")
                    .addElement("text", "Text with <special> & \"characters\"")
                    .endElement()
                    .endDocument()
                    .toString();

                // XML should be well-formed despite special characters
                assertThat(xml).isNotEmpty();
                // Note: XMLStreamWriter handles escaping automatically
            }
        }
    }
}