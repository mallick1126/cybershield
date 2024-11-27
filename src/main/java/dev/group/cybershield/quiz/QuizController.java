package dev.group.cybershield.quiz;

import dev.group.cybershield.entity.Questions;
import dev.group.cybershield.repository.QuestionRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
public class QuizController {

    @Autowired
    private QuestionRepo questionRepo;

    @GetMapping("/testing")
    public Questions testController(){
        Optional<Questions> questionData = questionRepo.findById(1L);
        log.info("fetched data from database " + questionData.get());
        return questionData.get();
    }
}
