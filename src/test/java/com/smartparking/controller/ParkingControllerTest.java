package com.smartparking.controller;

import com.smartparking.dto.BookingRequestDTO;
import com.smartparking.dto.BookingResponseDTO;
import com.smartparking.service.ParkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingControllerTest {

    @Mock
    private ParkingService parkingService;

    @InjectMocks
    private ParkingController parkingController;

    private BookingRequestDTO validBookingRequest;

    @BeforeEach
    void setUp() {
        validBookingRequest = new BookingRequestDTO();
        validBookingRequest.setSlotId(1L);
        validBookingRequest.setVehicleNumber("KA01AB1234");
        validBookingRequest.setCustomerName("John Doe");
        validBookingRequest.setPhoneNumber("9876543210");
        validBookingRequest.setVehicleType("CAR");
    }

    @Test
    void shouldReturnSuccess_WhenBookingIsValid() {
        // Given
        BookingResponseDTO successResponse = new BookingResponseDTO("Slot booked successfully");
        successResponse.setBookingCode("ABC12");
        when(parkingService.bookSlot(any(BookingRequestDTO.class))).thenReturn(successResponse);

        // When
        ResponseEntity<BookingResponseDTO> response = parkingController.bookSlot(validBookingRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getBookingCode()).isEqualTo("ABC12");
    }

    @Test
    void shouldReturnBadRequest_WhenBookingFails() {
        // Given
        BookingResponseDTO errorResponse = new BookingResponseDTO("Slot not available");
        when(parkingService.bookSlot(any(BookingRequestDTO.class))).thenReturn(errorResponse);

        // When
        ResponseEntity<BookingResponseDTO> response = parkingController.bookSlot(validBookingRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
