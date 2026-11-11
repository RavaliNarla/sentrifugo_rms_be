package com.sentrifugo.rms.masterportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sentrifugo.rms.common.util.MasterCodeUtil;
import com.sentrifugo.rms.db.entity.DepartmentEntity;
import com.sentrifugo.rms.db.entity.LocationEntity;
import com.sentrifugo.rms.db.entity.PositionTitleEntity;
import com.sentrifugo.rms.db.repository.DepartmentRepository;
import com.sentrifugo.rms.db.repository.LocationRepository;
import com.sentrifugo.rms.db.repository.PositionTitleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Upserts RMS copy masters from HRMS IAM lists. Preserves RMS-only fields on position titles.
 *
 * <p>Designed for Hikari pool size 2: IAM HTTP is outside DB transactions; only one sync runs
 * at a time; each row is persisted in its own short transaction so audit {@code REQUIRES_NEW}
 * can use the second connection without pool starvation.
 */
@Slf4j
@Service
public class MasterSyncService {

    private static final long SYNC_COOLDOWN_MS = 60_000L;
    /** Serializes all master syncs so pool=2 is never shared across concurrent syncs. */
    private static final Object GLOBAL_SYNC_LOCK = new Object();

    private final HrmsIamClient hrmsIamClient;
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final PositionTitleRepository positionTitleRepository;
    private final TransactionTemplate tx;

    private final ConcurrentHashMap<String, Long> lastSyncAt = new ConcurrentHashMap<>();

