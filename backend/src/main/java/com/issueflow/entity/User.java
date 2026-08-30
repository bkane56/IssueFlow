package com.issueflow.entity;

import com.issueflow.constants.ValidationConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = ValidationConstants.NAME_MAX_LENGTH)
    private String name;

    @Column(nullable = false, unique = true, length = ValidationConstants.EMAIL_MAX_LENGTH)
    private String email;

    @Column(nullable = false)
    private boolean active = true;

    public User(String name, String email, boolean active) {
        this.name = name;
        this.email = email;
        this.active = active;
    }
}
