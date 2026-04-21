package com.credithistory.service;

import com.credithistory.database.CreditDAO;
import com.credithistory.database.PaymentDAO;
import com.credithistory.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CreditService {

    private final CreditDAO creditDAO;
    private final PaymentDAO paymentDAO;
    private final ScoreCalculator scoreCalculator;

    public CreditService() {
        this.creditDAO = new CreditDAO();
        this.paymentDAO = new PaymentDAO();
        this.scoreCalculator = new ScoreCalculator();
    }

    // Получить все кредиты клиента
    public List<Credit> getClientCredits(int clientId) {
        return creditDAO.getCreditsByClientId(clientId);
    }

    // Оформить новый кредит
    public Credit issueCredit(int clientId, int userId, BigDecimal amount,
                              int termMonths, BigDecimal interestRate) {
        Credit credit = new Credit(clientId, userId, amount, termMonths, interestRate, LocalDate.now());
        boolean created = creditDAO.createCredit(credit);

        if (created) {
            // Генерируем график платежей
            BigDecimal monthlyPayment = credit.getMonthlyPayment();
            paymentDAO.generatePaymentSchedule(credit.getId(), monthlyPayment,
                    credit.getIssueDate(), termMonths);
            return credit;
        }
        return null;
    }

    // Рассчитать кредитный рейтинг клиента
    public int calculateClientScore(int clientId) {
        return scoreCalculator.calculateScore(clientId);
    }

    // Получить полную информацию о кредитном рейтинге
    public CreditScore getClientCreditScore(int clientId) {
        return scoreCalculator.calculateScoreWithCategory(clientId);
    }

    // Закрыть кредит
    public boolean closeCredit(int creditId) {
        return creditDAO.closeCredit(creditId);
    }

    // Проверить, может ли клиент получить кредит
    public boolean canGetCredit(int clientId, BigDecimal requestedAmount) {
        int score = calculateClientScore(clientId);

        if (score >= 700) {
            return true;
        } else if (score >= 550) {
            // Может получить не более 70% от запрашиваемой суммы
            return true;
        } else {
            return false;
        }
    }

    // Получить рекомендацию по кредиту
    public String getCreditRecommendation(int clientId, BigDecimal requestedAmount) {
        int score = calculateClientScore(clientId);
        return scoreCalculator.getRecommendation(score, requestedAmount);
    }

    // Получить статистику по всем кредитам
    public CreditDAO.CreditStatistics getOverallStatistics() {
        return creditDAO.getStatistics();
    }

    // Получить активные кредиты
    public List<Credit> getActiveCredits() {
        return creditDAO.getActiveCredits();
    }

    // Получить просроченные кредиты
    public List<Credit> getOverdueCredits() {
        return creditDAO.getOverdueCredits();
    }
}