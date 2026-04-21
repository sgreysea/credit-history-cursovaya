package com.credithistory.model;

import com.credithistory.database.PaymentDAO;
import com.credithistory.database.CreditDAO;

import java.math.BigDecimal;
import java.util.List;

public class ScoreCalculator {

    // Базовый рейтинг нового клиента
    private static final int BASE_SCORE = 500;

    // Максимальный и минимальный рейтинг
    private static final int MAX_SCORE = 850;
    private static final int MIN_SCORE = 100;

    // Коэффициенты для расчёта
    private static final double ON_TIME_BONUS_MULTIPLIER = 3.0;      // +3 балла за каждый % своевременных платежей
    private static final int OVERDUE_PENALTY_PER_PAYMENT = 50;       // -50 баллов за каждый просроченный платёж
    private static final int CLOSED_CREDIT_BONUS = 25;              // +25 баллов за каждый закрытый кредит
    private static final int ACTIVE_CREDIT_PENALTY = 10;            // -10 баллов за каждый активный кредит

    private final PaymentDAO paymentDAO;
    private final CreditDAO creditDAO;

    public ScoreCalculator() {
        this.paymentDAO = new PaymentDAO();
        this.creditDAO = new CreditDAO();
    }

    /**
     * Рассчитать кредитный рейтинг клиента
     * @param clientId ID клиента
     * @return кредитный рейтинг (от 100 до 850)
     */
    public int calculateScore(int clientId) {
        int score = BASE_SCORE;

        // 1. Получаем статистику по платежам
        PaymentDAO.PaymentStatistics paymentStats = paymentDAO.getPaymentStatisticsByClientId(clientId);

        // 2. Бонус за своевременные платежи
        double onTimePercentage = paymentStats.getOnTimePercentage();
        score += (int) (onTimePercentage * ON_TIME_BONUS_MULTIPLIER);

        // 3. Штраф за просроченные платежи
        score -= paymentStats.getOverdueCount() * OVERDUE_PENALTY_PER_PAYMENT;

        // 4. Анализ кредитов клиента
        List<Credit> credits = creditDAO.getCreditsByClientId(clientId);

        int activeCredits = 0;
        int closedCredits = 0;
        BigDecimal totalDebt = BigDecimal.ZERO;

        for (Credit credit : credits) {
            if (credit.getStatus() == CreditStatus.ACTIVE) {
                activeCredits++;
                totalDebt = totalDebt.add(credit.getAmount());
            } else if (credit.getStatus() == CreditStatus.CLOSED) {
                closedCredits++;
            }
        }

        // 5. Бонус за закрытые кредиты
        score += closedCredits * CLOSED_CREDIT_BONUS;

        // 6. Штраф за активные кредиты
        score -= activeCredits * ACTIVE_CREDIT_PENALTY;

        // 7. Дополнительный штраф за большую долговую нагрузку
        if (totalDebt.compareTo(new BigDecimal("50000")) > 0) {
            score -= 50;
        } else if (totalDebt.compareTo(new BigDecimal("20000")) > 0) {
            score -= 25;
        }

        // 8. Ограничение диапазона
        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
    }

    /**
     * Рассчитать рейтинг и вернуть с категорией
     * @param clientId ID клиента
     * @return объект CreditScore с рейтингом и категорией
     */
    public CreditScore calculateScoreWithCategory(int clientId) {
        int score = calculateScore(clientId);
        String category = getScoreCategory(score);
        String description = getScoreDescription(score);

        return new CreditScore(clientId, score, category, description);
    }

    /**
     * Получить категорию рейтинга
     */
    public String getScoreCategory(int score) {
        if (score >= 700) {
            return "ВЫСОКИЙ";
        } else if (score >= 500) {
            return "СРЕДНИЙ";
        } else {
            return "НИЗКИЙ";
        }
    }

    /**
     * Получить текстовое описание рейтинга
     */
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

    /**
     * Получить рекомендацию по кредитованию
     */
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

    /**
     * Сравнить рейтинг клиента с другими клиентами
     */
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

    /**
     * Рассчитать прогноз изменения рейтинга
     */
    public int predictFutureScore(int clientId, int monthsAhead) {
        int currentScore = calculateScore(clientId);
        List<Credit> credits = creditDAO.getCreditsByClientId(clientId);

        int activeCredits = 0;
        for (Credit credit : credits) {
            if (credit.getStatus() == CreditStatus.ACTIVE) {
                activeCredits++;
            }
        }

        // Прогноз: если клиент будет платить вовремя, рейтинг растёт
        int predictedScore = currentScore;

        if (activeCredits > 0) {
            // За каждый месяц своевременных платежей +3 балла
            predictedScore += monthsAhead * 3;
        }

        // Уменьшение количества активных кредитов со временем
        predictedScore += monthsAhead / 12 * CLOSED_CREDIT_BONUS;

        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, predictedScore));
    }
}