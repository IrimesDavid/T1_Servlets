package com.csvplatform.common.jobs;

/**
 * Content of a job's status.json.
 *
 * @param state   where the job currently is
 * @param message optional detail, e.g. the error text when state is FAILED (may be null)
 */
public record JobStatus(State state, String message) {

    public enum State {
        UPLOADED, ANALYZING, ANALYZED, CLEANING, DONE, FAILED
    }

    public static JobStatus of(State state) {
        return new JobStatus(state, null);
    }

    public static JobStatus failed(String message) {
        return new JobStatus(State.FAILED, message);
    }
}