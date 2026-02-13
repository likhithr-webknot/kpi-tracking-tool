package com.webknot.kpi.exceptions;

public class CrudOperationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private Class<?> affectedEntity = null;

    public CrudOperationException() {

    }

    public CrudOperationException(String message, Class<?> affectedEntity) {
        super(message);

        this.affectedEntity = affectedEntity;
    }

    public CrudOperationException(Exception exception, Class<?> affectedEntity) {
        super("CRUD operation exception in entity: [" + affectedEntity.getName() + "]", exception);

        this.affectedEntity = affectedEntity;
    }

    public CrudOperationException(String message, Exception exception, Class<?> affectedEntity) {
        super(message, exception);

        this.affectedEntity = affectedEntity;
    }

    public Class<?> getAffectedEntity() {
        return affectedEntity;
    }

    public static CrudOperationException asNullEntity(Class<?> affectedEntity) {
        return new CrudOperationException("Passed entity is null: " + affectedEntity.getName(), affectedEntity);
    }

    public static CrudOperationException asFailedAddOperation(Class<?> affectedEntity, Exception exception) {
        return new CrudOperationException("Exception while adding a new entity: " + affectedEntity.getName(), exception,
                affectedEntity);
    }

    public static CrudOperationException asFailedGetOperation(Class<?> affectedEntity, Exception exception) {
        return new CrudOperationException("Exception while getting one or more entities of type: " + affectedEntity.getName(), exception,
                affectedEntity);
    }

    public static CrudOperationException asFailedDeleteOperation(Class<?> affectedEntity, Exception exception) {
        return new CrudOperationException("Exception while deleting all entities of type: " + affectedEntity.getName(), exception,
                affectedEntity);
    }

    public static CrudOperationException asEntityNotFound(Class<?> affectedEntity, Object id) {
        String message = "Entity not found: " + affectedEntity.getName() + " with ID [" + id + "]";
        return new CrudOperationException(message, affectedEntity);
    }

    public static CrudOperationException asFailedUpdateOperation(Class<?> affectedEntity, Exception exception) {
        return new CrudOperationException("Exception while updating an existing entity: " + affectedEntity.getName(),
                exception, affectedEntity);
    }
}