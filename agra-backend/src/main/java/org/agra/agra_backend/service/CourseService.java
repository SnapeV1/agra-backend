package org.agra.agra_backend.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.agra.agra_backend.dao.CourseRepository;
import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.model.CourseProgress;
import org.agra.agra_backend.model.CourseTranslation;
import org.agra.agra_backend.model.QuizAnswer;
import org.agra.agra_backend.model.QuizAnswerTranslation;
import org.agra.agra_backend.model.QuizQuestion;
import org.agra.agra_backend.model.QuizQuestionTranslation;
import org.agra.agra_backend.model.TextContent;
import org.agra.agra_backend.model.TextContentTranslation;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CourseService {
    private static final String DEFAULT_COURSE_IMAGE_URL = "https://res.cloudinary.com/dmumvupow/image/upload/v1759008723/Default_Can_you_name_the_type_of_farming_Rinjhasfamily_is_enga_2_ciduil.webp";
    
    private CloudinaryService cloudinaryService;
    private CourseProgressService courseProgressService;

    private final CourseRepository courseRepository;

public CourseService(CourseRepository courseRepository, CloudinaryService cloudinaryService, CourseProgressService courseProgressService){
    this.cloudinaryService=cloudinaryService;
    this.courseRepository=courseRepository;
    this.courseProgressService=courseProgressService;

}
    @Caching(evict = {
            @CacheEvict(value = {"courses:all", "courses:detail", "courses:country", "courses:domain", "courses:featured", "courses:active"}, allEntries = true)
    })
    public Course createCourse(Course course, MultipartFile courseImage) throws IOException {
        ensureTranslations(course, null);
        ensureTextContentTranslations(course);
        course.setCreatedAt(new java.util.Date());
        course.setUpdatedAt(new java.util.Date());
        
        // Generate IDs for TextContent objects if they don't have them
        generateTextContentIds(course);

        course = courseRepository.save(course);
        System.out.println(course.getImageUrl());
        if (courseImage != null && !courseImage.isEmpty()) {
            course = getCourse(courseImage, course);
        } else {
            // Set default image when no course image is provided
            course.setImageUrl(DEFAULT_COURSE_IMAGE_URL);
            course.setThumbnailUrl(DEFAULT_COURSE_IMAGE_URL);
            course.setDetailImageUrl(DEFAULT_COURSE_IMAGE_URL);
            course = courseRepository.save(course);
        }

        return course;
    }


    @Cacheable(cacheNames = "courses:all", key = "'all'")
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Cacheable(cacheNames = "courses:active", key = "'active'")
    public List<Course> getActiveCourses() {
        return courseRepository.findByArchivedFalse();
    }


    @Cacheable(cacheNames = "courses:detail", key = "#id")
    public Optional<Course> getCourseById(String id) {
        System.out.println("CourseService: getCourseById called with id: " + id);
        Optional<Course> result = courseRepository.findById(id);
        if (result.isPresent()) {
            Course course = result.get();
            CourseTranslation translation = resolveTranslation(course, null);
            String title = translation != null ? translation.getTitle() : null;
            System.out.println("CourseService: Found course - Title: " + title + 
                             ", Archived: " + course.isArchived());
        } else {
            System.out.println("CourseService: Course not found with id: " + id);
        }
        return result;
    }


    @Caching(evict = {
            @CacheEvict(value = {"courses:all", "courses:detail", "courses:country", "courses:domain", "courses:featured", "courses:active"}, allEntries = true)
    })
    public Optional<Course> updateCourse(String id, Course updatedCourse, MultipartFile courseImage) throws IOException {
        return courseRepository.findById(id).map(existingCourse -> {
            try {
                System.out.println(
                        "incoming structure (JSON): " + new ObjectMapper().writeValueAsString(updatedCourse)
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            existingCourse.setDomain(updatedCourse.getDomain());
            existingCourse.setCountry(updatedCourse.getCountry());
            existingCourse.setTrainerId(updatedCourse.getTrainerId());
            existingCourse.setSessionIds(updatedCourse.getSessionIds());
            existingCourse.setLanguagesAvailable(updatedCourse.getLanguagesAvailable());
            ensureTranslations(existingCourse, updatedCourse);
            // Merge files: preserve existing files and add/update from payload
            if (updatedCourse.getFiles() != null) {
                if (existingCourse.getFiles() == null || existingCourse.getFiles().isEmpty()) {
                    existingCourse.setFiles(updatedCourse.getFiles());
                } else {
                    java.util.Map<String, org.agra.agra_backend.model.CourseFile> byId = new java.util.HashMap<>();
                    for (org.agra.agra_backend.model.CourseFile f : existingCourse.getFiles()) {
                        if (f != null && f.getId() != null) byId.put(f.getId(), f);
                    }
                    for (org.agra.agra_backend.model.CourseFile nf : updatedCourse.getFiles()) {
                        if (nf == null) continue;
                        if (nf.getId() != null && byId.containsKey(nf.getId())) {
                            byId.put(nf.getId(), nf);
                        } else {
                            // avoid duplicates by publicId if present
                            boolean duplicate = false;
                            if (nf.getPublicId() != null) {
                                for (org.agra.agra_backend.model.CourseFile ex : byId.values()) {
                                    if (nf.getPublicId().equals(ex.getPublicId())) { duplicate = true; break; }
                                }
                            }
                            if (!duplicate) {
                                String fileId = nf.getId();
                                if (fileId == null || fileId.isEmpty()) {
                                    fileId = java.util.UUID.randomUUID().toString();
                                    nf.setId(fileId);
                                }
                                byId.put(fileId, nf);
                            }
                        }
                    }
                    existingCourse.setFiles(new java.util.ArrayList<>(byId.values()));
                }
            }
            existingCourse.setTextContent(updatedCourse.getTextContent());
            // also update activeCall flag
            existingCourse.setActiveCall(updatedCourse.isActiveCall());
            
            // Generate IDs for TextContent objects if they don't have them
            generateTextContentIds(existingCourse);
            ensureTextContentTranslations(existingCourse);
            
            existingCourse.setUpdatedAt(new java.util.Date());
            System.out.println(existingCourse.getLanguagesAvailable());
            existingCourse = courseRepository.save(existingCourse);
            if (courseImage != null && !courseImage.isEmpty()) {
                try {
                    existingCourse = getCourse(courseImage, existingCourse);
                } catch (IOException e) {
                    System.err.println("Error uploading image: " + e.getMessage()); 
                    throw new RuntimeException("Failed to upload image", e);
                }
            } else if (existingCourse.getImageUrl() == null || existingCourse.getImageUrl().isEmpty()) {
                // Set default image if course doesn't have any image
                existingCourse.setImageUrl(DEFAULT_COURSE_IMAGE_URL);
                existingCourse.setThumbnailUrl(DEFAULT_COURSE_IMAGE_URL);
                existingCourse.setDetailImageUrl(DEFAULT_COURSE_IMAGE_URL);
                existingCourse = courseRepository.save(existingCourse);
            }

            return existingCourse;
        });
    }

    private Course getCourse(MultipartFile courseImage, Course existingCourse) throws IOException {
        String folderPath = "courses/" + existingCourse.getId();
        Map<String, Object> uploadResult = cloudinaryService.uploadImageToFolder(courseImage, folderPath);
        String imageUrl = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");

        existingCourse.setImageUrl(imageUrl);
        existingCourse.setImagePublicId(publicId);

        String baseUrl = imageUrl.substring(0, imageUrl.lastIndexOf('/') + 1);
        String filename = publicId.substring(publicId.lastIndexOf('/') + 1);
        existingCourse.setThumbnailUrl(baseUrl + "c_fill,w_300,h_200/" + filename);
        existingCourse.setDetailImageUrl(baseUrl + "c_fit,w_800,h_600/" + filename);

        existingCourse = courseRepository.save(existingCourse);
        return existingCourse;
    }

    @Caching(evict = {
            @CacheEvict(value = {"courses:all", "courses:detail", "courses:country", "courses:domain", "courses:featured", "courses:active"}, allEntries = true)
    })
    public void deleteCourse(String id) {
        System.out.println("CourseService: Deleting course with id: " + id);
        
        // First, delete all enrollments for this course to prevent orphaned records
        try {
            List<CourseProgress> enrollments = courseProgressService.getCourseEnrollments(id);
            System.out.println("CourseService: Found " + enrollments.size() + " enrollments to delete");
            
            for (CourseProgress enrollment : enrollments) {
                courseProgressService.unenrollUser(enrollment.getUserId(), id);
                System.out.println("CourseService: Deleted enrollment for user: " + enrollment.getUserId());
            }
        } catch (Exception e) {
            System.err.println("CourseService: Error deleting enrollments for course " + id + ": " + e.getMessage());
            // Continue with course deletion even if enrollment cleanup fails
        }
        
        // Then delete the course
        courseRepository.deleteById(id);
        System.out.println("CourseService: Successfully deleted course: " + id);
    }


    @Caching(evict = {
            @CacheEvict(value = {"courses:all", "courses:detail", "courses:country", "courses:domain", "courses:featured", "courses:active"}, allEntries = true)
    })
    public void ArchiveCourse(String id) {
        Optional<Course> courseOpt = courseRepository.findById(id);
        if (courseOpt.isPresent()) {
            Course course = courseOpt.get();
            course.setArchived(!course.isArchived());
            course.setUpdatedAt(new Date());
            courseRepository.save(course);
        } else {
            throw new RuntimeException("Course not found with id: " + id);
        }
    }


    @Cacheable(cacheNames = "courses:country", key = "#country")
    public List<Course> getCoursesByCountry(String country) {
        return courseRepository.findByCountry(country);
    }


    @Cacheable(cacheNames = "courses:domain", key = "#domain")
    public List<Course> getCoursesByDomain(String domain) {
        return courseRepository.findByDomain(domain);
    }
    
    @Caching(evict = {
            @CacheEvict(value = {"courses:all", "courses:detail", "courses:country", "courses:domain", "courses:featured", "courses:active"}, allEntries = true)
    })
    public Course save(Course course) {
        return courseRepository.save(course);
    }
    
    /**
     * Generates unique IDs for TextContent objects that don't have them
     */
    private void generateTextContentIds(Course course) {
        if (course.getTextContent() != null) {
            course.getTextContent().forEach(textContent -> {
                if (textContent.getId() == null || textContent.getId().isEmpty()) {
                    textContent.setId(UUID.randomUUID().toString());
                }
                if (textContent.getQuizQuestions() != null) {
                    textContent.getQuizQuestions().forEach(question -> {
                        if (question.getId() == null || question.getId().isEmpty()) {
                            question.setId(UUID.randomUUID().toString());
                        }
                        if (question.getAnswers() != null) {
                            question.getAnswers().forEach(answer -> {
                                if (answer.getId() == null || answer.getId().isEmpty()) {
                                    answer.setId(UUID.randomUUID().toString());
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    private void ensureTextContentTranslations(Course course) {
        if (course == null || course.getTextContent() == null) return;
        String defaultLanguage = "en";
        for (TextContent textContent : course.getTextContent()) {
            if (textContent == null) continue;
            Map<String, TextContentTranslation> merged = new HashMap<>();
            if (textContent.getTranslations() != null) {
                merged.putAll(textContent.getTranslations());
            }
            textContent.setTranslations(merged.isEmpty() ? null : merged);

            if (textContent.getQuizQuestions() != null) {
                for (QuizQuestion question : textContent.getQuizQuestions()) {
                    ensureQuizQuestionTranslations(question, defaultLanguage);
                    if (question != null && question.getAnswers() != null) {
                        for (QuizAnswer answer : question.getAnswers()) {
                            ensureQuizAnswerTranslations(answer, defaultLanguage);
                        }
                    }
                }
            }
        }
    }

    private void ensureQuizQuestionTranslations(QuizQuestion question, String defaultLanguage) {
        if (question == null) return;
        Map<String, QuizQuestionTranslation> merged = new HashMap<>();
        if (question.getTranslations() != null) {
            merged.putAll(question.getTranslations());
        }
        question.setTranslations(merged.isEmpty() ? null : merged);
    }

    private void ensureQuizAnswerTranslations(QuizAnswer answer, String defaultLanguage) {
        if (answer == null) return;
        Map<String, QuizAnswerTranslation> merged = new HashMap<>();
        if (answer.getTranslations() != null) {
            merged.putAll(answer.getTranslations());
        }
        answer.setTranslations(merged.isEmpty() ? null : merged);
    }

    public Course localizeCourse(Course course, Locale locale) {
        if (course == null) return null;
        Course localized = new Course();
        BeanUtils.copyProperties(course, localized);
        if (course.getTranslations() == null || course.getTranslations().isEmpty()) {
            localized.setTextContent(localizeTextContents(course.getTextContent(), locale, course.getDefaultLanguage()));
            return localized;
        }

        CourseTranslation translation = resolveTranslation(course, locale);

        if (translation != null) {
            if (translation.getTitle() != null) localized.setTitle(translation.getTitle());
            if (translation.getDescription() != null) localized.setDescription(translation.getDescription());
            if (translation.getGoals() != null) localized.setGoals(translation.getGoals());
        }

        localized.setTextContent(localizeTextContents(course.getTextContent(), locale, course.getDefaultLanguage()));
        return localized;
    }

    public List<Course> localizeCourses(List<Course> courses, Locale locale) {
        if (courses == null || courses.isEmpty()) return courses;
        List<Course> localized = new java.util.ArrayList<>(courses.size());
        for (Course c : courses) {
            localized.add(localizeCourse(c, locale));
        }
        return localized;
    }

    private void ensureTranslations(Course target, Course source) {
        if (target == null) return;
        String defaultLanguage = resolveDefaultLanguage(source != null ? source.getDefaultLanguage() : null,
                target.getDefaultLanguage());
        target.setDefaultLanguage(defaultLanguage);

        Map<String, CourseTranslation> merged = new HashMap<>();
        if (target.getTranslations() != null) {
            merged.putAll(target.getTranslations());
        }
        if (source != null && source.getTranslations() != null) {
            for (Map.Entry<String, CourseTranslation> entry : source.getTranslations().entrySet()) {
                mergeTranslation(merged, entry.getKey(), entry.getValue());
            }
        }

        CourseTranslation fromFields = buildTranslationFromFields(source);
        if (fromFields != null) {
            mergeTranslation(merged, defaultLanguage, fromFields);
        } else if (merged.isEmpty()) {
            CourseTranslation fallback = buildTranslationFromFields(target);
            if (fallback != null) {
                mergeTranslation(merged, defaultLanguage, fallback);
            }
        }

        target.setTranslations(merged.isEmpty() ? null : merged);
    }

    private String resolveDefaultLanguage(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) return preferred;
        if (fallback != null && !fallback.isBlank()) return fallback;
        return "en";
    }

    private CourseTranslation buildTranslationFromFields(Course course) {
        if (course == null) return null;
        if (course.getTitle() == null && course.getDescription() == null && course.getGoals() == null) {
            return null;
        }
        CourseTranslation translation = new CourseTranslation();
        translation.setTitle(course.getTitle());
        translation.setDescription(course.getDescription());
        translation.setGoals(course.getGoals());
        return translation;
    }

    private void mergeTranslation(Map<String, CourseTranslation> merged,
                                  String language,
                                  CourseTranslation update) {
        if (merged == null || update == null) return;
        String key = (language != null && !language.isBlank()) ? language : "en";
        CourseTranslation existing = merged.getOrDefault(key, new CourseTranslation());
        if (update.getTitle() != null) existing.setTitle(update.getTitle());
        if (update.getDescription() != null) existing.setDescription(update.getDescription());
        if (update.getGoals() != null) existing.setGoals(update.getGoals());
        merged.put(key, existing);
    }

    private CourseTranslation resolveTranslation(Course course, Locale locale) {
        if (course == null || course.getTranslations() == null || course.getTranslations().isEmpty()) return null;
        return resolveTranslation(course.getTranslations(), locale, course.getDefaultLanguage());
    }

    private List<TextContent> localizeTextContents(List<TextContent> contents,
                                                   Locale locale,
                                                   String defaultLanguage) {
        if (contents == null || contents.isEmpty()) return contents;
        List<TextContent> localized = new java.util.ArrayList<>(contents.size());
        for (TextContent content : contents) {
            if (content == null) continue;
            TextContent localizedContent = new TextContent();
            BeanUtils.copyProperties(content, localizedContent);
            localizedContent.setQuizQuestions(localizeQuizQuestions(content.getQuizQuestions(), locale, defaultLanguage));
            localized.add(localizedContent);
        }
        return localized;
    }

    private List<QuizQuestion> localizeQuizQuestions(List<QuizQuestion> questions,
                                                     Locale locale,
                                                     String defaultLanguage) {
        if (questions == null || questions.isEmpty()) return questions;
        List<QuizQuestion> localized = new java.util.ArrayList<>(questions.size());
        for (QuizQuestion question : questions) {
            if (question == null) continue;
            QuizQuestion localizedQuestion = new QuizQuestion();
            BeanUtils.copyProperties(question, localizedQuestion);
            localizedQuestion.setAnswers(localizeQuizAnswers(question.getAnswers(), locale, defaultLanguage));
            localized.add(localizedQuestion);
        }
        return localized;
    }

    private List<QuizAnswer> localizeQuizAnswers(List<QuizAnswer> answers,
                                                 Locale locale,
                                                 String defaultLanguage) {
        if (answers == null || answers.isEmpty()) return answers;
        List<QuizAnswer> localized = new java.util.ArrayList<>(answers.size());
        for (QuizAnswer answer : answers) {
            if (answer == null) continue;
            QuizAnswer localizedAnswer = new QuizAnswer();
            BeanUtils.copyProperties(answer, localizedAnswer);
            localized.add(localizedAnswer);
        }
        return localized;
    }

    private <T> T resolveTranslation(Map<String, T> translations, Locale locale, String defaultLanguage) {
        if (translations == null || translations.isEmpty()) return null;
        String language = locale != null ? locale.getLanguage() : null;
        T translation = null;
        if (language != null && !language.isBlank()) {
            translation = translations.get(language);
            if (translation == null) {
                translation = translations.get(language.toLowerCase());
            }
        }
        if (translation == null && locale != null) {
            translation = translations.get(locale.toString());
        }
        if (translation == null && defaultLanguage != null) {
            translation = translations.get(defaultLanguage);
        }
        if (translation == null) {
            translation = translations.get("en");
        }
        if (translation == null) {
            translation = translations.values().stream().findFirst().orElse(null);
        }
        return translation;
    }
}
