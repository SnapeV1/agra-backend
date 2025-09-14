package org.agra.agra_backend.service;


import org.agra.agra_backend.dao.CourseRepository;
import org.agra.agra_backend.model.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CourseService {
    private CloudinaryService cloudinaryService;

    private final CourseRepository courseRepository;

public CourseService(CourseRepository courseRepository,CloudinaryService cloudinaryService){
    this.cloudinaryService=cloudinaryService;
    this.courseRepository=courseRepository;

}
    public Course createCourse(Course course, MultipartFile courseImage) throws IOException {
        course.setCreatedAt(new java.util.Date());
        course.setUpdatedAt(new java.util.Date());

        course = courseRepository.save(course);
        System.out.println(course.getImageUrl());
        if (courseImage != null && !courseImage.isEmpty()) {
            course = getCourse(courseImage, course);
        }

        return course;
    }


    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }


    public Optional<Course> getCourseById(String id) {
        return courseRepository.findById(id);
    }


    public Optional<Course> updateCourse(String id, Course updatedCourse, MultipartFile courseImage) throws IOException {
        return courseRepository.findById(id).map(existingCourse -> {

            existingCourse.setTitle(updatedCourse.getTitle());
            existingCourse.setDescription(updatedCourse.getDescription());
            existingCourse.setDomain(updatedCourse.getDomain());
            existingCourse.setCountry(updatedCourse.getCountry());
            existingCourse.setUpdatedAt(new java.util.Date());
            existingCourse.setLanguagesAvailable(updatedCourse.getLanguagesAvailable());
            System.out.println(existingCourse.getLanguagesAvailable());
            existingCourse = courseRepository.save(existingCourse);
            if (courseImage != null && !courseImage.isEmpty()) {
                try {
                    existingCourse = getCourse(courseImage, existingCourse);
                } catch (IOException e) {
                    System.err.println("Error uploading image: " + e.getMessage());
                    throw new RuntimeException("Failed to upload image", e);
                }
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

    public void deleteCourse(String id) {
        courseRepository.deleteById(id);
    }


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


    public List<Course> getCoursesByCountry(String country) {
        return courseRepository.findByCountry(country);
    }


    public List<Course> getCoursesByDomain(String domain) {
        return courseRepository.findByDomain(domain);
    }
}
