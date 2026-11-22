package com.supportticket.controller;

import com.supportticket.dto.ChangeStatusRequest;
import com.supportticket.dto.CreateTicketRequest;
import com.supportticket.dto.TicketDetail;
import com.supportticket.dto.TicketPage;
import com.supportticket.dto.UpdateTicketRequest;
import com.supportticket.model.TicketStatus;
import com.supportticket.service.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketDetail> create(@Valid @RequestBody CreateTicketRequest request) {
        TicketDetail created = ticketService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + created.id())).body(created);
    }

    @GetMapping
    public TicketPage list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) @Size(max = 100, message = "Search text must be at most 100 characters.") String q,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be 0 or greater.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1.")
            @Max(value = TicketService.MAX_PAGE_SIZE, message = "Size must be at most 100.") int size) {
        return ticketService.list(status, q, page, size);
    }

    @GetMapping("/{id}")
    public TicketDetail get(@PathVariable long id) {
        return ticketService.get(id);
    }

    @PutMapping("/{id}")
    public TicketDetail update(@PathVariable long id, @Valid @RequestBody UpdateTicketRequest request) {
        return ticketService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public TicketDetail changeStatus(@PathVariable long id, @Valid @RequestBody ChangeStatusRequest request) {
        return ticketService.changeStatus(id, request.status());
    }
}
