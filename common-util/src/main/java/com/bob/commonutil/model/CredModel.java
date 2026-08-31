package com.bob.commonutil.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CredModel {
    private String email;
    private String password;
}
