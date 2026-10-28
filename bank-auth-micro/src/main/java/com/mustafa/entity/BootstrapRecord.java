package com.mustafa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

/** Durable completion marker, committed with the initial application admin. */
@Entity
@Table(name = "bootstrap_records")
public class BootstrapRecord {
    @Id
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private State state;

    public enum State {
        PREPARED,
        COMPLETE
    }

    protected BootstrapRecord() {
    }

    public BootstrapRecord(String name, State state) {
        this.name = name;
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public void complete() {
        this.state = State.COMPLETE;
    }
}
