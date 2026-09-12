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
            @RequestBody TransferModels.TransferRequest request) {

        TransferModels.TransferResponse response = transferService.handleTransfer(request);
        return ResponseEntity.ok(response);
    }
}