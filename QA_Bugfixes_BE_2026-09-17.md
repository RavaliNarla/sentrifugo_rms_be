# SCL RMS — Backend Bug Fixes (2026-09-17)

> Scope: `sentrifugo_rms_be` (`preview` branch)  
> Modules touched: `rms-db`, `master-portal`, `recruiter-portal`  
> Source: QA issues from `SCL_RMS_Issues` + FRS (`Recruitment_Module_Requirements v.2`)  
> Out of scope for this note: MCP/browser tooling, local env secrets, infra-only work

---

## Summary

Backend support for the QA/FRS pass: Position Title master (per-department + JD/experience defaults), Specialization master + seeder, candidate shortlist HOLD, Compensation CTC, Offer joining date / preview / L1–L2 approval workflow, interview scoring fields, job position validation, and requisition unfulfill (Undo Fulfilled).

Note: after adding `SpecializationRepository`, local runs must `mvn install` `rms-db` so master/recruiter pick up the new class on the classpath.

---

## New files

### `rms-db`

| File | Purpose |
|------|---------|
| `.../entity/SpecializationEntity.java` | Specialization master entity (`common` schema), optional link to education qualification |
| `.../repository/SpecializationRepository.java` | CRUD + by-education (including general/unlinked) query |

### `master-portal`

| File | Purpose |
|------|---------|
| `.../config/MasterDataSeeder.java` | Idempotent seed: departments, locations, approved-by roles, specializations, position-title↔department linking for known sample titles |
| `.../controller/SpecializationController.java` | Admin/list APIs for specializations |
| `.../service/SpecializationService.java` | Specialization business logic |
| `.../dto/SpecializationDTO.java` | Specialization DTO |
| `.../dto/PositionTitleDTO.java` | Position title DTO (department, JD, min experience) |

### `recruiter-portal` DTOs

| File | Purpose |
|------|---------|
| `dto/ShortlistDecisionRequest.java` | `SHORTLIST` / `REJECT` / `HOLD` |
| `dto/CompensationDetailsRequest.java` | CTC / compensation payload |
| `dto/OfferPreviewRequest.java` | Offer HTML preview request |
| `dto/OfferApprovalActionRequest.java` | L1/L2 approve/reject payload |

---

## Modified files (what changed)

### `rms-db` entities / enums / repositories

**`PositionTitleEntity.java`**
- `departmentId`, `jobDescription`, `minimumExperienceYears`
- Unique constraint on `(name, department_id)`
- Soft-delete / active filter retained

**`JobPositionEntity.java`**
- New/extended fields for specialization, certifications, medical fitness, employment type, contractual period, approved-by other text, etc. (aligned with Add Position FRS)

**`CandidateEntity.java`**
- CTC / compensation-related fields as needed for Compensation Management
- Status support for hold path

**`CandidateOfferEntity.java`**
- Joining date
- L1/L2 approval metadata / status fields

**`PanelMemberScoreEntity.java`**
- Decision field support: SELECT / REJECT / HOLD (plus score/comments)

**`CandidateStatus.java`**
- Added statuses used by shortlist hold flow (e.g. `ON_HOLD`)

**`OfferStatus.java`**
- Statuses for pending L1/L2 / approved / rejected offer approval workflow

**`PositionTitleRepository.java`**
- `findAllByDepartmentIdOrderByNameAsc`
- Existence check by name + department

**`JobPositionRepository.java`**
- Queries supporting delete/validation for NEW-only positions, etc.

**`CandidateOfferRepository.java`**
- Pending-approval style queries for Offer Approvals

### `master-portal`

**`PositionTitleController.java` / `PositionTitleService.java`**
- List all / by department
- Add / update / delete with duplicate name-per-department validation
- DTO mapping includes department name, JD, min experience

