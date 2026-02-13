package com.webknot.kpi.exceptions;

public enum CrudValidationErrorCode {
    UNKNOWN(0, "Not sure what the validation error is or no validation errors identified"),
    COULD_NOT_FIND_ENTITY(100, "Could not find the entity with the given ID"),
    DATA_VALIDATION(101, "A field in the entity does not have the required value"),
    INVALID_IDENTIFIER(102, "An invalid entity identifier passed"),
    COULD_NOT_FIND_USER(103, "Could not find user with specified ID");

    public final int code;
    public final String message;

    private CrudValidationErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}