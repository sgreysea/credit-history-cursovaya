package com.credithistory.client;

import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

public class ClientBioController {

    @FXML private Label fullNameLabel;
    @FXML private Label createdAtLabel;
    @FXML private Label totalCreditsLabel;
    @FXML private Label activeCreditsLabel;
    @FXML private Label closedCreditsLabel;
    @FXML private PieChart pieChart;
    @FXML private Label ratingLetterLabel;
    @FXML private Label chartCaptionLabel;
    @FXML private ToggleButton paymentsToggle;
    @FXML private ToggleButton creditsToggle;

    private int payOnTime;
    private int payEarly;
    private int payLate;
    private int payOverdue;

    private int crActive;
    private int crOverdue;
    private int crClosedNorm;
    private int crClosedEarly;

    @FXML
    private void initialize() {
        ToggleGroup g = new ToggleGroup();
        paymentsToggle.setToggleGroup(g);
        creditsToggle.setToggleGroup(g);
        g.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == paymentsToggle) {
                fillPaymentsPie();
                chartCaptionLabel.setText("график: платежи");
            } else if (n == creditsToggle) {
                fillCreditsPie();
                chartCaptionLabel.setText("график: кредиты");
            }
        });
    }

    /** Формат: ... рейтинг|цвет [| кредиты: активн|просроч.|закрыт обыч|закр. досроч — 4 поля] */
    public void setClientData(String[] data) {
        fullNameLabel.setText(data[0]);
        createdAtLabel.setText(data[1].length() >= 10 ? data[1].substring(0, 10) : data[1]);
        totalCreditsLabel.setText(data[2]);
        activeCreditsLabel.setText(data[3]);
        closedCreditsLabel.setText(data[4]);

        int totalPaid = Integer.parseInt(data[5]);
        int paidOnTime = Integer.parseInt(data[6]);
        int earlyPayments = Integer.parseInt(data[7]);
        int totalOverdue = Integer.parseInt(data[8]);
        payOnTime = paidOnTime;
        payEarly = earlyPayments;
        payLate = Math.max(0, totalPaid - paidOnTime - earlyPayments);
        payOverdue = totalOverdue;

        if (data.length >= 15) {
            crActive = Integer.parseInt(data[11]);
            crOverdue = Integer.parseInt(data[12]);
            crClosedNorm = Integer.parseInt(data[13]);
            crClosedEarly = Integer.parseInt(data[14]);
        } else {
            crActive = Integer.parseInt(data[3]);
            crOverdue = 0;
            crClosedNorm = Integer.parseInt(data[4]);
            crClosedEarly = 0;
        }

        fillPaymentsPie();

        String rating = data[9];
        String color = data[10];
        ratingLetterLabel.setText(rating);
        ratingLetterLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        paymentsToggle.setSelected(true);
    }

    private void fillPaymentsPie() {
        pieChart.getData().clear();
        pieChart.setTitle("");
        addSliceIfPositive("вовремя", payOnTime);
        addSliceIfPositive("досрочно (плат.)", payEarly);
        addSliceIfPositive("с опозданием", payLate);
        addSliceIfPositive("просрочено", payOverdue);
        if (pieChart.getData().isEmpty()) {
            PieChart.Data empty = new PieChart.Data("нет данных", 1);
            pieChart.getData().add(empty);
        }
    }

    private void fillCreditsPie() {
        pieChart.getData().clear();
        pieChart.setTitle("");
        addSliceIfPositive("активные", crActive);
        addSliceIfPositive("просрочено (кредит)", crOverdue);
        addSliceIfPositive("закрыты", crClosedNorm);
        addSliceIfPositive("закрыты досрочно", crClosedEarly);
        if (pieChart.getData().isEmpty()) {
            pieChart.getData().add(new PieChart.Data("нет данных", 1));
        }
    }

    private void addSliceIfPositive(String label, int value) {
        if (value > 0) {
            pieChart.getData().add(new PieChart.Data(label + " (" + value + ")", value));
        }
    }

    @FXML
    private void onCloseClick() {
        ((Stage) ratingLetterLabel.getScene().getWindow()).close();
    }
}
