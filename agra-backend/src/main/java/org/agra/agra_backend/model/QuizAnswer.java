package org.agra.agra_backend.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class    QuizAnswer {
    private String id;
    private Map<String, QuizAnswerTranslation> translations;
    @JsonAlias("isCorrect")
    private boolean correct;
}
