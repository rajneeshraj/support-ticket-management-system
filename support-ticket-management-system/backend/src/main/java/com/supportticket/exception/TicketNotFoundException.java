package com.supportticket.exception;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(long id) {
        super("Ticket " + id + " was not found.");
    }
}
