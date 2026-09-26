package com.example.ticketai.domain;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED;

    /** Returns true if a transition from this status to {@code target} is allowed. */
    public boolean canTransitionTo(TicketStatus target) {
        return switch (this) {
            case OPEN -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == RESOLVED || target == CANCELLED;
            case RESOLVED -> target == CLOSED;
            case CLOSED, CANCELLED -> false;
        };
    }
}
