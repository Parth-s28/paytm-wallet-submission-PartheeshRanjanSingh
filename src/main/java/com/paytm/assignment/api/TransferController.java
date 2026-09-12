package com.paytm.assignment.api;

import com.paytm.assignment.service.TransferService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public TransferModels.TransferResponse createTransfer(
            @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
            @RequestBody TransferModels.TransferRequest request) {

        return transferService.transferMoney(
                idempotencyKey,
                request.fromWalletId(),
                request.toWalletId(),
                request.amountPaise()
        );
    }
}