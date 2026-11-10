package com.learnova.service;

import com.learnova.config.security.SecurityUtils;
import com.learnova.dto.rating.RatingRequest;
import com.learnova.dto.rating.RatingResponse;
import com.learnova.dto.rating.RatingSummaryResponse;
import com.learnova.exception.CourseNotFoundException;
import com.learnova.exception.DuplicateRatingException;
import com.learnova.exception.KeycloakUserNotFoundException;
import com.learnova.exception.LessonNotFoundException;
import com.learnova.exception.RatingNotFoundException;
import com.learnova.mapper.RatingMapper;
import com.learnova.model.Course;
import com.learnova.model.Lesson;
import com.learnova.model.Rating;
import com.learnova.model.User;
import com.learnova.repository.CourseRepository;
import com.learnova.repository.LessonRepository;
import com.learnova.repository.RatingRepository;
import com.learnova.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RatingService {

    private final RatingRepository ratingRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final RatingMapper ratingMapper;

    public RatingService(RatingRepository ratingRepository, CourseRepository courseRepository,
            LessonRepository lessonRepository, UserRepository userRepository, RatingMapper ratingMapper) {
        this.ratingRepository = ratingRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.ratingMapper = ratingMapper;
    }

    public RatingResponse createCourseRating(Long courseId, RatingRequest request) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new CourseNotFoundException(courseId));

        User user = getCurrentUser();

        if (ratingRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            throw new DuplicateRatingException("User has already submitted a rating for this course");
        }

        Rating rating = ratingMapper.toEntity(request);
        rating.setUser(user);
        rating.setCourse(course);

        Rating saved = ratingRepository.save(rating);
        return ratingMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> getCourseRatings(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }
        return ratingRepository.findByCourseId(courseId).stream().map(ratingMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse getCourseRatingSummary(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }
        Double avg = ratingRepository.calculateAverageRatingByCourseId(courseId)
                .map(val -> Math.round(val * 10.0) / 10.0).orElse(null);
        long count = ratingRepository.countByCourseId(courseId);
        return new RatingSummaryResponse(avg, count);
    }

    @Transactional(readOnly = true)
    public RatingResponse getMyCourseRating(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }
        User user = getCurrentUser();
        return ratingRepository.findByUserIdAndCourseId(user.getId(), courseId).map(ratingMapper::toResponse)
                .orElseThrow(() -> new RatingNotFoundException("Rating not found for this course"));
    }

    public RatingResponse updateMyCourseRating(Long courseId, RatingRequest request) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }
        User user = getCurrentUser();
        Rating rating = ratingRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new RatingNotFoundException("Rating not found for this course"));

        ratingMapper.updateRating(request, rating);
        Rating updated = ratingRepository.save(rating);
        return ratingMapper.toResponse(updated);
    }

    public void deleteMyCourseRating(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }
        User user = getCurrentUser();
        Rating rating = ratingRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new RatingNotFoundException("Rating not found for this course"));

        ratingRepository.delete(rating);
    }

    public RatingResponse createLessonRating(Long lessonId, RatingRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new LessonNotFoundException(lessonId));

        User user = getCurrentUser();

        if (ratingRepository.existsByUserIdAndLessonId(user.getId(), lessonId)) {
            throw new DuplicateRatingException("User has already submitted a rating for this lesson");
        }

        Rating rating = ratingMapper.toEntity(request);
        rating.setUser(user);
        rating.setLesson(lesson);

        Rating saved = ratingRepository.save(rating);
        return ratingMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> getLessonRatings(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }
        return ratingRepository.findByLessonId(lessonId).stream().map(ratingMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RatingSummaryResponse getLessonRatingSummary(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }
        Double avg = ratingRepository.calculateAverageRatingByLessonId(lessonId)
                .map(val -> Math.round(val * 10.0) / 10.0).orElse(null);
        long count = ratingRepository.countByLessonId(lessonId);
        return new RatingSummaryResponse(avg, count);
    }

    @Transactional(readOnly = true)
    public RatingResponse getMyLessonRating(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }
        User user = getCurrentUser();
        return ratingRepository.findByUserIdAndLessonId(user.getId(), lessonId).map(ratingMapper::toResponse)
                .orElseThrow(() -> new RatingNotFoundException("Rating not found for this lesson"));
    }

    public RatingResponse updateMyLessonRating(Long lessonId, RatingRequest request) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }
        User user = getCurrentUser();
        Rating rating = ratingRepository.findByUserIdAndLessonId(user.getId(), lessonId)
                .orElseThrow(() -> new RatingNotFoundException("Rating not found for this lesson"));

        ratingMapper.updateRating(request, rating);
        Rating updated = ratingRepository.save(rating);
        return ratingMapper.toResponse(updated);
    }

    public void deleteMyLessonRating(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }
        User user = getCurrentUser();
        Rating rating = ratingRepository.findByUserIdAndLessonId(user.getId(), lessonId)
                .orElseThrow(() -> new RatingNotFoundException("Rating not found for this lesson"));

        ratingRepository.delete(rating);
    }

    private User getCurrentUser() {
        String keycloakId = SecurityUtils.getCurrentKeycloakId();
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new KeycloakUserNotFoundException(keycloakId));
    }
}
