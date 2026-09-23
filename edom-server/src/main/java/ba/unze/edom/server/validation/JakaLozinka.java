package ba.unze.edom.server.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = JakaLozinkaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface JakaLozinka {
    String message() default "Lozinka nije dovoljno jaka";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}