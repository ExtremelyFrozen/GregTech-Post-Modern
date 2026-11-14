package com.gregtechceu.gtceu.api.sync_system.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Calls this no-argument method on the client after any of the named fields has changed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ClientFieldChangeListener {

    /**
     * The fields to listen for changes on.
     */
    String[] fieldName();
}
