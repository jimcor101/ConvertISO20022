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
package com.fintech.payments.ui;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.fintech.payments.converter.ConversionService;
import com.fintech.payments.model.ConversionResult;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main controller class for the ConvertISO20022 application's primary user interface.
 * <p>
 * This controller manages the main window UI components and coordinates user interactions
 * for payment format conversion. It implements the Initializable interface to support
 * FXML-based UI initialization and follows the MVC pattern by separating UI logic
 * from business logic.
 * </p>
 * 
 * <h3>Key Responsibilities:</h3>
 * <ul>
 *   <li>File selection through native file chooser dialogs</li>
 *   <li>Input format selection and validation</li>
 *   <li>Conversion process initiation and monitoring</li>
 *   <li>Real-time progress reporting and logging</li>
 *   <li>Result presentation and error handling</li>
 *   <li>UI state management and user feedback</li>
 * </ul>
 * 
 * <h3>UI Components:</h3>
 * <p>
 * The controller manages several FXML-injected UI components including file selection
 * buttons, format combo boxes, progress indicators, and result display areas.
 * All user interactions are handled through JavaFX event handlers.
 * </p>
 * 
 * <h3>Threading:</h3>
 * <p>
 * Conversion operations are performed on background threads using JavaFX Task
 * to prevent UI freezing. Progress updates are safely marshaled back to the
 * JavaFX Application Thread for UI updates.
 * </p>
 * 
 * @author Jim Cornacchia
 * @version 1.0.0
 * @since 1.0.0
 */
public class MainController implements Initializable {

    /** Button for triggering file selection dialog */
    @FXML private Button selectFileButton;
    
    /** Label displaying the name of the currently selected file */
    @FXML private Label selectedFileLabel;
    
    /** Combo box for selecting the input file format (MT103, NACHA) */
    @FXML private ComboBox<String> formatComboBox;
    
    /** Button for initiating the conversion process */
    @FXML private Button convertButton;
    
    /** Text area for displaying conversion progress and log messages */
    @FXML private TextArea conversionLogArea;
    
    /** Progress bar showing conversion progress */
    @FXML private ProgressBar progressBar;
    
    /** Status label showing current operation status */
    @FXML private Label statusLabel;

    /** Currently selected input file */
    private File selectedFile;
    
    /** Service instance for performing conversions */
    private ConversionService conversionService;

    /**
     * Initializes the controller after the FXML file has been loaded.
     * <p>
     * This method is automatically called by the JavaFX framework after
     * the FXML loading process completes. It sets up the initial UI state,
     * populates combo boxes, configures component visibility, and establishes
     * event handlers.
     * </p>
     * 
     * @param location the location used to resolve relative paths for the root object,
     *                 or null if the location is not known
     * @param resources the resources used to localize the root object,
     *                  or null if the root object was not localized
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        conversionService = new ConversionService();
        
        formatComboBox.getItems().addAll("MT103", "NACHA");
        formatComboBox.setValue("MT103");
        
        convertButton.setDisable(true);
        progressBar.setVisible(false);
        
        setupEventHandlers();
    }

    /**
     * Configures event handlers for UI components.
     * <p>
     * Establishes the necessary event bindings between UI components and
     * their corresponding handler methods. This includes file selection,
     * conversion initiation, and input validation triggers.
     * </p>
     */
    private void setupEventHandlers() {
        selectFileButton.setOnAction(e -> selectFile());
        convertButton.setOnAction(e -> performConversion());
        
        formatComboBox.setOnAction(e -> validateInputs());
    }

    /**
     * Handles file selection through a native file chooser dialog.
     * <p>
     * Presents a file chooser dialog to the user with appropriate file filters
     * based on the selected input format. Updates the UI to display the selected
     * file name and triggers input validation to enable/disable the convert button.
     * </p>
     * 
     * <h4>File Filters:</h4>
     * <ul>
     *   <li>MT103 format: *.mt103, *.txt files</li>
     *   <li>NACHA format: *.ach, *.txt files</li>
     *   <li>All formats: *.* (fallback option)</li>
     * </ul>
     */
    @FXML
    private void selectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Legacy Payment File");
        
