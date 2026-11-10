package com.smartparking.service;

import com.smartparking.entity.Booking;
import com.smartparking.entity.BookingEvent;
import com.smartparking.entity.ParkingSlot;
import com.smartparking.enums.BookingEventType;
import com.smartparking.enums.BookingStatus;
import com.smartparking.enums.SlotStatus;
import com.smartparking.repository.BookingEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingEventServiceTest {

    @Mock
    private BookingEventRepository bookingEventRepository;

    @InjectMocks
    private BookingEventService bookingEventService;

    private Booking booking;

    @BeforeEach
    void setUp() {
        ParkingSlot slot = new ParkingSlot(3, 1);
        slot.setId(10L);
        slot.setStatus(SlotStatus.OCCUPIED);

        booking = new Booking();
        booking.setId(42L);
        booking.setBookingCode("AB12C");
        booking.setParkingSlot(slot);
        booking.setVehicleNumber("KA01AB1234");
        booking.setVehicleType(com.smartparking.enums.VehicleType.CAR);
        booking.setBookingTime(LocalDateTime.now().minusMinutes(90));
        booking.setStatus(BookingStatus.ACTIVE);
    }

    @Test
    void recordBookingCreated_buildsEventWithExpectedFields() {
        bookingEventService.recordBookingCreated(booking);

        ArgumentCaptor<BookingEvent> captor = ArgumentCaptor.forClass(BookingEvent.class);
        verify(bookingEventRepository).save(captor.capture());
        BookingEvent event = captor.getValue();

        assertThat(event.getBookingId()).isEqualTo(42L);
        assertThat(event.getBookingCode()).isEqualTo("AB12C");
        assertThat(event.getEventType()).isEqualTo(BookingEventType.BOOKING_CREATED);
        assertThat(event.getPreviousStatus()).isNull();
        assertThat(event.getNewStatus()).isEqualTo("ACTIVE");
        assertThat(event.getParkingSlotId()).isEqualTo(10L);
        assertThat(event.getEventTimestamp()).isNotNull();
        assertThat(event.getAdditionalDetails()).containsEntry("vehicleNumber", "KA01AB1234");
    }

    @Test
    void recordVehicleExited_recordsEventWithExitDetails() {
        booking.setExitTime(LocalDateTime.now());
        booking.setDurationMinutes(90L);
        booking.setParkingFee(40);
        booking.setStatus(BookingStatus.COMPLETED);

        bookingEventService.recordVehicleExited(booking, "ACTIVE");

        ArgumentCaptor<BookingEvent> captor = ArgumentCaptor.forClass(BookingEvent.class);
        verify(bookingEventRepository).save(captor.capture());
        BookingEvent event = captor.getValue();

        assertThat(event.getEventType()).isEqualTo(BookingEventType.VEHICLE_EXITED);
        assertThat(event.getPreviousStatus()).isEqualTo("ACTIVE");
        assertThat(event.getNewStatus()).isEqualTo("COMPLETED");
        assertThat(event.getAdditionalDetails())
                .containsEntry("durationMinutes", 90L)
                .containsEntry("parkingFee", 40);
    }

    @Test
    void recordVehicleExited_skipsDuplicateForSameBooking() {
        when(bookingEventRepository.existsByBookingIdAndEventType(42L, BookingEventType.VEHICLE_EXITED))
                .thenReturn(true);

        bookingEventService.recordVehicleExited(booking, "ACTIVE");

        verify(bookingEventRepository, never()).save(any(BookingEvent.class));
    }

    @Test
    void recordPaymentRecorded_recordsPaymentDetails() {
        booking.setPaymentMethod("UPI");
        booking.setTransactionId("12345");
        booking.setPaymentTime(LocalDateTime.now());
        booking.setParkingFee(40);

        bookingEventService.recordPaymentRecorded(booking, "ACTIVE");

        ArgumentCaptor<BookingEvent> captor = ArgumentCaptor.forClass(BookingEvent.class);
        verify(bookingEventRepository).save(captor.capture());
        BookingEvent event = captor.getValue();

        assertThat(event.getEventType()).isEqualTo(BookingEventType.PAYMENT_RECORDED);
        Map<String, Object> details = event.getAdditionalDetails();
        assertThat(details).containsEntry("paymentMethod", "UPI")
                .containsEntry("transactionId", "12345")
                .containsEntry("amount", 40);
    }

    @Test
    void recordPaymentRecorded_skipsDuplicateForSameBooking() {
        when(bookingEventRepository.existsByBookingIdAndEventType(42L, BookingEventType.PAYMENT_RECORDED))
                .thenReturn(true);

        bookingEventService.recordPaymentRecorded(booking, "ACTIVE");

        verify(bookingEventRepository, never()).save(any(BookingEvent.class));
        verify(bookingEventRepository).existsByBookingIdAndEventType(eq(42L), eq(BookingEventType.PAYMENT_RECORDED));
    }

    @Test
    void recordStatusChanged_noOpWhenStatusUnchanged() {
        bookingEventService.recordStatusChanged(booking, "ACTIVE", "ACTIVE");

        verify(bookingEventRepository, never()).save(any(BookingEvent.class));
    }

    @Test
    void recordStatusChanged_recordsTransition() {
        bookingEventService.recordStatusChanged(booking, "ACTIVE", "COMPLETED");

        ArgumentCaptor<BookingEvent> captor = ArgumentCaptor.forClass(BookingEvent.class);
        verify(bookingEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(BookingEventType.BOOKING_STATUS_CHANGED);
        assertThat(captor.getValue().getPreviousStatus()).isEqualTo("ACTIVE");
        assertThat(captor.getValue().getNewStatus()).isEqualTo("COMPLETED");
    }
}
