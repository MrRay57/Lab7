package app.common.models.validators;

import java.io.Serializable;

/**
 * Интерфейс для объектов, поддерживающих проверку валидности своих полей
 */
public interface Validatable extends Serializable {
    boolean validate();
}