    public MasterSyncService(
            HrmsIamClient hrmsIamClient,
            DepartmentRepository departmentRepository,
            LocationRepository locationRepository,
            PositionTitleRepository positionTitleRepository,
            PlatformTransactionManager transactionManager) {
        this.hrmsIamClient = hrmsIamClient;
        this.departmentRepository = departmentRepository;
        this.locationRepository = locationRepository;
        this.positionTitleRepository = positionTitleRepository;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public void syncDepartments(String hrmsToken) {
        if (blank(hrmsToken)) {
            return;
        }
        runSynced("departments", () -> {
            List<JsonNode> remote;
            try {
                remote = hrmsIamClient.listDepartments(hrmsToken);
            } catch (Exception ex) {
                log.warn("Department sync skipped: {}", ex.getMessage());
                return;
            }
            persistDepartments(remote);
        });
    }

    public void syncLocations(String hrmsToken) {
        if (blank(hrmsToken)) {
            return;
        }
        runSynced("locations", () -> {
            List<JsonNode> remote;
            try {
                remote = hrmsIamClient.listBusinessUnits(hrmsToken);
            } catch (Exception ex) {
                log.warn("Location (BU) sync skipped: {}", ex.getMessage());
                return;
            }
            persistLocations(remote);
        });
    }

    public void syncPositionTitles(String hrmsToken) {
        if (blank(hrmsToken)) {
            return;
        }
        runSynced("positionTitles", () -> {
            List<JsonNode> remote;
            try {
                remote = hrmsIamClient.listDesignations(hrmsToken);
            } catch (Exception ex) {
                log.warn("Position title (designation) sync skipped: {}", ex.getMessage());
                return;
            }
            persistPositionTitles(remote);
        });
    }

    private void runSynced(String key, Runnable work) {
        synchronized (GLOBAL_SYNC_LOCK) {
            Long last = lastSyncAt.get(key);
            long now = System.currentTimeMillis();
            if (last != null && now - last < SYNC_COOLDOWN_MS) {
                return;
            }
            work.run();
            lastSyncAt.put(key, System.currentTimeMillis());
        }
    }

    private void persistDepartments(List<JsonNode> remote) {
        Set<String> usedCodes = tx.execute(status -> {
            Set<String> used = loadUsedCodes(departmentRepository.findAllWithHrmsIdIncludingInactive().stream()
                    .map(DepartmentEntity::getCode).toList());
            departmentRepository.findAll().forEach(d -> {
                if (d.getCode() != null) {
                    used.add(d.getCode().trim().toUpperCase(Locale.ROOT));
                }
            });
            return used;
        });
        if (usedCodes == null) {
            usedCodes = new HashSet<>();
        }

        Set<String> seen = new HashSet<>();
        for (JsonNode row : remote) {
            String hrmsId = text(row, "id");
            if (blank(hrmsId) || !isActiveFlag(row)) {
                continue;
            }
            String name = firstNonBlank(text(row, "departmentName", "department_name", "name"));
            if (blank(name)) {
                continue;
            }
            seen.add(hrmsId);
            String description = text(row, "description");
            String buIds = joinBusinessUnitIds(row);
            String preferred = text(row, "departmentCode", "department_code");
            Set<String> codesRef = usedCodes;
            tx.executeWithoutResult(status -> upsertDepartment(hrmsId, name, description, buIds, preferred, codesRef));
        }

        Set<String> seenFinal = seen;
        tx.executeWithoutResult(status -> softDeactivateMissing(
                departmentRepository.findAllWithHrmsIdIncludingInactive(),
                seenFinal,
                e -> {
                    if (Boolean.TRUE.equals(e.getIsActive())) {
                        e.setIsActive(false);
                        departmentRepository.save(e);
                    }
                },
                DepartmentEntity::getHrmsId));
    }

    private void upsertDepartment(
            String hrmsId,
            String name,
            String description,
            String buIds,
            String preferred,
            Set<String> usedCodes) {
        DepartmentEntity entity = departmentRepository.findIncludingInactiveByHrmsId(hrmsId).orElse(null);
        if (entity != null && entity.getCode() != null) {
            usedCodes.remove(entity.getCode().trim().toUpperCase(Locale.ROOT));
        }
        String code = resolveCodeAsIs(preferred, name, entity != null ? entity.getCode() : null, usedCodes);
        if (entity == null) {
            entity = DepartmentEntity.builder()
                    .hrmsId(hrmsId)
                    .name(name.trim())
                    .code(code)
                    .description(trimToNull(description))
                    .hrmsBusinessUnitIds(buIds)
                    .isActive(true)
                    .build();
        } else {
            entity.setIsActive(true);
            entity.setName(name.trim());
            entity.setCode(code);
            entity.setDescription(trimToNull(description));
            entity.setHrmsBusinessUnitIds(buIds);
        }
        departmentRepository.save(entity);
    }

    private void persistLocations(List<JsonNode> remote) {
        Set<String> usedCodes = tx.execute(status -> {
            Set<String> used = loadUsedCodes(locationRepository.findAllWithHrmsIdIncludingInactive().stream()
                    .map(LocationEntity::getCode).toList());
            locationRepository.findAll().forEach(l -> {
                if (l.getCode() != null) {
                    used.add(l.getCode().trim().toUpperCase(Locale.ROOT));
                }
            });
            return used;
        });
        if (usedCodes == null) {
            usedCodes = new HashSet<>();
        }

        Set<String> seen = new HashSet<>();
        for (JsonNode row : remote) {
            String hrmsId = text(row, "id");
            if (blank(hrmsId) || !isActiveFlag(row)) {
                continue;
            }
            String name = firstNonBlank(text(row, "business_unit_name", "businessUnitName", "name"));
            if (blank(name)) {
                continue;
            }
            seen.add(hrmsId);
            String address = formatAddress(row.get("address"));
            if (blank(address)) {
                address = text(row, "address");
            }
            String preferred = text(row, "emp_code_prefix", "empCodePrefix");
            String addressFinal = address;
            Set<String> codesRef = usedCodes;
            tx.executeWithoutResult(status -> upsertLocation(hrmsId, name, addressFinal, preferred, codesRef));
        }

        Set<String> seenFinal = seen;
        tx.executeWithoutResult(status -> softDeactivateMissing(
                locationRepository.findAllWithHrmsIdIncludingInactive(),
                seenFinal,
                e -> {
                    if (Boolean.TRUE.equals(e.getIsActive())) {
                        e.setIsActive(false);
                        locationRepository.save(e);
                    }
                },
                LocationEntity::getHrmsId));
    }

    private void upsertLocation(
            String hrmsId,
            String name,
            String address,
            String preferred,
            Set<String> usedCodes) {
        LocationEntity entity = locationRepository.findIncludingInactiveByHrmsId(hrmsId).orElse(null);
        if (entity != null && entity.getCode() != null) {
            usedCodes.remove(entity.getCode().trim().toUpperCase(Locale.ROOT));
        }
        String code = resolveCodeAsIs(preferred, name, entity != null ? entity.getCode() : null, usedCodes);
        if (entity == null) {
            entity = LocationEntity.builder()
                    .hrmsId(hrmsId)
                    .name(name.trim())
                    .code(code)
                    .stateId(null)
                    .address(trimToNull(address))
                    .isActive(true)
                    .build();
        } else {
            entity.setIsActive(true);
            entity.setName(name.trim());
            entity.setCode(code);
            entity.setAddress(trimToNull(address));
        }
        locationRepository.save(entity);
    }

    private void persistPositionTitles(List<JsonNode> remote) {
        Set<String> seen = new HashSet<>();
        for (JsonNode row : remote) {
            String hrmsId = text(row, "id");
            if (blank(hrmsId) || !isActiveFlag(row)) {
                continue;
            }
            String name = firstNonBlank(text(row, "designationName", "designation_name", "name"));
            if (blank(name)) {
                continue;
            }
            seen.add(hrmsId);
            String nameFinal = name;
            tx.executeWithoutResult(status -> upsertPositionTitle(hrmsId, nameFinal));
        }

        Set<String> seenFinal = seen;
        tx.executeWithoutResult(status -> softDeactivateMissing(
                positionTitleRepository.findAllWithHrmsIdIncludingInactive(),
                seenFinal,
                e -> {
                    if (Boolean.TRUE.equals(e.getIsActive())) {
                        e.setIsActive(false);
                        positionTitleRepository.save(e);
                    }
                },
                PositionTitleEntity::getHrmsId));
    }

    private void upsertPositionTitle(String hrmsId, String name) {
        PositionTitleEntity entity = positionTitleRepository.findIncludingInactiveByHrmsId(hrmsId).orElse(null);
        if (entity == null) {
            entity = PositionTitleEntity.builder()
                    .hrmsId(hrmsId)
                    .name(name.trim())
                    .departmentId(null)
                    .jobDescription(null)
                    .minimumExperienceYears(null)
                    .isActive(true)
                    .build();
        } else {
            entity.setIsActive(true);
            entity.setName(name.trim());
        }
        positionTitleRepository.save(entity);
    }

    /** Prefer HRMS code as-is (no 3-char truncation). Fall back to existing or name-derived code. */
    private static String resolveCodeAsIs(String preferred, String name, String existing, Set<String> used) {
        if (!blank(preferred)) {
            String code = preferred.trim();
            if (code.length() > MasterCodeUtil.MAX_CODE_LENGTH) {
                code = code.substring(0, MasterCodeUtil.MAX_CODE_LENGTH);
            }
            used.add(code.toUpperCase(Locale.ROOT));
            return code;
        }
        if (!blank(existing)) {
            used.add(existing.trim().toUpperCase(Locale.ROOT));
            return existing.trim();
        }
        String generated = MasterCodeUtil.suggestFromName(name, used);
        used.add(generated.toUpperCase(Locale.ROOT));
        return generated;
    }

    private static String joinBusinessUnitIds(JsonNode row) {
        if (row == null) {
            return null;
        }
        JsonNode arr = row.get("businessUnits");
        if (arr == null || arr.isNull()) {
            arr = row.get("business_units");
        }
        if (arr == null || !arr.isArray() || arr.isEmpty()) {
            return null;
        }
        List<String> ids = new ArrayList<>();
        for (JsonNode n : arr) {
            if (n == null || n.isNull()) {
                continue;
            }
            if (n.isTextual() || n.isNumber()) {
                String id = n.asText();
                if (!blank(id)) {
                    ids.add(id.trim());
                }
            } else if (n.isObject()) {
                String id = text(n, "id", "_id");
                if (!blank(id)) {
                    ids.add(id.trim());
                }
            }
        }
        return ids.isEmpty() ? null : String.join(",", ids);
    }

    private static Set<String> loadUsedCodes(List<String> codes) {
        Set<String> used = new HashSet<>();
        for (String c : codes) {
            if (c != null && !c.isBlank()) {
                used.add(c.trim().toUpperCase(Locale.ROOT));
            }
        }
        return used;
    }

    private static <T> void softDeactivateMissing(
            List<T> existing,
            Set<String> seen,
            java.util.function.Consumer<T> deactivate,
            java.util.function.Function<T, String> hrmsIdFn) {
        for (T row : existing) {
            String id = hrmsIdFn.apply(row);
            if (id != null && !seen.contains(id)) {
                deactivate.accept(row);
            }
        }
    }

    private static boolean isActiveFlag(JsonNode row) {
        if (row == null) {
            return true;
        }
        JsonNode n = row.get("is_active");
        if (n == null || n.isNull()) {
            n = row.get("isActive");
        }
        if (n == null || n.isNull()) {
            return true;
        }
        return n.asBoolean(true);
    }

    /** Readable order: line1, line2, location/area, city, state, zip, country. */
    private static String formatAddress(JsonNode address) {
        if (address == null || address.isNull() || address.isMissingNode()) {
            return null;
        }
        if (address.isTextual()) {
            return address.asText();
        }
        if (!address.isObject()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        appendAddressPart(sb, text(address, "address_line_1", "addressLine1", "line1", "street"));
        appendAddressPart(sb, text(address, "address_line_2", "addressLine2", "line2"));
        appendAddressPart(sb, text(address, "location", "area", "locality"));
        appendAddressPart(sb, text(address, "city"));
        appendAddressPart(sb, text(address, "state", "stateName"));
        appendAddressPart(sb, text(address, "zip_code", "zipCode", "pincode", "postalCode", "pin_code"));
        appendAddressPart(sb, text(address, "country"));
        return sb.length() == 0 ? null : sb.toString();
    }

    private static void appendAddressPart(StringBuilder sb, String value) {
        if (blank(value)) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(value.trim());
    }

    private static String text(JsonNode node, String... keys) {
        if (node == null) {
            return null;
        }
        for (String key : keys) {
            JsonNode v = node.get(key);
            if (v != null && !v.isNull() && v.isValueNode()) {
                String s = v.asText();
                if (s != null && !s.isBlank()) {
                    return s;
                }
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (!blank(v)) {
                return v;
            }
        }
        return null;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimToNull(String value) {
        return blank(value) ? null : value.trim();
    }
}
