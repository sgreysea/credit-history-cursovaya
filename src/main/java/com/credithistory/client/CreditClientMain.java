<<<<<<< HEAD
package com.credithistory.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CreditClientMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Загружаем наше окно из login.fxml
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Кредитная система - Вход");
        primaryStage.setScene(new Scene(root, 400, 300));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
=======
package com.credithistory.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CreditClientMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Загружаем наше окно из login.fxml
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Кредитная система - Вход");
        primaryStage.setScene(new Scene(root, 400, 300));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
>>>>>>> 9a25b7675c45b2149c90b056a1d7d77d419d7ecd
}