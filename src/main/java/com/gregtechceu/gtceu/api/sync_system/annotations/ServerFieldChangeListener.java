package com.gregtechceu.gtceu.api.sync_system.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method called after a complete client field-update batch has been committed on the server.
 *
 * <p>
 * The method must be a non-static member of the class that declares the target fields and return {@code void}.
 * It may take no arguments, or accept the exact field type twice: first the old value, then the committed value.
 * A method with value arguments can only bind fields of that same type. Listener failures are logged and do not roll
 * back an accepted update batch.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ServerFieldChangeListener {

    /**
     * The {@link SyncToServer} or {@link SyncBoth} fields observed by the annotated method.
     */
    String[] fieldName();
}
