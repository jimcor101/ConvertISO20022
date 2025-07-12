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

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

/**
 * Modal dialog for configuring conversion settings and selecting output file location.
 * <p>
 * This dialog extends JavaFX Dialog to provide a user interface for setting conversion
 * parameters before starting the actual conversion process. It allows users to specify
 * the output file location, choose message type, and configure validation options.
 * </p>
 * 
 * <h3>Features:</h3>
 * <ul>
 *   <li>Output file selection with native file chooser integration</li>
 *   <li>ISO 20022 message type selection (pain.001.001.03, pain.008.001.02)</li>
 *   <li>XML validation options</li>
 *   <li>Original data reference inclusion settings</li>
 *   <li>Input validation and user feedback</li>
 * </ul>
 * 
 * <h3>Dialog Result:</h3>
 * <p>
 * The dialog returns an Optional&lt;File&gt; representing the selected output file.
 * If the user cancels the dialog or doesn't select a valid file, the Optional
 * will be empty.
 * </p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * ConversionDialog dialog = new ConversionDialog();
 * dialog.showAndWait().ifPresent(outputFile -> {
 *     // Proceed with conversion using the selected output file
 *     startConversion(outputFile);
 * });
 * }</pre>
 * 
 * @author Jim Cornacchia
 * @version 1.0.0
 * @since 1.0.0
 */
public class ConversionDialog extends Dialog<File> {

    /**
     * Constructs a new ConversionDialog with all UI components and event handlers.
     * <p>
     * This constructor initializes the dialog layout, creates all UI controls,
     * sets up event handlers for user interactions, and configures the result
     * converter. The dialog is modal and will block the parent window until
     * the user either confirms or cancels the operation.
     * </p>
     * 
     * <h4>UI Layout:</h4>
     * <p>
     * The dialog uses a GridPane layout with the following components:
     * </p>
     * <ul>
     *   <li>Output file path field with browse button</li>
     *   <li>Message type selection combo box</li>
     *   <li>XML validation checkbox</li>
     *   <li>Original data inclusion checkbox</li>
     * </ul>
     * 
     * <h4>Validation:</h4>
     * <p>
     * The Convert button is disabled until a valid output file path is selected.
     * Input validation occurs in real-time as the user interacts with the form.
     * </p>
     */
    public ConversionDialog() {
        setTitle("Conversion Settings");
        setHeaderText("Configure conversion output");

        ButtonType convertButtonType = new ButtonType("Convert", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(convertButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Output file selection components
        TextField outputFileField = new TextField();
        outputFileField.setPromptText("Select output file location...");
        outputFileField.setPrefWidth(300);
        outputFileField.setEditable(false);

        Button browseButton = new Button("Browse...");
        browseButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save ISO 20022 XML File");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML Files", "*.xml")
            );
            fileChooser.setInitialFileName("converted_payment.xml");

            Stage stage = (Stage) getDialogPane().getScene().getWindow();
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                outputFileField.setText(file.getAbsolutePath());
            }
        });

        // Message type selection
        ComboBox<String> messageTypeCombo = new ComboBox<>();
        messageTypeCombo.getItems().addAll(
            "pain.001.001.03 (Customer Credit Transfer)",
            "pain.008.001.02 (Customer Direct Debit)"
        );
        messageTypeCombo.setValue("pain.001.001.03 (Customer Credit Transfer)");

        // Conversion options
        CheckBox validateOutputCheckbox = new CheckBox("Validate output XML");
        validateOutputCheckbox.setSelected(true);

        CheckBox includeOriginalCheckbox = new CheckBox("Include original data as reference");
        includeOriginalCheckbox.setSelected(false);

        // Layout components in grid
        grid.add(new Label("Output File:"), 0, 0);
        grid.add(outputFileField, 1, 0);
        grid.add(browseButton, 2, 0);
        grid.add(new Label("Message Type:"), 0, 1);
        grid.add(messageTypeCombo, 1, 1, 2, 1);
        grid.add(validateOutputCheckbox, 1, 2, 2, 1);
        grid.add(includeOriginalCheckbox, 1, 3, 2, 1);

        getDialogPane().setContent(grid);

        // Configure convert button state based on input validation
        Button convertButton = (Button) getDialogPane().lookupButton(convertButtonType);
        convertButton.setDisable(true);

        outputFileField.textProperty().addListener((observable, oldValue, newValue) -> {
            convertButton.setDisable(newValue.trim().isEmpty());
        });

        // Set up result converter to return selected file
        setResultConverter(dialogButton -> {
            if (dialogButton == convertButtonType) {
                String filePath = outputFileField.getText();
                if (!filePath.isEmpty()) {
                    return new File(filePath);
                }
            }
            return null;
        });
    }
}