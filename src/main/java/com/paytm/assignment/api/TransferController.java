package com.paytm.assignment.api;

import com.paytm.assignment.service.TransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferModels.TransferResponse> createTransfer(
            @RequestHeader(value = "Idempotency-Key", required = false) String headerKey,
            @RequestBody TransferModels.TransferRequest request) {

        // Fallback logic for idempotency key
        String key = (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank())
                ? request.getIdempotencyKey()
                : (headerKey != null && !headerKey.isBlank()) ? headerKey : UUID.randomUUID().toString();

        // Reconstruct the request with the guaranteed key
        TransferModels.TransferRequest fullRequest = new TransferModels.TransferRequest(
                key,
                request.getFromWalletId(),
                request.getToWalletId(),
                request.getAmountPaise()
        );

        TransferModels.TransferResponse response = transferService.handleTransfer(fullRequest);
        return ResponseEntity.ok(response);
    }
}