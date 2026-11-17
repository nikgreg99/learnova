package com.learnova.service.security;

import com.learnova.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("quizSecurityService")
@RequiredArgsConstructor
public class QuizSecurityService {

    private final QuizRepository quizRepository;
    private final CourseSecurityService courseSecurityService;

    public boolean isQuizOwner(Long quizId) {
        return quizRepository.findById(quizId)
                .map(quiz -> courseSecurityService.isOwner(quiz.getCourse().getId()))
                .orElse(false);
    }

    public boolean canView(Long quizId) {
        return quizRepository.findById(quizId)
                .map(quiz -> courseSecurityService.canView(quiz.getCourse().getId()))
                .orElse(false);
    }
}
