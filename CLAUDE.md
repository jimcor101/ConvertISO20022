# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ConvertISO20022 is a JavaFX desktop application that converts legacy payment formats (MT103 and NACHA) to ISO 20022 XML format. The application provides a graphical interface for file selection, format conversion, and progress monitoring.

## Build and Development Commands

This is a Maven-based Java 17 project with JavaFX. Common commands:

```bash
# Build the project
mvn clean compile

# Run the application
mvn javafx:run

# Run tests
mvn test

# Create executable JAR
mvn clean package

# Run the packaged JAR
java -jar target/ConvertISO20022-1.0.0.jar
```

## Architecture

### Core Components

- **PaymentConverterApp**: Main JavaFX application entry point
- **ConversionService**: Central service that orchestrates format-specific converters
- **MainController**: Primary UI controller handling file selection and conversion initiation
- **ConversionDialog**: Modal dialog for conversion settings and progress display

### Conversion Architecture

The conversion process follows a strategy pattern:
- `ConversionService` delegates to format-specific converters (`MT103Converter`, `NACHAConverter`)
- Each converter implements file parsing and ISO 20022 XML generation
- Progress callbacks provide real-time status updates to the UI
- `ConversionResult` model encapsulates success/failure states

### UI Structure

- FXML-based JavaFX interface defined in `src/main/resources/MainWindow.fxml`
- Controllers handle user interactions and bind to conversion services
- Real-time progress tracking through callback functions

### Key Dependencies

- JavaFX 19.0.2.1 for UI framework
- JAXB for XML processing and ISO 20022 generation
- Jackson for JSON handling
- Apache Commons Lang3 for utilities
- JUnit Jupiter for testing

## Testing

Tests are located in standard Maven structure and use JUnit Jupiter. The project includes unit tests for conversion logic and integration tests for file processing workflows.