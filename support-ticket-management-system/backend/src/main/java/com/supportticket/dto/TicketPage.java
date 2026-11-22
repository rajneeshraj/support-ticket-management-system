package com.supportticket.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record TicketPage(
        List<TicketSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static TicketPage from(Page<TicketSummary> page) {
        return new TicketPage(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
