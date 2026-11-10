package com.smartparking.repository;

import com.smartparking.entity.BookingEvent;
import com.smartparking.enums.BookingEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingEventRepository extends JpaRepository<BookingEvent, Long> {

    List<BookingEvent> findByBookingIdOrderByEventTimestampAscIdAsc(Long bookingId);

    boolean existsByBookingIdAndEventType(Long bookingId, BookingEventType eventType);
}
