package com.tripplanner.exception;

/**
 * Thrown when a call to an external service (Google Maps, Gemini, etc.)
 * fails, times out, or is misconfigured (e.g. missing API key). Mapped to
 * 502/503 so the client can distinguish "our fault" from "their fault"
 * (400/404) and retry appropriately.
 */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
