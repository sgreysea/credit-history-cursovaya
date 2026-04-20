<<<<<<< HEAD
package com.credithistory.service;

import com.credithistory.model.Credit;
import com.credithistory.model.CreditStatus;
import com.credithistory.model.User;

import java.util.List;

public class ScoreCalculator {

    public int calculate(User user, List<Credit> credits) {
        int score = 500;

        for (Credit credit : credits) {
            if (credit.getBorrower().equals(user)) {

                if (credit.getStatus() == CreditStatus.OVERDUE) {
                    score -= 100;
                } else if (credit.getStatus() == CreditStatus.CLOSED) {
                    score += 50;
                } else {
                    score += 10;
                }
            }
        }

        if (score < 0) score = 0;
        if (score > 1000) score = 1000;

        return score;
    }
=======
package com.credithistory.service;

import com.credithistory.model.Credit;
import com.credithistory.model.CreditStatus;
import com.credithistory.model.User;

import java.util.List;

public class ScoreCalculator {

    public int calculate(User user, List<Credit> credits) {
        int score = 500;

        for (Credit credit : credits) {
            if (credit.getBorrower().equals(user)) {

                if (credit.getStatus() == CreditStatus.OVERDUE) {
                    score -= 100;
                } else if (credit.getStatus() == CreditStatus.CLOSED) {
                    score += 50;
                } else {
                    score += 10;
                }
            }
        }

        if (score < 0) score = 0;
        if (score > 1000) score = 1000;

        return score;
    }
>>>>>>> 9a25b7675c45b2149c90b056a1d7d77d419d7ecd
}