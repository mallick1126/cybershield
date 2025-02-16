package dev.group.cybershield.quiz.service;

import dev.group.cybershield.quiz.model.QuizDTO;

public interface QuizService {

    QuizDTO getQuiz(QuizDTO reqBody) throws Exception;

    QuizDTO submitQuiz(QuizDTO reqBody) throws Exception;

    QuizDTO viewQuiz(QuizDTO reqBody) throws Exception;

}
