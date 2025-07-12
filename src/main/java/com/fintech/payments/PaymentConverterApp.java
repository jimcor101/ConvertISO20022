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
package com.fintech.payments;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main JavaFX application class for the ConvertISO20022 payment format converter.
 * <p>
 * This application provides a desktop GUI for converting legacy payment formats
 * (MT103 and NACHA) to ISO 20022 XML format. The application uses JavaFX for the
 * user interface and follows the MVC pattern with FXML-based UI definition.
 * </p>
 * 
 * <h3>Features:</h3>
 * <ul>
 *   <li>File selection dialog with format-specific file filters</li>
 *   <li>Support for MT103 SWIFT messages and NACHA ACH files</li>
 *   <li>Real-time conversion progress tracking</li>
 *   <li>ISO 20022 pain.001.001.03 XML output generation</li>
 *   <li>Error handling and user feedback</li>
 * </ul>
 * 
 * <h3>Usage:</h3>
 * <p>
 * Run the application using Maven: {@code mvn javafx:run}<br>
 * Or execute the packaged JAR: {@code java -jar ConvertISO20022-1.0.0.jar}
 * </p>
 * 
 * @author Jim Cornacchia
 * @version 1.0.0
 * @since 1.0.0
 */
public class PaymentConverterApp extends Application {

    /**
     * Initializes and displays the main application window.
     * <p>
     * This method is called by the JavaFX framework when the application starts.
     * It loads the FXML layout, creates the scene, and configures the primary stage
     * with appropriate title, dimensions, and displays the window.
     * </p>
     * 
     * @param primaryStage the primary stage for this application, onto which
     *                     the application scene can be set
     * @throws Exception if the FXML file cannot be loaded or other initialization errors occur
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainWindow.fxml"));
        Scene scene = new Scene(loader.load());
        
        primaryStage.setTitle("ConvertISO20022 - Legacy Payment Format Converter");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    /**
     * Main entry point for the application.
     * <p>
     * This method launches the JavaFX application by calling the inherited
     * {@code launch()} method, which will eventually call the {@code start()}
     * method to initialize the UI.
     * </p>
     * 
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        launch(args);
    }
}