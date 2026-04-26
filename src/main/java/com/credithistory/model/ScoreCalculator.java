package com.credithistory.model;

import com.credithistory.database.PaymentDAO;
import com.credithistory.database.CreditDAO;

import java.math.BigDecimal;
import java.util.List;

public class ScoreCalculator {


    private static final int BASE_SCORE = 500;

    private static final int MAX_SCORE = 850;
    private static final int MIN_SCORE = 100;

    private static final double ON_TIME_BONUS_MULTIPLIER = 3.0;
    private static final int OVERDUE_PENALTY_PER_PAYMENT = 50;
    private static final int CLOSED_CREDIT_BONUS = 25;
    private static final int ACTIVE_CREDIT_PENALTY = 10;

    private final PaymentDAO paymentDAO;
    private final CreditDAO creditDAO;

    public ScoreCalculator() {
        this.paymentDAO = new PaymentDAO();
        this.creditDAO = new CreditDAO();
    }


    public int calculateScore(int clientId) {
        int score = BASE_SCORE;
        PaymentDAO.PaymentStatistics paymentStats = paymentDAO.getPaymentStatisticsByClientId(clientId);

        double onTimePercentage = paymentStats.getOnTimePercentage();
        score += (int) (onTimePercentage * ON_TIME_BONUS_MULTIPLIER);

        score -= paymentStats.getOverdueCount() * OVERDUE_PENALTY_PER_PAYMENT;

        List<Credit> credits = creditDAO.getCreditsByClientId(clientId);

        int activeCredits = 0;
        int closedCredits = 0;
        BigDecimal totalDebt = BigDecimal.ZERO;

        for (Credit credit : credits) {
            if (credit.getStatus() == CreditStatus.ACTIVE) {
                activeCredits++;
                totalDebt = totalDebt.add(credit.getAmount());

                List<Payment> payments = paymentDAO.getPaymentsByCreditId(credit.getId());
                for (Payment payment : payments) {
                    if (payment.getStatus() == PaymentStatus.OVERDUE) {
                        long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                                payment.getPlannedDate(),
                                java.time.LocalDate.now()
                        );

                        if (daysOverdue > 30) {
                            score -= 100;
                        } else if (daysOverdue > 7) {
                            score -= 30;
                        } else if (daysOverdue > 0) {
                            score -= 10;
                        }
                    }
                }

            } else if (credit.getStatus() == CreditStatus.CLOSED) {
                closedCredits++;
            }
        }

        score += closedCredits * CLOSED_CREDIT_BONUS;

        score -= activeCredits * ACTIVE_CREDIT_PENALTY;

        if (totalDebt.compareTo(new BigDecimal("50000")) > 0) {
            score -= 50;
        } else if (totalDebt.compareTo(new BigDecimal("20000")) > 0) {
            score -= 25;
        }

        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
    }

    public CreditScore calculateScoreWithCategory(int clientId) {
        int score = calculateScore(clientId);
        String category = getScoreCategory(score);
        String description = getScoreDescription(score);

        return new CreditScore(clientId, score, category, description);
    }

    public String getScoreCategory(int score) {
        if (score >= 700) {
            return "ВЫСОКИЙ";
        } else if (score >= 500) {
            return "СРЕДНИЙ";
        } else {
            return "НИЗКИЙ";
        }
    }


    public String getScoreDescription(int score) {
        if (score >= 750) {
            return "Отличная кредитная история. Высокая вероятность одобрения кредита на льготных условиях.";
        } else if (score >= 700) {
            return "Хорошая кредитная история. Кредитование возможно на стандартных условиях.";
        } else if (score >= 600) {
            return "Удовлетворительная кредитная история. Возможно одобрение с повышенной ставкой.";
        } else if (score >= 500) {
            return "Средняя кредитная история. Требуется дополнительная проверка или поручитель.";
        } else if (score >= 400) {
            return "Низкий кредитный рейтинг. Высокий риск. Требуется залог или поручитель.";
        } else {
            return "Очень низкий кредитный рейтинг. Рекомендуется отказать в кредитовании.";
        }
    }


    public String getRecommendation(int score, BigDecimal requestedAmount) {
        if (score >= 700) {
            return "РЕКОМЕНДОВАНО: Кредит до " + requestedAmount + " BYN может быть одобрен.";
        } else if (score >= 550) {
            BigDecimal maxAmount = requestedAmount.multiply(new BigDecimal("0.7"));
            return "РЕКОМЕНДОВАНО С ОГРАНИЧЕНИЯМИ: Максимальная сумма " +
                    String.format("%.2f", maxAmount) + " BYN или требуется поручитель.";
        } else if (score >= 400) {
            return "НЕ РЕКОМЕНДОВАНО: Высокий риск невозврата. Требуется залог или отказ.";
        } else {
            return "ОТКАЗАНО: Кредитная история неудовлетворительная.";
        }
    }


    public String getPercentileRank(int score) {
        if (score >= 750) {
            return "Топ 10% заёмщиков";
        } else if (score >= 650) {
            return "Топ 25% заёмщиков";
        } else if (score >= 550) {
            return "Средний уровень (50% заёмщиков)";
        } else if (score >= 450) {
            return "Ниже среднего (нижние 25%)";
        } else {
            return "Высокий риск (нижние 10%)";
        }
    }


    public int predictFutureScore(int clientId, int monthsAhead) {
        int currentScore = calculateScore(clientId);
        List<Credit> credits = creditDAO.getCreditsByClientId(clientId);

        int activeCredits = 0;
        for (Credit credit : credits) {
            if (credit.getStatus() == CreditStatus.ACTIVE) {
                activeCredits++;
            }
        }

        int predictedScore = currentScore;

        if (activeCredits > 0) {
            predictedScore += monthsAhead * 3;
        }

        predictedScore += monthsAhead / 12 * CLOSED_CREDIT_BONUS;

        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, predictedScore));
    }
}