package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.NewsArticle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsArticleRepository extends MongoRepository<NewsArticle, String> {
    java.util.List<NewsArticle> findByCountryIgnoreCase(String country);
    java.util.List<NewsArticle> findByPublishedAtStartingWith(String datePrefix);
    java.util.List<NewsArticle> findByCountryIgnoreCaseAndPublishedAtStartingWith(String country, String datePrefix);
}