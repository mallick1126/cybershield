package dev.group.cybershield.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Questions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long question_id;

    private String question;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private String status;
    private String created_on;
    private String updated_on;
}
