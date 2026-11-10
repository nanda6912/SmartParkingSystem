package com.smartparking.service;

import com.smartparking.dto.BookingRequestDTO;
import com.smartparking.dto.BookingResponseDTO;
import com.smartparking.entity.Booking;
import com.smartparking.entity.ParkingSlot;
import com.smartparking.enums.SlotStatus;
import com.smartparking.repository.BookingRepository;
import com.smartparking.repository.ParkingSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingServiceBookingEventTest {

    @Mock
    private ParkingSlotRepository parkingSlotRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingEventService bookingEventService;

    @InjectMocks
    private ParkingService parkingService;

    private ParkingSlot lockedSlot;
    private BookingRequestDTO bookingRequest;

    @BeforeEach
    void setUp() {
        lockedSlot = new ParkingSlot(5, 1);
        lockedSlot.setId(1L);
        lockedSlot.setStatus(SlotStatus.LOCKED);
        lockedSlot.setLockUntil(LocalDateTime.now().plusMinutes(2));

        bookingRequest = new BookingRequestDTO();
        bookingRequest.setSlotId(1L);
        bookingRequest.setVehicleNumber("KA01AB1234");
        bookingRequest.setCustomerName("John Doe");
        bookingRequest.setPhoneNumber("9876543210");
        bookingRequest.setVehicleType("CAR");
    }

    @Test
    void bookSlot_recordsBookingCreatedEvent() {
        when(parkingSlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lockedSlot));
        when(bookingRepository.findActiveBookingByVehicleNumber("KA01AB1234"))
                .thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(parkingSlotRepository.save(any(ParkingSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponseDTO response = parkingService.bookSlot(bookingRequest);

        assertThat(response.getBookingCode()).isNotNull();
        assertThat(lockedSlot.getStatus()).isEqualTo(SlotStatus.OCCUPIED);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingEventService).recordBookingCreated(captor.capture());
        Booking booked = captor.getValue();
        assertThat(booked.getVehicleNumber()).isEqualTo("KA01AB1234");
        assertThat(booked.getBookingTime()).isNotNull();
        assertThat(booked.getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    void bookSlot_doesNotRecordEventWhenSlotNotLocked() {
        lockedSlot.setStatus(SlotStatus.AVAILABLE);
        when(parkingSlotRepository.findByIdWithLock(1L)).thenReturn(Optional.of(lockedSlot));

        BookingResponseDTO response = parkingService.bookSlot(bookingRequest);

        assertThat(response.getBookingCode()).isNull();
        verify(bookingEventService, never()).recordBookingCreated(any(Booking.class));
    }
}
