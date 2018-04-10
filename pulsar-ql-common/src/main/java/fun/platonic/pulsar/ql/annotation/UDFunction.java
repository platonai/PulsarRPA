package fun.platonic.pulsar.ql.annotation;

import javax.annotation.meta.TypeQualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@TypeQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface UDFunction {
    boolean nobuffer() default true;
    boolean deterministic() default false;
}
