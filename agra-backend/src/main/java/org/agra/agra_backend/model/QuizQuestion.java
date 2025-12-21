package org.agra.agra_backend.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizQuestion {
    private String id;
    private String question;
    private Map<String, QuizQuestionTranslation> translations;
    /**
     * One answer in this list should have correct=true.
     * Accepts incoming payload key "options" for backwards compatibility.
     */
    @JsonAlias("options")
    private List<QuizAnswer> answers;
}
