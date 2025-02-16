package dev.group.cybershield.quiz.service;

import dev.group.cybershield.common.constants.CommonConstants;
import dev.group.cybershield.common.exception.BadRequestException;
import dev.group.cybershield.entity.*;
import dev.group.cybershield.quiz.QuizConstants;
import dev.group.cybershield.quiz.model.OptionDTO;
import dev.group.cybershield.quiz.model.QuestionDTO;
import dev.group.cybershield.quiz.model.QuizDTO;
import dev.group.cybershield.repository.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QuizServiceImpl implements QuizService {

    @Autowired
    private QuestionsRepo questionsRepo;

    @Autowired
    private AnswersRepo answersRepo;

    @Autowired
    private TestMasterRepo testMasterRepo;

    @Autowired
    private TestQuestionMapRepo testQuestionMapRepo;

    @Autowired
    private UsersRepo usersRepo;

    @Value("${number.of.question.in.quiz}")
    private Integer totalQuizQuestionsCount;

    @Override
    public QuizDTO getQuiz(QuizDTO reqBody) throws Exception {
        try {
            log.info("getQuiz_Service_userId : {}", reqBody.getUserId());
            QuizDTO quiz;
            List<TestMaster> unattemptedQuizForUser = testMasterRepo.findByUserIdAndTestStatusAndStatus(reqBody.getUserId(), QuizConstants.TEST_STATUS_INIT, CommonConstants.STATUS_A);
            if (unattemptedQuizForUser != null && !unattemptedQuizForUser.isEmpty()) {
                quiz = findQuizById(unattemptedQuizForUser.get(0).getTestId(), false);
            } else {
                quiz = generateQuiz(reqBody.getUserId());
            }
            return quiz;
        } catch (Exception e) {
            log.error("getQuiz_Service_error: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public QuizDTO submitQuiz(QuizDTO reqBody) throws Exception {
        try {
            log.info("submitTest_Service_reqBody : {}", reqBody);
            int correctQuestionCount = 0;
            QuizDTO submitQuiz = new QuizDTO();

            Optional<TestMaster> testOpt = testMasterRepo.findById(reqBody.getTestId());
            if (testOpt.isEmpty()) {
                throw new BadRequestException("Quiz does not exist !");
            }

            TestMaster test = testOpt.get();
            if (test.getTestStatus().equalsIgnoreCase(QuizConstants.TEST_STATUS_COMPLETED)) {
                throw new BadRequestException("Quiz already submitted !");
            }

            // get all question form test question map by test id and update its user_answer_id and save again
            List<TestQuestionMap> questionsToBeSubmitted = testQuestionMapRepo.findByTestId(reqBody.getTestId());
            List<Integer> questionIds = questionsToBeSubmitted.stream().map(TestQuestionMap::getQuestionId).toList();
            List<Answers> correctAnswersForAllQuestionsToBeSubmitted = answersRepo.findByQuestionIdInAndIsCorrect(questionIds, QuizConstants.CORRECT_ANSWERS);

            for (TestQuestionMap questionToSubmit : questionsToBeSubmitted) {
                Optional<QuestionDTO> givenQuestionOpt = reqBody.getQuestionsList().stream().filter(que -> que.getQuestionId().equals(questionToSubmit.getQuestionId())).findFirst();
                if (givenQuestionOpt.isPresent()) {
                    QuestionDTO givenQuestion = givenQuestionOpt.get();
                    questionToSubmit.setUsersAnswerId(givenQuestion.getSelectedOptionId());
                    Optional<Answers> correctAnswersOpt = correctAnswersForAllQuestionsToBeSubmitted.stream().filter(ans -> ans.getQuestionId().equals(givenQuestion.getQuestionId())).findFirst();
                    if (correctAnswersOpt.isPresent() && correctAnswersOpt.get().getAnswerId().equals(givenQuestion.getSelectedOptionId()))
                        correctQuestionCount++;
                }
            }
            testQuestionMapRepo.saveAll(questionsToBeSubmitted);

            double score = ((double) correctQuestionCount / totalQuizQuestionsCount) * 100.00;
            submitQuiz.setScore(score);

            if (score >= 80.00) {
                submitQuiz.setGrade(QuizConstants.TEST_PASSED);
            } else {
                submitQuiz.setGrade(QuizConstants.TEST_FAILED);
            }

            test.setScore(score);
            test.setTestStatus(QuizConstants.TEST_STATUS_COMPLETED);
            testMasterRepo.save(test);
            return submitQuiz;
        } catch (Exception e) {
            log.error("submitTest_Service_Error: {}", e.getMessage());
            throw new Exception(e);
        }
    }

    @Override
    public QuizDTO viewQuiz(QuizDTO reqBody) throws Exception {
        Optional<Users> userOpt = usersRepo.findById(reqBody.getUserId());
        Users user = userOpt.orElseThrow(() -> new BadRequestException("User not found !"));

        QuizDTO viewQuiz = findQuizById(reqBody.getTestId(), true);

        if (viewQuiz.getUserId() != user.getUserId())
            throw new BadRequestException("Quiz is not assigned to this user");

        return findQuizById(reqBody.getTestId(), true);
    }

    @Transactional
    private QuizDTO generateQuiz(Integer userId) throws Exception {
        try {
            log.info("generateQuiz_Service userId:{}", userId);

            QuizDTO quiz = new QuizDTO();
            usersRepo.findById(userId).ifPresentOrElse(
                    (user) -> {
                        quiz.setUserId(user.getUserId());
                    },
                    () -> {
                        throw new BadRequestException("User does not exist !");
                    }
            );

            // generateQuiz from QUESTIONS
            List<QuestionDTO> questionDTOList = new ArrayList<>();
            List<Questions> questionList = questionsRepo.getRandomQuestions(totalQuizQuestionsCount);
            List<Integer> questionIds = questionList.stream().map(Questions::getQuestionId).toList();
            List<Answers> answersListForAllQuestions = answersRepo.findByQuestionIdInAndStatus(questionIds, CommonConstants.STATUS_A);

            // saveQuiz in TEST_MASTER
            TestMaster quizMaster = new TestMaster();
            quizMaster.setUserId(userId);
            quizMaster.setTestStatus(QuizConstants.TEST_STATUS_INIT);
            quizMaster.setStatus(CommonConstants.STATUS_A);
            TestMaster savedQuiz = testMasterRepo.save(quizMaster);
            quiz.setTestId(savedQuiz.getTestId());
            log.info("generateQuiz_Service userId:{} and testId:{} ", userId, savedQuiz.getTestId());


            for (Questions question : questionList) {
                QuestionDTO questionDTO = new QuestionDTO();
                questionDTO.setQuestionId(question.getQuestionId());
                questionDTO.setQuestion(question.getQuestion());

                // saveQuestion in TEST_QUESTION_MAP table
                TestQuestionMap generatedQuestion = new TestQuestionMap();
                generatedQuestion.setTestId(savedQuiz.getTestId());
                generatedQuestion.setQuestionId(question.getQuestionId());
                generatedQuestion.setStatus(CommonConstants.STATUS_A);

                // get the answers for current question and shuffle them
                List<Answers> answersList = new ArrayList<>(answersListForAllQuestions.stream().filter(answers -> answers.getQuestionId().equals(question.getQuestionId())).toList());
                Collections.shuffle(answersList);

                // set the Answers in optionDTO and TEST_QUESTION_MAP in sequence
                List<OptionDTO> optionList = new ArrayList<OptionDTO>();
                int count = 1;
                for (Answers ans : answersList) {
                    OptionDTO option = new OptionDTO();
                    option.setOptionId(ans.getAnswerId());
                    option.setOption(ans.getAnswer());
                    optionList.add(option);

                    // set option in TEST_QUESTION_MAP
                    switch (count) {
                        case 1:
                            generatedQuestion.setGivenOption1(ans.getAnswerId());
                            break;
                        case 2:
                            generatedQuestion.setGivenOption2(ans.getAnswerId());
                            break;
                        case 3:
                            generatedQuestion.setGivenOption3(ans.getAnswerId());
                            break;
                        case 4:
                            generatedQuestion.setGivenOption4(ans.getAnswerId());
                            break;
                    }
                    count++;
                }
                questionDTO.setOptions(optionList);  // set optionList in questionDTO
                questionDTOList.add(questionDTO);  // set questionDTO in questionDTO list
                testQuestionMapRepo.save(generatedQuestion); // save testQuestionMap
            }
            quiz.setQuestionsList(questionDTOList);
            return quiz;
        } catch (Exception e) {
            log.error("generateQuiz_Service_error: {}", e.getMessage());
            throw e;
        }
    }

    @SneakyThrows
    private QuizDTO findQuizById(Integer quizId, boolean isView) {
        try {
            log.info("findQuizById {}", quizId);
            QuizDTO quiz = new QuizDTO();

            Optional<TestMaster> quizTobeViewedOpt = testMasterRepo.findById(quizId);
            if (quizTobeViewedOpt.isPresent()) {
                // prepare data for the viewQuiz response
                List<TestQuestionMap> questionsOfQuiz = testQuestionMapRepo.findByTestId(quizId);
                List<Integer> questionIds = questionsOfQuiz.stream().map(TestQuestionMap::getQuestionId).toList();
                List<Integer> answersIds = questionsOfQuiz.stream()
                        .map(que -> List.of(que.getGivenOption1(), que.getGivenOption2(), que.getGivenOption3(), que.getGivenOption4()))
                        .reduce(new ArrayList<>(), (accumulator, current) -> {
                            accumulator.addAll(current);
                            return accumulator;
                        });
                List<Questions> questions = questionsRepo.findAllById(questionIds);
                List<Answers> answers = answersRepo.findAllById(answersIds);
                Map<Integer, Questions> questionsMap = questions.stream().collect(Collectors.toMap(que -> que.getQuestionId(), que -> que));
                Map<Integer, Answers> answersMap = answers.stream().collect(Collectors.toMap(ans -> ans.getAnswerId(), ans -> ans));

                // start preparing viewQuiz response
                TestMaster quizTobeViewed = quizTobeViewedOpt.get();
                quiz.setUserId(quizTobeViewed.getUserId());
                quiz.setTestId(quizTobeViewed.getTestId());
                quiz.setScore(quizTobeViewed.getScore());
                List<QuestionDTO> questionDTOList = new ArrayList<>();
                for (TestQuestionMap question : questionsOfQuiz) {
                    QuestionDTO questionDTO = new QuestionDTO();
                    questionDTO.setQuestionId(question.getQuestionId());
                    questionDTO.setQuestion(questionsMap.get(question.getQuestionId()).getQuestion());
                    questionDTO.setSelectedOptionId(question.getUsersAnswerId());

                    List<OptionDTO> optionDTOList = new ArrayList<>();

                    for (int i = 1; i <= 4; i++) {
                        // this will get us the all the options for the current question in sequence
                        String fieldName = "givenOption" + i;
                        Integer fieldValue = (Integer) question.getClass().getDeclaredField(fieldName).get(question);
                        OptionDTO optionDTO = new OptionDTO();
                        optionDTO.setOptionId(fieldValue);
                        optionDTO.setOption(answersMap.get(fieldValue).getAnswer());
                        optionDTOList.add(optionDTO);
                        if (isView && answersMap.get(fieldValue).getIsCorrect().equalsIgnoreCase(QuizConstants.CORRECT_ANSWERS)) {
                            questionDTO.setCorrectOptionId(fieldValue);
                        }
                    }

                    questionDTO.setOptions(optionDTOList);
                    questionDTOList.add(questionDTO);
                }
                quiz.setQuestionsList(questionDTOList);
            } else {
                throw new BadRequestException("Quiz does not exist !");
            }
            return quiz;
        } catch (Exception e) {
            log.error("findQuizById Service Error {}", e.getMessage());
            throw e;
        }
    }

}
