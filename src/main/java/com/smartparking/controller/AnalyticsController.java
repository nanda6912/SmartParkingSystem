package com.smartparking.controller;

import com.smartparking.entity.Booking;
import com.smartparking.entity.BookingEvent;
import com.smartparking.entity.ParkingSlot;
import com.smartparking.enums.BookingStatus;
import com.smartparking.enums.VehicleType;
import com.smartparking.repository.BookingEventRepository;
import com.smartparking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Autowired
    private BookingEventRepository bookingEventRepository;

    @Autowired
    private BookingRepository bookingRepository;

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
        map.put("additionalDetails", event.getAdditionalDetails());
        return map;
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
