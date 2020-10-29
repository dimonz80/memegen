package models;

import java.lang.annotation.*;

/**
 * Аннотация для документирования API
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface APIDescription {
    public String value() default "";
}
