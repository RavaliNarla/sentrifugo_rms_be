package com.bob.jobportal.model;

import com.bob.jobportal.util.PwdTracker;
import com.bob.jobportal.util.VacancyTracker;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VacancyMatrixModel {
    VacancyTracker vacancyTracker;
    PwdTracker pwdTracker;
}
