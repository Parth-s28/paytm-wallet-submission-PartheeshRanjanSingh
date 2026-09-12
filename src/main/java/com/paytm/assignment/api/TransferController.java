package com.paytm.assignment.api;

import com.paytm.assignment.service.TransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        // Resolve key priority: Body parameter -> HTTP Header -> Fallback UUID string
        String key = (request.idempotencyKey() != null && !request.idempotencyKey().isBlank())
                ? request.idempotencyKey()
                : (headerKey != null && !headerKey.isBlank()) ? headerKey : java.util.UUID.randomUUID().toString();

        TransferModels.TransferRequest normalizedRequest = new TransferModels.TransferRequest(
                key,
                request.fromWalletId(),
                request.toWalletId(),
                request.amountPaise()
        );

        TransferModels.TransferResponse response = transferService.handleTransfer(normalizedRequest);
        return ResponseEntity.ok(response);
    }
}