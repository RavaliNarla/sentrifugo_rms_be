package com.sentrifugo.rms.recruiterportal.email;

import com.sentrifugo.rms.common.service.MailService;
import com.sentrifugo.rms.common.service.PdfConverterService;
import com.sentrifugo.rms.common.util.IstTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SCL-branded HTML email bodies (and matching PDF copies) for RMS notification scenarios.
 * Wording follows client templates under SCLEmail notification / Offer Letter revised.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RmsEmailTemplates {

    private final MailService mailService;
    private final PdfConverterService pdfConverterService;

    @Value("${app.company.name:Sagar Cements Limited}")
    private String companyName;

    public record BuiltEmail(String subject, String html) {}

    public void sendAsync(String to, BuiltEmail email) {
        sendAsync(to, email, false, null);
    }

    public void sendAsync(String to, BuiltEmail email, boolean attachPdf, String pdfFileName) {
        if (to == null || to.isBlank() || email == null) {
            return;
        }
        try {
            List<MailService.Attachment> attachments = new ArrayList<>();
            if (attachPdf) {
                try {
                    byte[] pdf = pdfConverterService.convertHtmlStringToPdf(wrapPrintable(email.html()));
                    attachments.add(new MailService.Attachment(
                            pdfFileName != null ? pdfFileName : "notification.pdf", pdf, "application/pdf"));
                } catch (Exception e) {
                    log.warn("PDF attach failed for {}: {}", email.subject(), e.getMessage());
                }
            }
            if (attachments.isEmpty()) {
                mailService.sendHtmlEmailAsync(to, email.subject(), email.html());
            } else {
                mailService.sendHtmlEmailAsync(to, email.subject(), email.html(), attachments);
            }
        } catch (Exception e) {
            log.warn("Failed to queue email '{}' to {}: {}", email.subject(), to, e.getMessage());
        }
    }

    public void sendSyncWithAttachments(String to, BuiltEmail email, List<MailService.Attachment> extra) {
        if (to == null || to.isBlank() || email == null) {
            return;
        }
        List<MailService.Attachment> all = new ArrayList<>();
        if (extra != null) {
            all.addAll(extra);
        }
        mailService.sendHtmlEmail(to, email.subject(), email.html(), all.isEmpty() ? null : all);
    }

    public void sendAsyncWithAttachments(String to, BuiltEmail email, List<MailService.Attachment> extra) {
        if (to == null || to.isBlank() || email == null) {
            return;
        }
        List<MailService.Attachment> all = new ArrayList<>();
        if (extra != null) {
            all.addAll(extra);
        }
        mailService.sendHtmlEmailAsync(to, email.subject(), email.html(), all.isEmpty() ? null : all);
    }

    // ── Candidate shortlist ───────────────────────────────────────────────

    public BuiltEmail shortlisted(String candidateName, String positionName, String locationName) {
        String subject = "You have been shortlisted – " + nz(positionName) + " | " + companyName;
        String body = p("Dear " + bold(esc(candidateName)) + ",")
                + p("Thank you for your interest in " + bold(esc(companyName))
                + " and for taking the time to apply for the " + bold(esc(positionName)) + " position.")
                + candidateRoleDetails(positionName, locationName)
                + p("We are pleased to inform you that you have been " + bold("shortlisted")
                + " for the next stage of our selection process. Our team will contact you with further details shortly.")
                + signOff();
        return new BuiltEmail(subject, frame(subject, body));
    }

    public BuiltEmail onHold(String candidateName, String positionName, String locationName) {
        String subject = "Update on Your Application – " + nz(positionName) + " | " + companyName;
        String body = p("Dear " + bold(esc(candidateName)) + ",")
                + p("Thank you for your interest in " + bold(esc(companyName))
                + " and for taking the time to apply for the " + bold(esc(positionName)) + " position.")
                + candidateRoleDetails(positionName, locationName)
                + p("Your application is currently " + bold("on hold")
                + ". We will update you as soon as there is further progress.")
                + signOff();
        return new BuiltEmail(subject, frame(subject, body));
    }

    public BuiltEmail shortlistRejected(String candidateName, String positionName, String locationName) {
        String subject = "Update on Your Application – " + nz(positionName) + " | " + companyName;
        String body = p("Dear " + bold(esc(candidateName)) + ",")
                + p("Thank you for your interest in " + bold(esc(companyName))
                + " and for taking the time to apply for the " + bold(esc(positionName)) + " position.")
                + candidateRoleDetails(positionName, locationName)
                + p("After reviewing your application, we regret to inform you that we will not be progressing your application to the next stage of the selection process at this time.")
                + p("We appreciate your interest in joining " + bold(esc(companyName))
                + ". We will retain your resume in our talent database, and if a suitable opportunity arises in the future that matches your experience and profile, we will be happy to get in touch with you.")
                + p("We wish you the very best in your future career endeavours.")
                + signOff();
        return new BuiltEmail(subject, frame(subject, body));
    }

    // ── Interview invite (candidate) ──────────────────────────────────────

    public BuiltEmail interviewInvite(String candidateName, String positionName, int round, String roundName,
                                      LocalDate date, LocalTime start, LocalTime end, String location,
                                      String panelName, String acceptUrl, String declineUrl) {
        String subject = "Interview Scheduled – " + nz(positionName) + " | " + companyName;
        String roundLabel = "Round " + round + (roundName != null && !roundName.isBlank() ? " (" + roundName + ")" : "");
        String timeLabel = IstTime.formatTime(start) + " – " + IstTime.formatTime(end);
        String body = p("Dear " + bold(esc(candidateName)) + ",")
                + p("Your interview has been scheduled with " + bold(esc(companyName))
                + " for the position of " + bold(esc(positionName)) + ".")
                + "<p style='margin:16px 0 6px;'><b><u>Interview Details</u></b></p>"
                + "<ul style='margin:0 0 16px;padding-left:18px;line-height:1.6;'>"
                + "<li><b>Round:</b> " + esc(roundLabel) + "</li>"
                + "<li><b>Date:</b> " + IstTime.formatDate(date) + "</li>"
                + "<li><b>Time:</b> " + esc(timeLabel) + "</li>"
                + "<li><b>Interview Location:</b> " + esc(nz(location)) + "</li>"
                + "<li><b>Interviewer:</b> " + esc(nz(panelName)) + "</li>"
                + "</ul>"
                + p("Please be available at the scheduled date and time.")
                + p("You may also add this interview to your calendar using the calendar invitation included with this email.")
                + calendarNote()
                + actionButtons(acceptUrl, "Accept Interview", "#1b7a3d", declineUrl, "Decline Interview", "#a20e37")
                + signOff();
        return new BuiltEmail(subject, frame(subject, body));
    }

    // ── Qualify / Disqualify ──────────────────────────────────────────────

    public BuiltEmail interviewOutcome(String candidateName, String positionName, String locationName,
                                       int round, String roundName,
                                       boolean qualified, LocalDate interviewDate) {
        String outcome = qualified ? "Qualified" : "Disqualified";
        String subject = "Interview Result – Round " + round
                + (roundName != null && !roundName.isBlank() ? " (" + roundName + ")" : "")
                + " | " + nz(positionName);
        String body = p("Dear " + bold(esc(candidateName)) + ",")
                + p("Thank you for attending the interview with " + bold(esc(companyName))
                + " for the position of " + bold(esc(positionName)) + ".")
                + candidateRoleDetails(positionName, locationName)
                + "<p style='margin:16px 0 6px;'><b><u>Result Details</u></b></p>"
                + "<ul style='margin:0 0 16px;padding-left:18px;line-height:1.6;'>"
                + "<li><b>Round:</b> Round " + round
                + (roundName != null && !roundName.isBlank() ? " (" + esc(roundName) + ")" : "") + "</li>"
                + (interviewDate != null ? "<li><b>Interview Date:</b> " + IstTime.formatDate(interviewDate) + "</li>" : "")
                + "<li><b>Outcome:</b> " + bold(outcome) + "</li>"
                + "</ul>"
                + (qualified
                ? p("We are pleased to inform you that you have " + bold("qualified") + " for the next stage of our process. Our team will be in touch with further details.")
                : p("After careful evaluation, we will not be progressing your application further at this time. We appreciate your interest and wish you the very best in your future career endeavours."))
                + signOff();
        return new BuiltEmail(subject, frame(subject, body));
    }

    // ── Offer to candidate ────────────────────────────────────────────────

    public BuiltEmail offerLetter(String candidateName, String designation, String location,
                                  LocalDate joiningDate, LocalDate validUntil,
                                  String acceptUrl, String declineUrl, String hrContact) {
        String subject = "Offer of Employment | " + companyName;
        String body = p("Dear " + bold(esc(candidateName)) + ",")
                + p("We are delighted to offer you the opportunity to join " + bold(esc(companyName))
                + " as " + bold(esc(designation)) + " at " + bold(esc(nz(location))) + ".")
                + p("Your " + bold("Offer Letter") + " is attached for your review and includes the details of your proposed employment and applicable terms and conditions.")
                + "<p style='margin:16px 0 6px;'><b><u>Offer Details</u></b></p>"
                + "<ul style='margin:0 0 16px;padding-left:18px;line-height:1.6;'>"
                + "<li><b>Position:</b> " + esc(nz(designation)) + "</li>"
                + "<li><b>Location:</b> " + esc(nz(location)) + "</li>"
                + "<li><b>Proposed Joining Date:</b> " + IstTime.formatDate(joiningDate) + "</li>"
                + "<li><b>Offer Valid Until:</b> " + IstTime.formatDate(validUntil) + "</li>"
                + "</ul>"
                + p("Please review the Offer Letter and confirm your decision within " + bold("3 working days") + " of receiving this email.")
                + actionButtons(acceptUrl, "Accept Offer", "#1b7a3d", declineUrl, "Decline Offer", "#a20e37")
                + (hrContact != null && !hrContact.isBlank()
                ? p("If you have any questions or require clarification, please contact " + bold(esc(hrContact)) + ".")
                : "")
                + p("We look forward to having you join the " + bold("Sagar Cements family")
                + " and wish you a rewarding journey with us.")
                + warmSignOff();
        return new BuiltEmail(subject, frame(subject, body));
    }

    public BuiltEmail offerAccepted(String candidateName, String designation, String location, LocalDate joiningDate) {
        String subject = "Offer Acceptance Confirmed | " + companyName;
        String body = p("Dear " + bold(esc(candidateName)) + ",")
                + p("Thank you for accepting our offer to join " + bold(esc(companyName)) + ".")
                + p("We are pleased to have you join us as " + bold(esc(nz(designation)))
                + " at " + bold(esc(nz(location)))
                + ". Your acceptance has been successfully recorded, and our team will be in touch with you regarding the next steps and joining formalities.")
                + "<p style='margin:16px 0 6px;'><b>Offer Details</b></p>"
                + "<ul style='margin:0 0 16px;padding-left:18px;line-height:1.6;'>"
                + "<li><b>Position:</b> " + esc(nz(designation)) + "</li>"
                + "<li><b>Location:</b> " + esc(nz(location)) + "</li>"
                + "<li><b>Joining Date:</b> " + IstTime.formatDate(joiningDate) + "</li>"
                + "</ul>"
                + p("We are pleased to welcome you to the " + bold("Sagar Cements family")
                + " and look forward to your journey with the organization.")
                + warmSignOff();
        return new BuiltEmail(subject, frame(subject, body));
    }

    public BuiltEmail offerDeclined(String candidateName, String designation, String location) {
        String subject = "Offer Decision Acknowledgement | " + companyName;
        String body = p("Dear " + bold(esc(candidateName)) + ",")
                + p("Thank you for taking the time to consider the opportunity with " + bold(esc(companyName)) + ".")
                + candidateRoleDetails(designation, location)
                + p("We acknowledge your decision to decline our offer for the position of "
                + bold(esc(nz(designation))) + " and appreciate your interest in joining us.")
                + p(bold("Wish you the very best in your future career endeavours."))
                + warmSignOff();
        return new BuiltEmail(subject, frame(subject, body));
    }

    // ── Internal: recruiter / approver / interviewer ──────────────────────

    /** Context block for hiring-team emails (candidate + req + pos + location). */
    public record HiringDetails(String candidateName, String requisitionCode, String positionName, String locationName) {
        public static HiringDetails of(String candidateName, String requisitionCode, String positionName, String locationName) {
            return new HiringDetails(candidateName, requisitionCode, positionName, locationName);
        }
    }

    public BuiltEmail recruiterInviteResponse(String recruiterName, String candidateName, String positionName,
                                              String requisitionCode, String locationName,
                                              int round, String roundName, LocalDate interviewDate,
                                              LocalTime start, LocalTime end, boolean accepted) {
        String verb = accepted ? "accepted" : "declined";
        String when = interviewDate == null ? ""
                : " scheduled on " + bold(esc(IstTime.formatDate(interviewDate)))
                + (start != null && end != null
                        ? ", " + bold(esc(IstTime.formatTime(start) + " – " + IstTime.formatTime(end))) : "");
        String subject = "Candidate " + verb + " interview invite – " + nz(candidateName);
        String body = p("Hi " + esc(nz(recruiterName)) + ",")
                + p(bold(esc(candidateName)) + " has " + bold(verb) + " the interview invitation"
                + " (Round " + round
                + (roundName != null && !roundName.isBlank() ? " – " + esc(roundName) : "") + ")" + when + ".")
                + hiringTeamDetails(HiringDetails.of(candidateName, requisitionCode, positionName, locationName))
                + p("Please log in to Sagar Recruitment Hub to review the updated status.")
                + signOff();
        return new BuiltEmail(subject, frame(subject, body));
    }

    public BuiltEmail recruiterOfferResponse(String recruiterName, String candidateName, String designation,
                                             String requisitionCode, String locationName, boolean accepted) {
        String verb = accepted ? "accepted" : "declined";
        String subject = "Candidate " + verb + " offer – " + nz(candidateName);
        String body = p("Hi " + esc(nz(recruiterName)) + ",")
                + p(bold(esc(candidateName)) + " has " + bold(verb) + " the offer.")
                + hiringTeamDetails(HiringDetails.of(candidateName, requisitionCode, designation, locationName))
                + p("Please log in to Sagar Recruitment Hub to review the updated status.")
                + signOff();
        return new BuiltEmail(subject, frame(subject, body));
    }

    public BuiltEmail approverSubmission(String approverName, String itemType, String level, HiringDetails details) {
        String subjectLabel = details != null && notBlank(details.candidateName())
                ? details.candidateName()
                : (details != null && notBlank(details.requisitionCode()) ? details.requisitionCode() : itemType);
        String subject = itemType + " submitted for " + level + " approval – " + nz(subjectLabel);
        String body = p("Hi " + esc(nz(approverName)) + ",")
                + p("A " + esc(itemType).toLowerCase() + " has been submitted for your " + bold(esc(level)) + " approval.")
                + hiringTeamDetails(details)
                + p("Please log in to Sagar Recruitment Hub to review and take action.")
                + signOff();
        return new BuiltEmail(subject, frame(subject, body));
    }

    public BuiltEmail approvalDecisionNotice(String recipientName, String itemType, String decidedByLevel,
                                             boolean approved, String nextHint, HiringDetails details) {
        String verb = approved ? "approved" : "rejected";
        String subjectLabel = details != null && notBlank(details.candidateName())
                ? details.candidateName()
                : (details != null && notBlank(details.requisitionCode()) ? details.requisitionCode() : itemType);
        String subject = itemType + " " + verb + " by " + decidedByLevel + " – " + nz(subjectLabel);
        String body = p("Hi " + esc(nz(recipientName)) + ",")
                + p("The following " + esc(itemType).toLowerCase() + " was " + bold(verb) + " by "
                + bold(esc(decidedByLevel)) + ".")
                + hiringTeamDetails(details)
                + (nextHint != null && !nextHint.isBlank() ? p(esc(nextHint)) : "")
                + p("Please log in to Sagar Recruitment Hub for details.")
                + signOff();
        return new BuiltEmail(subject, frame(subject, body));
    }

    public BuiltEmail panelDaySchedule(String memberName, String dayLabel, int dayCount, List<String> interviewLines) {
        String subject = "Interview scheduled — " + dayLabel;
        StringBuilder list = new StringBuilder();
        if (interviewLines != null && !interviewLines.isEmpty()) {
            list.append("<p style='margin:16px 0 6px;'><b><u>Interviews</u></b></p>")
                    .append("<ul style='margin:0 0 16px;padding-left:18px;line-height:1.6;'>");
            for (String line : interviewLines) {
                if (line != null && !line.isBlank()) {
                    list.append("<li>").append(esc(line)).append("</li>");
                }
            }
            list.append("</ul>");
        }
        String body = p("Hi " + esc(nz(memberName)) + ",")
                + p("You have " + bold(String.valueOf(dayCount)) + " interview"
                + (dayCount == 1 ? "" : "s") + " on " + bold(esc(dayLabel)) + ".")
                + list
                + calendarNote()
                + p("Please log in to Sagar Recruitment Hub for full details. Candidates appear on your interview list only after they " + bold("Accept") + " the invite.")
                + signOff();
        return new BuiltEmail(subject, frame(subject, body));
    }

    // ── HTML helpers ──────────────────────────────────────────────────────

    private String candidateRoleDetails(String positionName, String locationName) {
        if (!notBlank(positionName) && !notBlank(locationName)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<p style='margin:16px 0 6px;'><b><u>Role Details</u></b></p>")
                .append("<ul style='margin:0 0 16px;padding-left:18px;line-height:1.6;'>");
        if (notBlank(positionName)) {
            sb.append("<li><b>Position:</b> ").append(esc(positionName)).append("</li>");
        }
        if (notBlank(locationName)) {
            sb.append("<li><b>Location:</b> ").append(esc(locationName)).append("</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private String hiringTeamDetails(HiringDetails details) {
        if (details == null) {
            return "";
        }
        boolean any = notBlank(details.candidateName()) || notBlank(details.requisitionCode())
                || notBlank(details.positionName()) || notBlank(details.locationName());
        if (!any) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<p style='margin:16px 0 6px;'><b><u>Details</u></b></p>")
                .append("<ul style='margin:0 0 16px;padding-left:18px;line-height:1.6;'>");
        if (notBlank(details.candidateName())) {
            sb.append("<li><b>Candidate:</b> ").append(esc(details.candidateName())).append("</li>");
        }
        if (notBlank(details.requisitionCode())) {
            sb.append("<li><b>Requisition:</b> ").append(esc(details.requisitionCode())).append("</li>");
        }
        if (notBlank(details.positionName())) {
            sb.append("<li><b>Position:</b> ").append(esc(details.positionName())).append("</li>");
        }
        if (notBlank(details.locationName())) {
            sb.append("<li><b>Location:</b> ").append(esc(details.locationName())).append("</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String frame(String title, String bodyHtml) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/></head>"
                + "<body style='margin:0;padding:0;background:#f4f6f5;font-family:Segoe UI,Arial,sans-serif;color:#222;'>"
                + "<div style='max-width:640px;margin:24px auto;background:#fff;border:1px solid #dde3df;border-radius:8px;overflow:hidden;'>"
                + "<div style='background:linear-gradient(90deg,#14532d,#1b7a3d);color:#fff;padding:16px 22px;'>"
                + "<div style='font-size:18px;font-weight:700;letter-spacing:0.3px;'>" + esc(companyName) + "</div>"
                + "<div style='font-size:12px;opacity:0.9;margin-top:2px;'>Recruitment Notification</div>"
                + "</div>"
                + "<div style='padding:22px 24px;font-size:14px;line-height:1.55;'>"
                + bodyHtml
                + "</div>"
                + "<div style='background:#f7faf8;border-top:1px solid #e5ebe7;padding:12px 24px;font-size:11px;color:#6b7280;'>"
                + "This is an automated message from Sagar Recruitment Hub. Please do not reply to this email."
                + "</div></div></body></html>";
    }

    private String wrapPrintable(String html) {
        // Already a full HTML document from frame().
        return html;
    }

    private String signOff() {
        return "<p style='margin-top:22px;'>Regards,<br/><b>Human Resources</b><br/><b>"
                + esc(companyName) + "</b></p>";
    }

    private String warmSignOff() {
        return "<p style='margin-top:22px;'>Warm regards,<br/><b>Human Resources</b><br/><b>"
                + esc(companyName) + "</b></p>";
    }

    private String calendarNote() {
        return "<div style='background:#fff8e6;border:2px solid #e6a800;border-radius:6px;padding:12px 14px;margin:16px 0;'>"
                + "<p style='margin:0;font-size:13px;color:#5c4500;line-height:1.45;'>"
                + "The attached <b>.ics</b> calendar file is a reminder only. "
                + "Opening or adding it does <b>not</b> confirm your response. "
                + "Use the Accept / Decline buttons in this email to record your decision in our system."
                + "</p></div>";
    }

    private String actionButtons(String primaryUrl, String primaryLabel, String primaryColor,
                                 String secondaryUrl, String secondaryLabel, String secondaryColor) {
        return "<p style='margin-top:20px;'>"
                + "<a href='" + esc(primaryUrl) + "' style='background:" + primaryColor
                + ";color:#fff;padding:12px 26px;text-decoration:none;border-radius:4px;margin-right:10px;"
                + "display:inline-block;font-weight:bold;'>" + esc(primaryLabel) + "</a>"
                + "<a href='" + esc(secondaryUrl) + "' style='background:" + secondaryColor
                + ";color:#fff;padding:12px 26px;text-decoration:none;border-radius:4px;"
                + "display:inline-block;font-weight:bold;'>" + esc(secondaryLabel) + "</a>"
                + "</p>";
    }

    private static String p(String inner) {
        return "<p style='margin:0 0 12px;'>" + inner + "</p>";
    }

    private static String bold(String inner) {
        return "<b>" + inner + "</b>";
    }

    private static String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String nz(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
