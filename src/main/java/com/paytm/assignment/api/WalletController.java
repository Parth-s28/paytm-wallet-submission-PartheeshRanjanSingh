package com.paytm.assignment.api;
import com.paytm.assignment.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/wallets")
public class WalletController {
    private final WalletService service;
    public WalletController(WalletService service) { this.service = service; }

    @PostMapping
    public WalletModels.WalletResponse getOrCreate(HttpServletRequest request) {
        return service.getOrCreate(user(request));
    }

    @GetMapping("/{id}")
    public WalletModels.WalletResponse get(@PathVariable String id) {
        return service.get(id);
    }

    private String user(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) throw new IllegalArgumentException("Missing Bearer token");
        String u = h.substring(7).trim();
        if (u.isBlank()) throw new IllegalArgumentException("Empty Bearer token");
        return u;
    }
}
