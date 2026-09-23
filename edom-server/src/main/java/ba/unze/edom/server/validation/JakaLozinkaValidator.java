package ba.unze.edom.server.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class JakaLozinkaValidator
        implements ConstraintValidator<JakaLozinka, String> {

    @Override
    public boolean isValid(String vrijednost, ConstraintValidatorContext ctx) {

        String poruka = LozinkaPravila.poruka(vrijednost);
        if (poruka == null) return true;

        ctx.disableDefaultConstraintViolation();
        ctx.buildConstraintViolationWithTemplate(poruka).addConstraintViolation();
        return false;
    }
}