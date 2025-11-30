package org.agra.agra_backend.service;


import org.agra.agra_backend.dao.CourseRepository;
import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.model.CourseProgress;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;
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
            @CacheEvict(value = {"courses:all", "courses:detail", "courses:country", "courses:domain", "courses:featured"}, allEntries = true)
    })
    public Course createCourse(Course course, MultipartFile courseImage) throws IOException {
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


    @Cacheable(cacheNames = "courses:detail", key = "#id")
    public Optional<Course> getCourseById(String id) {
        System.out.println("CourseService: getCourseById called with id: " + id);
        Optional<Course> result = courseRepository.findById(id);
        if (result.isPresent()) {
            Course course = result.get();
            System.out.println("CourseService: Found course - Title: " + course.getTitle() + 
                             ", Archived: " + course.isArchived());
        } else {
            System.out.println("CourseService: Course not found with id: " + id);
        }
        return result;
    }


    @Caching(evict = {
            @CacheEvict(value = {"courses:all", "courses:detail", "courses:country", "courses:domain", "courses:featured"}, allEntries = true)
    })
    public Optional<Course> updateCourse(String id, Course updatedCourse, MultipartFile courseImage) throws IOException {
        return courseRepository.findById(id).map(existingCourse -> {

            existingCourse.setTitle(updatedCourse.getTitle());
            existingCourse.setDescription(updatedCourse.getDescription());
            existingCourse.setGoals(updatedCourse.getGoals());
            existingCourse.setDomain(updatedCourse.getDomain());
            existingCourse.setCountry(updatedCourse.getCountry());
            existingCourse.setTrainerId(updatedCourse.getTrainerId());
            existingCourse.setSessionIds(updatedCourse.getSessionIds());
            existingCourse.setLanguagesAvailable(updatedCourse.getLanguagesAvailable());
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
            @CacheEvict(value = {"courses:all", "courses:detail", "courses:country", "courses:domain", "courses:featured"}, allEntries = true)
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
            @CacheEvict(value = {"courses:all", "courses:detail", "courses:country", "courses:domain", "courses:featured"}, allEntries = true)
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
            @CacheEvict(value = {"courses:all", "courses:detail", "courses:country", "courses:domain", "courses:featured"}, allEntries = true)
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
            });
        }
    }
}
