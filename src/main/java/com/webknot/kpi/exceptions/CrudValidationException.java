package com.webknot.kpi.exceptions;
import java.util.Set;

import jakarta.validation.ConstraintViolation;

public class CrudValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private Class<?> affectedEntity = null;
    private Set<?> violations = null;
    private CrudValidationErrorCode errorCode = CrudValidationErrorCode.UNKNOWN;

    public CrudValidationException() {

    }

    public CrudValidationException(Class<?> affectedEntity, Set<?> violations) {
        super("CRUD validation exception in entity: [" + affectedEntity.getName() + "] with " + violations.size() + " violations");

        this.affectedEntity = affectedEntity;
        this.violations = violations;
        this.errorCode = CrudValidationErrorCode.DATA_VALIDATION;
    }

    public CrudValidationException(String message)
    {
        super(message);
    }

    public CrudValidationException(Class<?> affectedEntity, String message, CrudValidationErrorCode errorCode) {
        super(message);

        this.affectedEntity = affectedEntity;
        this.errorCode = errorCode;
    }

    public Class<?> getAffectedEntity() {
        return affectedEntity;
    }

    public Set<?> getViolations() {
        return violations;
    }

    public CrudValidationErrorCode getErrorCode() {
        return errorCode;
    }

    public static CrudValidationException asFailedValidationOperation(Class<?> affectedEntity, Set<?> violations) {
        return new CrudValidationException(affectedEntity, violations);
    }

    public static CrudValidationException asInvalidEntityId(Class<?> affectedEntity) {
        return new CrudValidationException(affectedEntity, "Invalid entity identifier passed. Entity identifier should be >= 1", CrudValidationErrorCode.INVALID_IDENTIFIER);
    }

    public static CrudValidationException asMissingEntity(Class<?> affectedEntity, long id) {
        return new CrudValidationException(affectedEntity, "Could not find entity with ID = " + id, CrudValidationErrorCode.COULD_NOT_FIND_ENTITY);
    }

    public static CrudValidationException asInvalidUser(Class<?> affectedEntity, long userID) {
        return new CrudValidationException(affectedEntity, "Could not find user with ID = " + userID, CrudValidationErrorCode.COULD_NOT_FIND_USER);
    }

    public static CrudValidationException asInavlidEntry(String item) {
        return new CrudValidationException(item + " Cannot be Zero");
    }

    public String getViolationsAsString() {
        StringBuffer buffer = new StringBuffer();

        if (hasViolations()) {
            for(Object violation : violations) {
                ConstraintViolation<?> someViolation = (ConstraintViolation<?>) violation;
                if (someViolation.getPropertyPath() != null) {
                    buffer.append(someViolation.getPropertyPath());
                    buffer.append(": ");
                }
                buffer.append(someViolation.getMessage());
                buffer.append(";");
            }
        }

        return buffer.toString();
    }

    public boolean hasViolations() {
        return ((violations != null) && (violations.size() > 0));
    }
}
