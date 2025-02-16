package dev.group.cybershield.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Table(name = "test_master")
@Entity
public class TestMaster extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_id")
    public Integer testId;

    @Column(name = "user_id")
    public Integer userId;

    public Double score; // can be null

    public String category;

    public String level;

    @Column(name = "test_status")
    public String testStatus;


}
