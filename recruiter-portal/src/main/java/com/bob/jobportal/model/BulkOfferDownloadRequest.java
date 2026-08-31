package com.bob.jobportal.model;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulkOfferDownloadRequest {
    private List<UUID> offerIds;
}
