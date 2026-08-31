package com.bob.jobportal.model;

import com.bob.db.dto.OfferApprovalHistoryDTO;
import com.bob.db.dto.OfferApprovalsDTO;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OfferApprovalHistoryModel {
//    private OfferApprovalHistoryDTO offerApprovalHistory;
    private OfferApprovalsDTO offerApproval;
    private String fullName;
    private BigDecimal combinedScore;
    private String state;
    private String city;
    private String letterNumber;
}
