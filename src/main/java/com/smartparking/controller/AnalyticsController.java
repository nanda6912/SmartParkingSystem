package com.smartparking.controller;

import com.smartparking.entity.Booking;
import com.smartparking.entity.BookingEvent;
import com.smartparking.entity.ParkingSlot;
import com.smartparking.enums.BookingStatus;
import com.smartparking.enums.VehicleType;
import com.smartparking.repository.BookingEventRepository;
import com.smartparking.repository.BookingRepository;
import com.smartparking.service.AnalyticsService;
import com.smartparking.service.OccupancySnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-only analytics endpoints for permanent booking history and
 * lifecycle events. All endpoints are protected by AuthInterceptor
 * (session required) via /api/analytics/** path registration.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * Reporting zone for analytics date defaults. Matches the snapshot
     * bucketing zone ({@code parking.analytics.zone-id}); stored booking
     * timestamps remain JVM-local.
     */
    @Value("${parking.analytics.zone-id:Asia/Kolkata}")
    private String zoneId = "Asia/Kolkata";

    @Autowired
    private BookingEventRepository bookingEventRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private OccupancySnapshotService occupancySnapshotService;

    /**
     * KPI overview: completed sessions, recorded revenue, averages and
     * live (current-state) occupancy. Date filters apply to historical
     * metrics only; occupancy fields always reflect right now.
     */
    @GetMapping("/overview")
    public ResponseEntity<?> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (invalidRange(from, to)) {
            return badRequest("'from' must not be after 'to'");
        }
        try {
            return ResponseEntity.ok(analyticsService.getOverview(toFrom(from), toTo(to)));
        } catch (Exception e) {
            log.error("Error building analytics overview: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Failed to build overview"));
        }
    }

    /** Recorded revenue analytics (daily/weekly/monthly, by method and floor). */
    @GetMapping("/revenue")
    public ResponseEntity<?> getRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (invalidRange(from, to)) {
            return badRequest("'from' must not be after 'to'");
        }
        try {
            return ResponseEntity.ok(analyticsService.getRevenue(toFrom(from), toTo(to)));
        } catch (Exception e) {
            log.error("Error building revenue analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Failed to build revenue analytics"));
        }
    }

    /** Occupancy analytics from occupancy_snapshots plus live current state. */
    @GetMapping("/occupancy")
    public ResponseEntity<?> getOccupancy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer floor) {
        if (invalidRange(from, to)) {
            return badRequest("'from' must not be after 'to'");
        }
        if (floor != null && floor < 1) {
            return badRequest("'floor' must be a positive integer");
        }
        try {
            return ResponseEntity.ok(analyticsService.getOccupancy(toFrom(from), toTo(to), floor));
        } catch (Exception e) {
            log.error("Error building occupancy analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Failed to build occupancy analytics"));
        }
    }

    /**
     * Snapshot collection health: enabled flag, configured interval,
     * latest/earliest snapshot timestamps, last-24h count, overdue flag
     * and missing-interval detection. All values come from real
     * occupancy_snapshots rows and parking.analytics.* configuration.
     */
    @GetMapping("/occupancy/health")
    public ResponseEntity<?> getOccupancyHealth() {
        try {
            return ResponseEntity.ok(occupancySnapshotService.getSnapshotHealth());
        } catch (Exception e) {
            log.error("Error building occupancy snapshot health: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Failed to build occupancy health"));
        }
    }

    /** Vehicle-type analytics for completed sessions. */
    @GetMapping("/vehicles")
    public ResponseEntity<?> getVehicles(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (invalidRange(from, to)) {
            return badRequest("'from' must not be after 'to'");
        }
        try {
            return ResponseEntity.ok(analyticsService.getVehicles(toFrom(from), toTo(to)));
        } catch (Exception e) {
            log.error("Error building vehicle analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Failed to build vehicle analytics"));
        }
    }

    /** Operational analytics: booking volumes, payments, trends, status mix. */
    @GetMapping("/operations")
    public ResponseEntity<?> getOperations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (invalidRange(from, to)) {
            return badRequest("'from' must not be after 'to'");
        }
        try {
            return ResponseEntity.ok(analyticsService.getOperations(toFrom(from), toTo(to)));
        } catch (Exception e) {
            log.error("Error building operational analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Failed to build operational analytics"));
        }
    }

    /** Hourly entry/exit/revenue traffic profile and peak hours. */
    @GetMapping("/hourly-traffic")
    public ResponseEntity<?> getHourlyTraffic(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (invalidRange(from, to)) {
            return badRequest("'from' must not be after 'to'");
        }
        try {
            return ResponseEntity.ok(analyticsService.getHourlyTraffic(toFrom(from), toTo(to)));
        } catch (Exception e) {
            log.error("Error building hourly traffic analytics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Failed to build hourly traffic analytics"));
        }
    }

    /**
     * CSV export of the transaction report. PII is masked consistently
     * with the booking-history API. Capped at 50,000 rows.
     */
    @GetMapping("/reports/transactions.csv")
    public ResponseEntity<?> exportTransactionsCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false) String paymentMethod) {
        if (invalidRange(from, to)) {
            return badRequest("'from' must not be after 'to'");
        }
        try {
            LocalDateTime fromDt = toFrom(from);
            LocalDateTime toDt = toTo(to);
            var page = bookingRepository.searchBookingHistory(
                    fromDt, toDt, null, vehicleType, paymentMethod, null,
                    org.springframework.data.domain.PageRequest.of(0, 50000));

            StringBuilder csv = new StringBuilder();
            csv.append("booking_id,booking_code,vehicle_number,customer_name,phone_number,vehicle_type,"
                    + "slot_id,floor,booking_time,exit_time,duration_minutes,parking_fee,status,"
                    + "payment_method,transaction_id,payment_time\n");
            for (Booking b : page.getContent()) {
                csv.append(csv(b.getId())).append(',')
                        .append(csv(b.getBookingCode())).append(',')
                        .append(csv(maskVehicleNumber(b.getVehicleNumber()))).append(',')
                        .append(csv(maskCustomerName(b.getCustomerName()))).append(',')
                        .append(csv(maskPhoneNumber(b.getPhoneNumber()))).append(',')
                        .append(csv(b.getVehicleType() != null ? b.getVehicleType().name() : "")).append(',')
                        .append(csv(b.getParkingSlot() != null ? b.getParkingSlot().getSlotId() : "")).append(',')
                        .append(csv(b.getParkingSlot() != null ? String.valueOf(b.getParkingSlot().getFloor()) : "")).append(',')
                        .append(csv(b.getBookingTime() != null ? b.getBookingTime().format(FORMATTER) : "")).append(',')
                        .append(csv(b.getExitTime() != null ? b.getExitTime().format(FORMATTER) : "")).append(',')
                        .append(csv(effectiveDurationMinutes(b) != null
                                ? String.valueOf(effectiveDurationMinutes(b)) : "")).append(',')
                        .append(csv(b.getParkingFee() != null ? String.valueOf(b.getParkingFee()) : "")).append(',')
                        .append(csv(b.getStatus() != null ? b.getStatus().name() : "")).append(',')
                        .append(csv(b.getPaymentMethod())).append(',')
                        .append(csv(b.getTransactionId())).append(',')
                        .append(csv(b.getPaymentTime() != null ? b.getPaymentTime().format(FORMATTER) : ""))
                        .append('\n');
            }
            if (page.getTotalElements() > page.getContent().size()) {
                log.warn("Transaction CSV export truncated at {} of {} rows", page.getContent().size(), page.getTotalElements());
            }

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=transactions_"
                            + LocalDate.now(reportingZone()).format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv")
                    .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                    .body(csv.toString());
        } catch (Exception e) {
            log.error("Error exporting transaction CSV: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Failed to export transactions"));
        }
    }

    /**
     * Duration for completed bookings: stored duration_minutes when valid,
     * otherwise exit_time - booking_time clamped at zero (legacy rows).
     */
    private static Long effectiveDurationMinutes(Booking b) {
        if (b.getDurationMinutes() != null && b.getDurationMinutes() >= 0) {
            return b.getDurationMinutes();
        }
        if (b.getBookingTime() != null && b.getExitTime() != null) {
            return Math.max(0, java.time.Duration.between(b.getBookingTime(), b.getExitTime()).toMinutes());
        }
        return null;
    }

    private static boolean invalidRange(LocalDate from, LocalDate to) {
        return from != null && to != null && from.isAfter(to);
    }

    private static ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", message));
    }

    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        // Neutralize spreadsheet formula injection for untrusted text fields
        if (!s.isEmpty() && "=+-@\t\r\n".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private ZoneId reportingZone() {
        return ZoneId.of(zoneId != null ? zoneId : "Asia/Kolkata");
    }

    private LocalDateTime toFrom(LocalDate from) {
        return from != null ? from.atStartOfDay()
                : LocalDate.now(reportingZone()).minusDays(30).atStartOfDay();
    }

    private LocalDateTime toTo(LocalDate to) {
        return to != null ? to.plusDays(1).atStartOfDay()
                : LocalDate.now(reportingZone()).plusDays(1).atStartOfDay();
    }

    /**
     * Full lifecycle event history for a single booking, oldest first.
     */
    @GetMapping("/bookings/{bookingId}/history")
    public ResponseEntity<?> getBookingEventHistory(@PathVariable Long bookingId) {
        try {
            if (!bookingRepository.existsById(bookingId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "error", "Booking not found",
                        "message", "No booking found with ID: " + bookingId));
            }

            List<BookingEvent> events = bookingEventRepository
                    .findByBookingIdOrderByEventTimestampAscIdAsc(bookingId);

            List<Map<String, Object>> result = events.stream()
                    .map(this::toEventMap)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "bookingId", bookingId,
                    "eventCount", result.size(),
                    "events", result));
        } catch (Exception e) {
            log.error("Error fetching event history for booking {}: {}", bookingId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Failed to fetch booking history"));
        }
    }

    /**
     * Paged, filterable permanent booking history (transaction report data).
     * PII fields are masked consistently with DataSyncService.
     */
    @GetMapping("/booking-history")
    public ResponseEntity<?> getBookingHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (invalidRange(from, to)) {
            return badRequest("'from' must not be after 'to'");
        }
        try {
            int safePage = Math.max(page, 0);
            int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
            Pageable pageable = PageRequest.of(safePage, safeSize);

            LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
            LocalDateTime toDateTime = to != null ? to.plusDays(1).atStartOfDay() : null;

            Page<Booking> bookingPage = bookingRepository.searchBookingHistory(
                    fromDateTime, toDateTime, status, vehicleType,
                    paymentMethod, active, pageable);

            List<Map<String, Object>> content = bookingPage.getContent().stream()
                    .map(this::toBookingHistoryMap)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("content", content);
            response.put("totalElements", bookingPage.getTotalElements());
            response.put("totalPages", bookingPage.getTotalPages());
            response.put("number", bookingPage.getNumber());
            response.put("size", bookingPage.getSize());
            response.put("first", bookingPage.isFirst());
            response.put("last", bookingPage.isLast());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching booking history: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Failed to fetch booking history"));
        }
    }

    private Map<String, Object> toEventMap(BookingEvent event) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", event.getId());
        map.put("bookingId", event.getBookingId());
        map.put("bookingCode", event.getBookingCode() != null && !event.getBookingCode().isEmpty()
                ? event.getBookingCode() : (event.getBookingId() != null ? "ID-" + event.getBookingId() : null));
        map.put("eventType", event.getEventType() != null ? event.getEventType().name() : null);
        map.put("previousStatus", event.getPreviousStatus());
        map.put("newStatus", event.getNewStatus());
        map.put("eventTimestamp", event.getEventTimestamp() != null
                ? event.getEventTimestamp().format(FORMATTER) : null);
        map.put("parkingSlotId", event.getParkingSlotId());
        map.put("additionalDetails", sanitizeEventDetails(event.getAdditionalDetails()));
        return map;
    }

    /**
     * Event details may embed raw booking fields (e.g. vehicleNumber in
     * BOOKING_CREATED). Return a copy with the same PII masking applied
     * as the booking-history endpoint; stored audit data is unchanged.
     */
    private Map<String, Object> sanitizeEventDetails(Map<String, Object> details) {
        if (details == null) {
            return null;
        }
        Map<String, Object> copy = new HashMap<>(details);
        copy.replaceAll((key, value) -> {
            if ("vehicleNumber".equals(key) && value instanceof String vehicleNumber) {
                return maskVehicleNumber(vehicleNumber);
            }
            return normalizeJsonbValue(value);
        });
        return copy;
    }

    /**
     * JSONB round-trips can deserialize timestamps into
     * [year, month, day, hour, minute, second, nano] arrays. Normalize
     * them back to the same "yyyy-MM-dd HH:mm:ss" format used elsewhere.
     */
    private static Object normalizeJsonbValue(Object value) {
        if (!(value instanceof List<?> list) || list.size() < 6
                || !list.subList(0, 6).stream().allMatch(Number.class::isInstance)) {
            return value;
        }
        try {
            int nano = list.size() > 6 && list.get(6) instanceof Number n ? n.intValue() : 0;
            LocalDateTime dt = LocalDateTime.of(
                    ((Number) list.get(0)).intValue(), ((Number) list.get(1)).intValue(),
                    ((Number) list.get(2)).intValue(), ((Number) list.get(3)).intValue(),
                    ((Number) list.get(4)).intValue(), ((Number) list.get(5)).intValue(), nano);
            return dt.format(FORMATTER);
        } catch (RuntimeException e) {
            return value;
        }
    }

    private Map<String, Object> toBookingHistoryMap(Booking booking) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", booking.getId());
        map.put("bookingCode", booking.getBookingCode() != null && !booking.getBookingCode().isEmpty()
                ? booking.getBookingCode() : "ID-" + booking.getId());
        map.put("vehicleNumber", maskVehicleNumber(booking.getVehicleNumber()));
        map.put("customerName", maskCustomerName(booking.getCustomerName()));
        map.put("phoneNumber", maskPhoneNumber(booking.getPhoneNumber()));
        map.put("vehicleType", booking.getVehicleType() != null ? booking.getVehicleType().name() : null);

        ParkingSlot slot = booking.getParkingSlot();
        if (slot != null) {
            map.put("slotId", slot.getSlotId());
            map.put("slotNumber", slot.getSlotNumber());
            map.put("floor", slot.getFloor());
        } else {
            map.put("slotId", "UNKNOWN");
            map.put("slotNumber", 0);
            map.put("floor", 0);
        }

        map.put("bookingTime", booking.getBookingTime() != null
                ? booking.getBookingTime().format(FORMATTER) : null);
        map.put("exitTime", booking.getExitTime() != null
                ? booking.getExitTime().format(FORMATTER) : null);
        map.put("durationMinutes", booking.getDurationMinutes());
        map.put("parkingFee", booking.getParkingFee());
        map.put("status", booking.getStatus() != null ? booking.getStatus().name() : null);
        map.put("isActive", booking.getIsActive());
        map.put("paymentMethod", booking.getPaymentMethod());
        map.put("transactionId", booking.getTransactionId());
        map.put("paymentTime", booking.getPaymentTime() != null
                ? booking.getPaymentTime().format(FORMATTER) : null);
        map.put("piiMasked", true);
        return map;
    }

    private String maskVehicleNumber(String vehicleNumber) {
        if (vehicleNumber == null) {
            return "***";
        }
        return vehicleNumber.length() > 3
                ? "***" + vehicleNumber.substring(vehicleNumber.length() - 3)
                : "***";
    }

    private String maskCustomerName(String customerName) {
        if (customerName == null) {
            return "**";
        }
        return customerName.length() > 2
                ? customerName.substring(0, 2) + "***"
                : "**";
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "***-***-****";
        }
        return "***-***-" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
