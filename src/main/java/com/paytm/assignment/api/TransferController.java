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
            @RequestHeader(value = "X-Idempotency-Key", required = false) String headerKey,
            @RequestBody TransferModels.TransferRequest request) {

        // Fallback logic for idempotency key
        String key = (request.idempotencyKey() != null && !request.idempotencyKey().isBlank())
                ? request.idempotencyKey()
                : (headerKey != null && !headerKey.isBlank()) ? headerKey : UUID.randomUUID().toString();

        // Reconstruct the request with the guaranteed key
        TransferModels.TransferRequest fullRequest = new TransferModels.TransferRequest(
                key,
                request.fromWalletId(),
                request.toWalletId(),
                request.amountPaise()
        );

        TransferModels.TransferResponse response = transferService.handleTransfer(fullRequest);
        return ResponseEntity.ok(response);
    }
}