**`MasterDataSeeder.java`**
- Seeds FRS department / location / approved-by lists
- Seeds starter specializations (linked by education keyword where possible)
- Ensures sample position titles exist and **backfills `department_id` on orphan titles** so Add Position dropdowns are not empty

**`SpecializationController` / `SpecializationService` / `SpecializationDTO`**
- Full admin API + list-by-education for Add Position dropdown

### `recruiter-portal` controllers / services / DTOs

**`CandidateController.java` / `CandidateService.java` / `CandidateDTO.java` / `ShortlistDecisionRequest.java`**
- Endpoint to record shortlist decision: SHORTLIST / REJECT / HOLD
- Email/phone validation on create/update where applicable
- Status transitions including ON_HOLD

**`CompensationController.java` / `CompensationService.java` / `CompensationDetailsRequest.java`**
- Compensation Management per FRS (CTC fields)

**`OfferController.java` / `OfferService.java` / `GenerateOfferRequest.java` / `CandidateOfferDTO.java` / `OfferPreviewRequest.java` / `OfferApprovalActionRequest.java`**
- Joining date on generate offer
- Template HTML preview endpoint
- Submit for approval
- Pending approval list
- L1/L2 approve/reject actions
- Resend / guard rules as implemented in service

**`JobRequisitionController.java` / `JobRequisitionService.java`**
- `markFulfilled` / **`unmarkFulfilled`** (Undo Fulfilled)
- Submit for approval retained/extended

**`JobPositionService.java` / `JobPositionDTO.java`**
- Validation: master-required fields, duplicate position rules, date rules (`approvedOn` not future)
- Specialization name resolution
- Create/update/delete (NEW-only delete where applicable)

**`InterviewPoolController.java` / `InterviewPoolService.java` / `InterviewSchedulingService.java` / `InterviewScheduleDTO.java` / `PositionPanelService.java`**
- Assign-to-position date validation
- Interview email/location details
- Scheduling DTO extensions

**`ScoreSubmitRequest.java`**
- Decision: SELECT / REJECT / HOLD with score/comments

---

## Issue / FRS mapping (BE-relevant)

| ID / theme | BE change |
|------------|-----------|
| Position Master / SCL_07 | Titles by `departmentId`; JD + min experience on master |
| Specialization | Entity, repo, master APIs, seeder |
| SCL_25 shortlist | Decision API + `ON_HOLD` status |
| Compensation FRS | CTC request/service |
| Offer FRS | Joining date, preview, L1/L2 approval statuses/APIs |
| Undo Fulfilled | `unmarkFulfilled` on requisition |
| Interviewer scoring | Decision on panel member score |
| Add Position validation | JobPositionService field/date/master checks |

---

## Runtime / local notes

1. **Classpath:** After pulling, run `mvn -DskipTests install -pl rms-db -am` (or full install) before `spring-boot:run` on master/recruiter so `SpecializationRepository` is available.
2. **Ports (local default):** auth `:8085`, master `:8080`, recruiter `:8086`.
3. **DB:** `ddl-auto=update` creates/updates tables (e.g. `common.specializations`, position title columns). Seeder fills masters idempotently.
4. **QA data fix (DB, not necessarily in git):** Orphan position titles in `rms_dev` were updated to set `department_id` so by-department listing works immediately; seeder also covers linking known title names going forward.

---

## How to verify (BE)

1. `GET .../position-titles/by-department/{departmentId}` returns titles for that dept.
2. Specialization CRUD + by-education endpoints.
3. Candidate shortlist decision → status SHORTLISTED / REJECTED / ON_HOLD.
4. Offer preview + generate with joining date; submit-for-approval; L1/L2 action endpoints.
5. Requisition unfulfill endpoint restores from FULFILLED.
6. Position create rejects invalid/future approvedOn and missing master-required fields.

---

## Commit / branch

- Branch: `preview`
- Commit message (this push): QA/FRS backend fixes — position master, specializations, offers L1/L2, shortlist hold, unfulfill
