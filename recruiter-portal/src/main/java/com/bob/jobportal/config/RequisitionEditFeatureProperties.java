package com.bob.jobportal.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Feature flags for the Edit-Requisition flow. Allows operations team to disable
 * the entire feature or only the developer-only direct-approval path during staged rollout.
 *
 * Defaults are safe for non-prod (feature on, direct-mode on). Production should set
 * {@code recruiter.requisition-edit.direct-mode-enabled=false} so only the L1/L2 workflow path is callable.
 */
@Component
@Getter
public class RequisitionEditFeatureProperties {

    @Value("${recruiter.requisition-edit.enabled:true}")
    private boolean enabled;

    @Value("${recruiter.requisition-edit.direct-mode-enabled:true}")
    private boolean directModeEnabled;
}
