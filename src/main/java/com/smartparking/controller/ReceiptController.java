package com.smartparking.controller;

import com.smartparking.service.ReceiptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/receipt")
public class ReceiptController {
    
    @Autowired
    private ReceiptService receiptService;
    
    @GetMapping("/download/{bookingId}")
    public ResponseEntity<ByteArrayResource> downloadReceipt(@PathVariable Long bookingId) {
        try {
            byte[] receiptData = receiptService.generateReceipt(bookingId);
            ByteArrayResource resource = new ByteArrayResource(receiptData);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt_" + bookingId + ".txt")
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(receiptData.length)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
