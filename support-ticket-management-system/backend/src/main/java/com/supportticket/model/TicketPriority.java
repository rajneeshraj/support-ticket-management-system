package com.supportticket.model;

public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static final TicketPriority DEFAULT = MEDIUM;
}
