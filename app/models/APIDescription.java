package models;

import java.lang.annotation.*;


@Target(value=ElementType.METHOD)
@Retention(value= RetentionPolicy.RUNTIME)
public @interface APIDescription {
    public String value() default "";
}
