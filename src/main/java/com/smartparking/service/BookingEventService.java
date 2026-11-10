package com.smartparking.service;

import com.smartparking.entity.Booking;
import com.smartparking.entity.BookingEvent;
import com.smartparking.entity.ParkingSlot;
import com.smartparking.enums.BookingEventType;
import com.smartparking.repository.BookingEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Records permanent booking lifecycle events. All methods join the caller's
 * transaction so event rows commit or roll back together with the booking
 * update that produced them.
 */
@Service
public class BookingEventService {

    private static final Logger log = LoggerFactory.getLogger(BookingEventService.class);

    @Autowired
    private BookingEventRepository bookingEventRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void recordBookingCreated(Booking booking) {
        BookingEvent event = baseEvent(booking, BookingEventType.BOOKING_CREATED);
        event.setPreviousStatus(null);
        event.setNewStatus(statusName(booking));
        event.setAdditionalDetails(createdDetails(booking));
        bookingEventRepository.save(event);
        log.debug("Recorded BOOKING_CREATED event for booking {}", booking.getId());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void recordVehicleExited(Booking booking, String previousStatus) {
        if (bookingEventRepository.existsByBookingIdAndEventType(
                booking.getId(), BookingEventType.VEHICLE_EXITED)) {
            log.warn("Skipping duplicate VEHICLE_EXITED event for booking {}", booking.getId());
            return;
        }
        BookingEvent event = baseEvent(booking, BookingEventType.VEHICLE_EXITED);
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(statusName(booking));
        event.setAdditionalDetails(exitedDetails(booking));
        bookingEventRepository.save(event);
        log.debug("Recorded VEHICLE_EXITED event for booking {}", booking.getId());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void recordPaymentRecorded(Booking booking, String previousStatus) {
        if (bookingEventRepository.existsByBookingIdAndEventType(
                booking.getId(), BookingEventType.PAYMENT_RECORDED)) {
            log.warn("Skipping duplicate PAYMENT_RECORDED event for booking {}", booking.getId());
            return;
        }
        BookingEvent event = baseEvent(booking, BookingEventType.PAYMENT_RECORDED);
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(statusName(booking));
        event.setAdditionalDetails(paymentDetails(booking));
        bookingEventRepository.save(event);
        log.debug("Recorded PAYMENT_RECORDED event for booking {}", booking.getId());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void recordStatusChanged(Booking booking, String previousStatus, String newStatus) {
        if (previousStatus != null && previousStatus.equals(newStatus)) {
            return;
        }
        BookingEvent event = baseEvent(booking, BookingEventType.BOOKING_STATUS_CHANGED);
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        bookingEventRepository.save(event);
        log.debug("Recorded BOOKING_STATUS_CHANGED event for booking {}: {} -> {}",
                booking.getId(), previousStatus, newStatus);
    }

    @Transactional(readOnly = true)
    public boolean bookingHasEvents(Long bookingId) {
        return bookingId != null && bookingEventRepository.existsByBookingId(bookingId);
    }

    private BookingEvent baseEvent(Booking booking, BookingEventType type) {
        BookingEvent event = new BookingEvent();
        event.setBookingId(booking.getId());
        event.setBookingCode(booking.getBookingCode());
        event.setEventType(type);
        event.setEventTimestamp(LocalDateTime.now());
        ParkingSlot slot = booking.getParkingSlot();
        if (slot != null) {
            event.setParkingSlotId(slot.getId());
        }
        return event;
    }

    private Map<String, Object> createdDetails(Booking booking) {
        Map<String, Object> details = new HashMap<>();
        details.put("vehicleNumber", booking.getVehicleNumber());
        details.put("vehicleType", booking.getVehicleType() != null ? booking.getVehicleType().name() : null);
        details.put("bookingTime", booking.getBookingTime());
        ParkingSlot slot = booking.getParkingSlot();
        if (slot != null) {
            details.put("slotId", slot.getSlotId());
            details.put("floor", slot.getFloor());
        }
        return details;
    }

    private Map<String, Object> exitedDetails(Booking booking) {
        Map<String, Object> details = new HashMap<>();
        details.put("exitTime", booking.getExitTime());
        details.put("durationMinutes", booking.getDurationMinutes());
        details.put("parkingFee", booking.getParkingFee());
        return details;
    }

    private Map<String, Object> paymentDetails(Booking booking) {
        Map<String, Object> details = new HashMap<>();
        details.put("paymentMethod", booking.getPaymentMethod());
        details.put("transactionId", booking.getTransactionId());
        details.put("paymentTime", booking.getPaymentTime());
        details.put("amount", booking.getParkingFee());
        return details;
    }

    private String statusName(Booking booking) {
        return booking.getStatus() != null ? booking.getStatus().name() : null;
    }
}