        String selectedFormat = formatComboBox.getValue();
        if ("MT103".equals(selectedFormat)) {
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("MT103 Files", "*.mt103", "*.txt")
            );
        } else if ("NACHA".equals(selectedFormat)) {
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("NACHA Files", "*.ach", "*.txt")
            );
        }
        
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        Stage stage = (Stage) selectFileButton.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            selectedFileLabel.setText(selectedFile.getName());
            validateInputs();
        }
    }

    /**
     * Validates user inputs and enables/disables the convert button accordingly.
     * <p>
     * Checks that both a file has been selected and a format has been chosen
     * before enabling the conversion button. This prevents users from attempting
     * conversion without the necessary inputs.
     * </p>
     */
    private void validateInputs() {
        boolean hasFile = selectedFile != null;
        boolean hasFormat = formatComboBox.getValue() != null;
        convertButton.setDisable(!(hasFile && hasFormat));
    }

    /**
     * Initiates the conversion process by showing the conversion settings dialog.
     * <p>
     * This method serves as the entry point for conversion operations. It first
     * validates that required inputs are available, then presents the conversion
     * dialog for output file selection and conversion settings. If the user
     * confirms the dialog, the actual conversion process begins.
     * </p>
     */
    @FXML
    private void performConversion() {
        if (selectedFile == null || formatComboBox.getValue() == null) {
            return;
        }

        ConversionDialog dialog = new ConversionDialog();
        dialog.showAndWait().ifPresent(outputFile -> {
            startConversion(outputFile);
        });
    }

    /**
     * Starts the actual conversion process in a background thread.
     * <p>
     * This method configures the UI for conversion mode (showing progress indicators,
     * clearing logs) and creates a JavaFX Task to perform the conversion on a
     * background thread. Progress updates are displayed in real-time, and the
     * UI is updated when the conversion completes (successfully or with errors).
     * </p>
     * 
     * <h4>Threading Strategy:</h4>
     * <p>
     * The conversion runs on a daemon background thread to prevent blocking the
     * JavaFX Application Thread. Progress messages are safely marshaled back to
     * the UI thread through the Task's message property binding.
     * </p>
     * 
     * @param outputFile the destination file for the converted output
     */
    private void startConversion(File outputFile) {
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        statusLabel.setText("Converting...");
        conversionLogArea.clear();
        
        Task<ConversionResult> conversionTask = new Task<ConversionResult>() {
            @Override
            protected ConversionResult call() throws Exception {
                String format = formatComboBox.getValue();
                updateMessage("Reading input file: " + selectedFile.getName());
                
                return conversionService.convertFile(selectedFile, format, outputFile, 
                    message -> updateMessage(message));
            }
        };

        conversionTask.messageProperty().addListener((obs, oldMessage, newMessage) -> {
            conversionLogArea.appendText(newMessage + "\n");
        });

        conversionTask.setOnSucceeded(e -> {
            ConversionResult result = conversionTask.getValue();
            progressBar.setVisible(false);
            
            if (result.isSuccess()) {
                statusLabel.setText("Conversion completed successfully");
                conversionLogArea.appendText("\nConversion completed successfully!\n");
                conversionLogArea.appendText("Output file: " + result.getOutputFile().getAbsolutePath() + "\n");
                showSuccessDialog(result);
            } else {
                statusLabel.setText("Conversion failed");
                conversionLogArea.appendText("\nConversion failed: " + result.getErrorMessage() + "\n");
                showErrorDialog(result.getErrorMessage());
            }
        });

        conversionTask.setOnFailed(e -> {
            progressBar.setVisible(false);
            statusLabel.setText("Conversion failed");
            Throwable exception = conversionTask.getException();
            String errorMessage = exception != null ? exception.getMessage() : "Unknown error";
            conversionLogArea.appendText("\nConversion failed: " + errorMessage + "\n");
            showErrorDialog(errorMessage);
        });

        Thread conversionThread = new Thread(conversionTask);
        conversionThread.setDaemon(true);
        conversionThread.start();
    }

    /**
     * Displays a success dialog when conversion completes successfully.
     * <p>
     * Shows an informational alert dialog with details about the successful
     * conversion, including the output file name. This provides immediate
     * feedback to the user about the conversion outcome.
     * </p>
     * 
     * @param result the successful conversion result containing output file information
     */
    private void showSuccessDialog(ConversionResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Conversion Successful");
        alert.setHeaderText("File converted successfully");
        alert.setContentText("Output saved to: " + result.getOutputFile().getName());
        alert.showAndWait();
    }

    /**
     * Displays an error dialog when conversion fails.
     * <p>
     * Shows an error alert dialog with the specific error message that caused
     * the conversion to fail. This helps users understand what went wrong and
     * potentially take corrective action.
     * </p>
     * 
     * @param errorMessage the error message describing why the conversion failed
     */
    private void showErrorDialog(String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Conversion Failed");
        alert.setHeaderText("An error occurred during conversion");
        alert.setContentText(errorMessage);
        alert.showAndWait();
    }
}