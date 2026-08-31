package com.bob.candidateportal.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ExamCenterModel {
  private UUID applicationId;
  private UUID examCenterId;
  private UUID candidateId;
}
