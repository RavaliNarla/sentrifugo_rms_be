package com.bob.jobportal.model;

import com.bob.db.entity.CandidateSectionMarksEntity;
import com.bob.db.entity.CandidateWrittenExamMarksEntity;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ExaminationConfigurationMarksUploadExcelModel {
    private String searchKey;
   private CandidateWrittenExamMarksEntity writtenExamMarksEntity;
   private List<CandidateSectionMarksEntity> candidateSectionMarksEntityList;

}
