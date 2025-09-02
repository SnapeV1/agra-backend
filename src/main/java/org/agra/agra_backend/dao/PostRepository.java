package org.agra.agra_backend.dao;

import org.agra.agra_backend.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface PostRepository extends MongoRepository<Post, String> {

    List<Post> findByUserIdOrderByCreatedAtDesc(String userId);
        List<Post> findByIsCoursePostOrderByCreatedAtDesc(boolean isCoursePost);
    List<Post> findByCourseIdOrderByCreatedAtDesc(String courseId);

    Page<Post> findByIsCoursePostOrderByCreatedAtDesc(boolean isCoursePost, Pageable pageable);
    List<Post> findAllByOrderByCreatedAtDesc();


}