package com.smartparking.service;

import com.smartparking.entity.Booking;
import com.smartparking.entity.ParkingSlot;
import com.smartparking.enums.BookingStatus;
import com.smartparking.enums.SlotStatus;
import com.smartparking.enums.VehicleType;
import com.smartparking.repository.BookingRepository;
import com.smartparking.repository.ParkingSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExitServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ParkingSlotRepository parkingSlotRepository;

    @Mock
    private UPIPaymentService upiPaymentService;

    @Mock
    private BookingEventService bookingEventService;

    @InjectMocks
    private ExitService exitService;

    private Booking activeBooking;
    private ParkingSlot occupiedSlot;

    @BeforeEach
    void setUp() {
        occupiedSlot = new ParkingSlot(7, 1);
        occupiedSlot.setId(10L);
        occupiedSlot.setStatus(SlotStatus.OCCUPIED);

        activeBooking = new Booking();
        activeBooking.setId(42L);
        activeBooking.setBookingCode("AB12C");
        activeBooking.setParkingSlot(occupiedSlot);
        activeBooking.setVehicleNumber("KA01AB1234");
        activeBooking.setVehicleType(VehicleType.CAR);
        activeBooking.setCustomerName("John Doe");
        activeBooking.setPhoneNumber("9876543210");
        activeBooking.setBookingTime(LocalDateTime.now().minusMinutes(90));
        activeBooking.setStatus(BookingStatus.ACTIVE);
        activeBooking.setIsActive(true);
    }

    private void stubActiveBookingLookup() {
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(activeBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(parkingSlotRepository.save(any(ParkingSlot.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void processExit_setsDurationAndRecordsEvents() {
        stubActiveBookingLookup();

        Map<String, Object> result = exitService.processExit(42L);

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(activeBooking.getDurationMinutes()).isEqualTo(90L);
        assertThat(activeBooking.getExitTime()).isNotNull();
        // Fee rule preserved: ceil(90/60) * 20 = 40
        assertThat(activeBooking.getParkingFee()).isEqualTo(40);
        assertThat(activeBooking.getIsActive()).isFalse();
        assertThat(activeBooking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(occupiedSlot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);

        verify(bookingEventService).recordVehicleExited(eq(activeBooking), eq("ACTIVE"));
        verify(bookingEventService).recordStatusChanged(eq(activeBooking), eq("ACTIVE"), eq("COMPLETED"));
        verify(bookingEventService, never()).recordPaymentRecorded(any(), anyString());
    }

    @Test
    void processExit_feeCalculationUnchanged() {
        stubActiveBookingLookup();

        // 30 minutes -> ceil(30/60)=1 hour -> Rs 20
        activeBooking.setBookingTime(LocalDateTime.now().minusMinutes(30));

        exitService.processExit(42L);

        assertThat(activeBooking.getParkingFee()).isEqualTo(20);
        assertThat(activeBooking.getDurationMinutes()).isEqualTo(30L);
    }

    @Test
    void processExit_propagatesEventFailureForRollback() {
        stubActiveBookingLookup();
        doThrow(new RuntimeException("event insert failed"))
                .when(bookingEventService).recordVehicleExited(any(Booking.class), anyString());

        assertThatThrownBy(() -> exitService.processExit(42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process exit");
    }

    @Test
    void processExitWithPayment_setsDurationAndRecordsAllEvents() {
        stubActiveBookingLookup();
        when(upiPaymentService.validateTransactionId("12345")).thenReturn(true);

        Map<String, Object> result = exitService.processExitWithPayment(42L, "UPI", "12345");

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(activeBooking.getDurationMinutes()).isEqualTo(90L);
        assertThat(activeBooking.getPaymentMethod()).isEqualTo("UPI");
        assertThat(activeBooking.getTransactionId()).isEqualTo("12345");
        assertThat(activeBooking.getPaymentTime()).isNotNull();
        assertThat(activeBooking.getParkingFee()).isEqualTo(40);

        verify(bookingEventService).recordVehicleExited(eq(activeBooking), eq("ACTIVE"));
        verify(bookingEventService).recordPaymentRecorded(eq(activeBooking), eq("ACTIVE"));
        verify(bookingEventService).recordStatusChanged(eq(activeBooking), eq("ACTIVE"), eq("COMPLETED"));
    }

    @Test
    void processExitWithPayment_rejectsAlreadyExitedBooking() {
        activeBooking.setExitTime(LocalDateTime.now().minusMinutes(5));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(activeBooking));

        Map<String, Object> result = exitService.processExitWithPayment(42L, "CASH", null);

        assertThat(result).containsKey("error");
        verify(bookingEventService, never()).recordVehicleExited(any(), anyString());
        verify(bookingEventService, never()).recordPaymentRecorded(any(), anyString());
    }

    @Test
    void cleanupOldBookings_preservesCompletedBookings() {
        // Completed booking with an invalid legacy code must never be deleted
        ParkingSlot completedSlot = new ParkingSlot(8, 1);
        completedSlot.setId(11L);
        completedSlot.setStatus(SlotStatus.AVAILABLE);
        Booking completed = new Booking();
        completed.setId(1L);
        completed.setBookingCode("123");
        completed.setParkingSlot(completedSlot);
        completed.setExitTime(LocalDateTime.now().minusDays(1));
        completed.setParkingFee(40);
        completed.setStatus(BookingStatus.COMPLETED);
        completed.setIsActive(false);

        // Incomplete legacy booking with invalid code is still cleaned up
        ParkingSlot tempSlot = new ParkingSlot(9, 1);
        tempSlot.setId(12L);
        tempSlot.setStatus(SlotStatus.OCCUPIED);
        Booking temp = new Booking();
        temp.setId(2L);
        temp.setBookingCode("12");
        temp.setParkingSlot(tempSlot);
        temp.setStatus(BookingStatus.ACTIVE);
        temp.setIsActive(true);

        when(bookingRepository.findAll()).thenReturn(List.of(completed, temp));

        Map<String, Object> result = exitService.cleanupOldBookings();

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("removedCount")).isEqualTo(1);
        verify(bookingRepository, never()).delete(completed);
        verify(bookingRepository).delete(temp);
        assertThat(tempSlot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
        assertThat(completedSlot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void processExit_rejectsAlreadyExitedBooking() {
        activeBooking.setExitTime(LocalDateTime.now().minusMinutes(5));
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(activeBooking));

        assertThatThrownBy(() -> exitService.processExit(42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exited");

        verify(bookingEventService, never()).recordVehicleExited(any(), anyString());
    }
}
