package jdk.test.lib.crac;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark fields in {@link CracTest} that should be populated from main method arguments.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CracTestArg {
    /**
     * @return The (zero-based) index of the argument used as source of the data.
     */
    int value() default 0;

    /**
     * @return Can this argument be omitted?
     */
    boolean optional() default false;
}
