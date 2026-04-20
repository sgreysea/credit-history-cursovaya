package com.credithistory.service;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import com.credithistory.model.Credit;
import com.credithistory.model.User;
import com.credithistory.util.LoggerUtil;
import org.apache.logging.log4j.Logger;

import com.credithistory.service.ScoreCalculator;
import java.util.ArrayList;
import java.util.List;

public class CreditService {

    private static final Logger logger = LoggerUtil.getLogger(CreditService.class);

    private final List<Credit> credits = Collections.synchronizedList(new ArrayList<>());

    public String createCredit(User user, double amount, double rate, int term) {

        Credit credit = new Credit(
                System.currentTimeMillis(),
                amount,
                rate,
                term,
                user
        );

        credits.add(credit);

        logger.info("Кредит создан для пользователя: {}", user.getFullName());

        return "success: credit created";
    }

    public List<Credit> getUserCredits(User user) {
        List<Credit> result = new ArrayList<>();

        for (Credit credit : credits) {
            if (credit.getBorrower().equals(user)) {
                result.add(credit);
            }
        }

        return result;
    }
    public int getUserCreditScore(User user) {
        List<Credit> userCredits = getUserCredits(user);
        ScoreCalculator calculator = new ScoreCalculator();
        return calculator.calculate(user, userCredits);
    }
}