package org.agra.agra_backend.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizAnswer {
    private String id;
    private String text;
    @JsonAlias("isCorrect")
    private boolean correct;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public QuizAnswer(String value) {
        this.text = value;
        this.correct = false;
    }
}
