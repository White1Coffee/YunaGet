package nl.yunaget.client.core;

public enum TaskState {
    IDLE, SEARCHING, NAVIGATING, INTERACTING, VERIFYING, COMPLETE, PAUSED, ERROR;
    public boolean terminal() { return this == IDLE || this == COMPLETE || this == ERROR; }
    public boolean canTransitionTo(TaskState next) {
        if (next == this || next == IDLE || next == ERROR || next == PAUSED) return true;
        return switch (this) {
            case IDLE, COMPLETE, ERROR, PAUSED -> next == SEARCHING;
            case SEARCHING -> next == NAVIGATING || next == INTERACTING || next == COMPLETE;
            case NAVIGATING -> next == SEARCHING || next == INTERACTING;
            case INTERACTING -> next == VERIFYING || next == SEARCHING || next == COMPLETE;
            case VERIFYING -> next == INTERACTING || next == SEARCHING || next == COMPLETE;
        };
    }
}
