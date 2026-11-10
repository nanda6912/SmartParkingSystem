package com.smartparking.controller;

import com.smartparking.entity.Booking;
import com.smartparking.entity.BookingEvent;
import com.smartparking.entity.ParkingSlot;
import com.smartparking.enums.BookingEventType;
import com.smartparking.enums.BookingStatus;
import com.smartparking.enums.VehicleType;
import com.smartparking.repository.BookingEventRepository;
import com.smartparking.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private BookingEventRepository bookingEventRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private AnalyticsController analyticsController;

    @Test
    void getBookingEventHistory_returnsOrderedEvents() {
        BookingEvent event = new BookingEvent();
        event.setId(1L);
        event.setBookingId(42L);
        event.setBookingCode("AB12C");
        event.setEventType(BookingEventType.BOOKING_CREATED);
        event.setNewStatus("ACTIVE");
        event.setEventTimestamp(LocalDateTime.of(2026, 1, 1, 10, 0));

        when(bookingRepository.existsById(42L)).thenReturn(true);
        when(bookingEventRepository.findByBookingIdOrderByEventTimestampAscIdAsc(42L))
                .thenReturn(List.of(event));

        ResponseEntity<?> response = analyticsController.getBookingEventHistory(42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("success")).isEqualTo(true);
        assertThat(body.get("eventCount")).isEqualTo(1);
        List<?> events = (List<?>) body.get("events");
        Map<?, ?> first = (Map<?, ?>) events.get(0);
        assertThat(first.get("eventType")).isEqualTo("BOOKING_CREATED");
        assertThat(first.get("newStatus")).isEqualTo("ACTIVE");
    }

    @Test
    void getBookingEventHistory_returns404ForMissingBooking() {
        when(bookingRepository.existsById(999L)).thenReturn(false);

        ResponseEntity<?> response = analyticsController.getBookingEventHistory(999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getBookingHistory_returnsMaskedPagedResults() {
        ParkingSlot slot = new ParkingSlot(3, 1);
        slot.setId(10L);

        Booking booking = new Booking();
        booking.setId(42L);
        booking.setBookingCode("AB12C");
        booking.setVehicleNumber("KA01AB1234");
        booking.setCustomerName("John Doe");
        booking.setPhoneNumber("9876543210");
        booking.setVehicleType(VehicleType.CAR);
        booking.setParkingSlot(slot);
        booking.setBookingTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        booking.setExitTime(LocalDateTime.of(2026, 1, 1, 11, 30));
        booking.setDurationMinutes(90L);
        booking.setParkingFee(40);
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setIsActive(false);
        booking.setPaymentMethod("UPI");
        booking.setTransactionId("12345");

        when(bookingRepository.searchBookingHistory(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(booking), PageRequest.of(0, 20), 1));

        ResponseEntity<?> response = analyticsController.getBookingHistory(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1),
                BookingStatus.COMPLETED, VehicleType.CAR, "UPI", false, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("success")).isEqualTo(true);
        assertThat(body.get("totalElements")).isEqualTo(1L);

        List<?> content = (List<?>) body.get("content");
        Map<?, ?> item = (Map<?, ?>) content.get(0);
        assertThat(item.get("vehicleNumber")).isEqualTo("***234");
        assertThat(item.get("customerName")).isEqualTo("Jo***");
        assertThat(item.get("phoneNumber")).isEqualTo("***-***-3210");
        assertThat(item.get("durationMinutes")).isEqualTo(90L);
        assertThat(item.get("parkingFee")).isEqualTo(40);
        assertThat(item.get("status")).isEqualTo("COMPLETED");
        assertThat(item.get("piiMasked")).isEqualTo(true);
    }

    @Test
    void exportTransactionsCsv_fallsBackToComputedDurationForLegacyBookings() {
        ParkingSlot slot = new ParkingSlot(3, 1);
        Booking legacy = new Booking();
        legacy.setId(7L);
        legacy.setBookingCode("XY9ZZ");
        legacy.setVehicleNumber("KA01AB1234");
        legacy.setCustomerName("John Doe");
        legacy.setPhoneNumber("9876543210");
        legacy.setVehicleType(VehicleType.CAR);
        legacy.setParkingSlot(slot);
        legacy.setBookingTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        legacy.setExitTime(LocalDateTime.of(2026, 1, 1, 11, 30));
        legacy.setDurationMinutes(null); // pre-V5 legacy row
        legacy.setParkingFee(40);
        legacy.setStatus(BookingStatus.COMPLETED);
        legacy.setIsActive(false);

        Booking corrupted = new Booking();
        corrupted.setId(8L);
        corrupted.setBookingCode("AB12C");
        corrupted.setVehicleNumber("KA02CD5678");
        corrupted.setCustomerName("Jane Doe");
        corrupted.setPhoneNumber("9123456789");
        corrupted.setVehicleType(VehicleType.BIKE);
        corrupted.setParkingSlot(slot);
        corrupted.setBookingTime(LocalDateTime.of(2026, 1, 2, 10, 0));
        corrupted.setExitTime(LocalDateTime.of(2026, 1, 2, 9, 0)); // exit before entry
        corrupted.setDurationMinutes(-5L); // invalid stored value
        corrupted.setStatus(BookingStatus.COMPLETED);
        corrupted.setIsActive(false);

        when(bookingRepository.searchBookingHistory(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(legacy, corrupted), PageRequest.of(0, 50000), 2));

        ResponseEntity<?> response = analyticsController.exportTransactionsCsv(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String csv = (String) response.getBody();
        assertThat(csv).contains("duration_minutes");
        // legacy row: fallback computes 90 minutes from timestamps
        assertThat(csv).contains(",90,");
        // corrupted row: negative stored value ignored, negative interval clamped to 0
        assertThat(csv).contains(",0,");
    }

    @Test
    void exportTransactionsCsv_passesVehicleAndPaymentFilters() {
        when(bookingRepository.searchBookingHistory(
                any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50000), 0));

        analyticsController.exportTransactionsCsv(null, null, VehicleType.BIKE, "CASH");

        verify(bookingRepository).searchBookingHistory(
                any(), any(), isNull(), eq(VehicleType.BIKE), eq("CASH"), isNull(), any());
    }

    @Test
    void getBookingHistory_capsPageSize() {
        when(bookingRepository.searchBookingHistory(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 200), 0));

        analyticsController.getBookingHistory(null, null, null, null, null, null, 0, 5000);

        verify(bookingRepository).searchBookingHistory(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                org.mockito.ArgumentMatchers.argThat(p -> p.getPageSize() == 200));
    }
}
