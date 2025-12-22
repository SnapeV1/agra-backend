package org.agra.agra_backend.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.Map;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextContent {
    @Id
    private String id;
    private int order;
    private String type;
    private Map<String, String> title;
    private Map<String, String> content;

    /**
     * Populated when type == "QUIZ". Contains questions with answer options,
     * exactly one of which should be marked as correct on the client side.
     */
    @JsonAlias("questions")
    private java.util.List<QuizQuestion> quizQuestions;

}
