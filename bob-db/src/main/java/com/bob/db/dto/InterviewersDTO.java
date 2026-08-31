package com.bob.db.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
public class InterviewersDTO extends BaseDTO implements Serializable {

    @JsonProperty("interviewerId")
    private UUID id;

    private String firstName;

    private String lastName;

    private String fullName;

    private String email;

}
