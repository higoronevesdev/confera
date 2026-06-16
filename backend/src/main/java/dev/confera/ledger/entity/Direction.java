package dev.confera.ledger.entity;

public enum Direction {
    DEBIT, CREDIT;

    public Direction opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}