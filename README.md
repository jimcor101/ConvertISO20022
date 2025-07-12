# ConvertISO20022

A Java-based desktop application for converting legacy payment formats (MT103, NACHA) to ISO 20022 XML format.

## Features

- **Graphical User Interface**: JavaFX-based UI with file selection, conversion dialogs, and progress tracking
- **MT103 Conversion**: Convert SWIFT MT103 credit transfer messages to ISO 20022 pain.001.001.03
- **NACHA Conversion**: Convert ACH NACHA files to ISO 20022 pain.001.001.03
- **Real-time Progress**: Live conversion status and detailed logging
- **Error Handling**: Comprehensive error reporting and validation

## Requirements

- Java 17 or higher
- Maven 3.6 or higher

## Building and Running

### Build the project:
```bash
mvn clean compile
```

### Run the application:
```bash
mvn javafx:run
```

### Create executable JAR:
```bash
mvn clean package
java -jar target/ConvertISO20022-1.0.0.jar
```

## Usage

1. **Select Input File**: Click "Select File..." and choose your MT103 or NACHA file
2. **Choose Format**: Select the input format (MT103 or NACHA) from the dropdown
3. **Convert**: Click "Convert to ISO 20022" to open the conversion dialog
4. **Configure Output**: Choose output location and conversion settings
5. **Start Conversion**: The application will process the file and show progress

## Supported Formats

### Input Formats
- **MT103**: SWIFT credit transfer messages
- **NACHA**: ACH credit transfer files

### Output Format
- **ISO 20022 pain.001.001.03**: Customer Credit Transfer Initiation

## Project Structure

```
src/main/java/com/fintech/payments/
├── PaymentConverterApp.java          # Main application class
├── ui/
│   ├── MainController.java           # Main UI controller
│   └── ConversionDialog.java         # Conversion settings dialog
├── converter/
│   ├── ConversionService.java        # Main conversion service
│   ├── MT103Converter.java           # MT103 to ISO 20022 converter
│   └── NACHAConverter.java           # NACHA to ISO 20022 converter
└── model/
    └── ConversionResult.java         # Conversion result model
```

## License

This project is for demonstration purposes in financial services portfolio.