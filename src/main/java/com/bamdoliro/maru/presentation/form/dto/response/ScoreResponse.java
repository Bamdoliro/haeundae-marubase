package com.bamdoliro.maru.presentation.form.dto.response;

import com.bamdoliro.maru.domain.form.domain.value.Score;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@AllArgsConstructor
public class ScoreResponse {

    private Double firstRoundScore;
    private Double totalScore;

    public ScoreResponse(Score score) {
        this.firstRoundScore = score.getFirstRoundScore();
        this.totalScore = score.getTotalScore();
    }

    public Double round(Double score) {
        if(score == null) {
            return null;
        }
        return BigDecimal.valueOf(score)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
