package com.bob.jobportal.model;

import com.bob.db.dto.JobRequisitionsDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JobRequisitionWithDraftResponseModel extends JobRequisitionsDTO {
    @JsonProperty("is_in_edit_mode")
    private Boolean isInEditMode;

    private RequisitionEditDraftResponseModel draft;
}
