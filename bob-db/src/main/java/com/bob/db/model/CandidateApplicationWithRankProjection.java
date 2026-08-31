package com.bob.db.model;

import java.util.UUID;

public interface CandidateApplicationWithRankProjection {

    UUID getId();

    UUID getCandidateId();

    UUID getPositionId();

    Integer getRankNumber();
}
