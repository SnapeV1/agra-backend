package org.agra.agra_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "news_articles")
public class NewsArticle {
    @Id
    private String id;

    private String title;
    private String description;
    private String url;
    private String source;
    private String publishedAt;
    private String country;
}