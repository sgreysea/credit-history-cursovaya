package com.credithistory.client;

import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ClientBioController {

    @FXML private Label fullNameLabel;
    @FXML private Label createdAtLabel;
    @FXML private Label totalCreditsLabel;
    @FXML private Label activeCreditsLabel;
    @FXML private Label closedCreditsLabel;
    @FXML private PieChart pieChart;
    @FXML private Label ratingLetterLabel;

    public void setClientData(String[] data) {
        // Формат: fullName|createdAt|totalCredits|activeCredits|closedCredits|totalPaid|paidOnTime|earlyPayments|totalOverdue|rating|color
        fullNameLabel.setText(data[0]);
        createdAtLabel.setText(data[1].substring(0, 10)); // только дата
        totalCreditsLabel.setText(data[2]);
        activeCreditsLabel.setText(data[3]);
        closedCreditsLabel.setText(data[4]);

        int totalPaid = Integer.parseInt(data[5]);
        int paidOnTime = Integer.parseInt(data[6]);
        int earlyPayments = Integer.parseInt(data[7]);
        int totalOverdue = Integer.parseInt(data[8]);
        int latePayments = totalPaid - paidOnTime;

        // Заполняем круговую диаграмму
        pieChart.getData().clear();

        if (paidOnTime > 0) {
            PieChart.Data slice1 = new PieChart.Data("Вовремя (" + paidOnTime + ")", paidOnTime);
            pieChart.getData().add(slice1);
        }
        if (earlyPayments > 0) {
            PieChart.Data slice2 = new PieChart.Data("Досрочно (" + earlyPayments + ")", earlyPayments);
            pieChart.getData().add(slice2);
        }
        if (latePayments > 0) {
            PieChart.Data slice3 = new PieChart.Data("С опозданием (" + latePayments + ")", latePayments);
            pieChart.getData().add(slice3);
        }
        if (totalOverdue > 0) {
            PieChart.Data slice4 = new PieChart.Data("Просрочено (" + totalOverdue + ")", totalOverdue);
            pieChart.getData().add(slice4);
        }

        // Устанавливаем букву рейтинга
        String rating = data[9];
        String color = data[10];
        ratingLetterLabel.setText(rating);
        ratingLetterLabel.setStyle("-fx-font-size: 48; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    }

    @FXML
    private void onCloseClick() {
        ((Stage) ratingLetterLabel.getScene().getWindow()).close();
    }
}