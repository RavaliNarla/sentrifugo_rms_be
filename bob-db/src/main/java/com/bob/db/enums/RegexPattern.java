package com.bob.db.enums;

import lombok.Data;


public enum RegexPattern {

    ALPHA_SPACE("^[A-Za-z\\s]*$"),
    ALPHA_NUMERIC_SPACE("^[A-Za-z0-9\\s]*$"),
    ALPHA_NUMERIC_SPACE_DASH_AMP("^[A-Za-z0-9 _\\-&/\\\\():]*$"),
    TEXTAREA_BASIC("^[A-Za-z0-9\\s.,\\-/()&'’:]*$"),
    NUMBERS_ONLY("^[0-9]*$"),
    ALPHA_NUMERIC_SPACE_AMP_DASH_UNDERSCORE_AT("^[A-Za-z0-9 _\\-&]*$"),
    NUMERIC_SPACE("^[A-Za-z0-9\\s]*$"),
    ALPHA_NUMERIC_SPACE_SPECIAL_BASIC("^[A-Za-z0-9 _\\-()/&]*$"),
    ALPHA_NUMERIC_SPACE_PUNCTUATION("^[A-Za-z0-9 _.,\\-():;&/]*$"),
    TITLE_ALPHANUMERIC_SPACE_PUNCTUATION("^[A-Za-z0-9 _.,\\-():;'&/]*$"),
    DESCRIPTION_ALPHA_NUMERIC_SPACE_PUNCTUATION("^[A-Za-z0-9\\s.,\\-_/()&:;'\"@#]*$"), //+% not present
    ADDRESS_LINE_PUNCTUATION("^[A-Za-z0-9\\s.,\\-_/()&:;'\"@#]*$"),
    REQ_DESCRIPTION_PUNCTUATION("^[A-Za-z0-9\\s.,\\-_/()&:;'\"@#%+]*$"), //+% present
    REQ_TITLE_PATTERN("^[A-Za-z0-9\\s.,\\-_/()&:;'\"@#]*$"),
    ALPHA_NUMERIC_DASH("^[A-Za-z0-9-]+$"); // added it for offer letter number validation


    private final String regex;


    RegexPattern(String regex) {
        this.regex = regex;
    }

    public String getRegex() {
        return regex;
    }
}