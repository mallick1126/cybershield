package dev.group.cybershield.quiz.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class QuizDTO {

    @NotNull(message = "userId is mandatory", groups = {GetQuizGroup.class, SubmitQuizGroup.class, ViewQuizGroup.class} )
    private Integer userId;

    @NotNull(message = "testId is mandatory", groups = {SubmitQuizGroup.class, ViewQuizGroup.class})
    private Integer testId;

    private Double score;

    private String grade;

    @NotEmpty(message = "questionsList is mandatory", groups = {SubmitQuizGroup.class})
    private List<@Valid QuestionDTO> questionsList;

    public interface GetQuizGroup {
    }

    public interface SubmitQuizGroup {
    }

    public interface ViewQuizGroup {
    }
}

