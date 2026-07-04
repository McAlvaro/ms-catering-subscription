package com.mcalvaro.mscatering.infrastructure.shared.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValueOfEnumValidator implements ConstraintValidator<ValueOfEnum, Object> {
    private List<String> acceptedValues;

    @Override
    public void initialize(ValueOfEnum annotation) {
        acceptedValues = Stream.of(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String stringValue = (value instanceof Enum<?>) ? ((Enum<?>) value).name() : value.toString();
        boolean isValid = acceptedValues.contains(stringValue);
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "El valor '" + value + "' no es válido. Los valores permitidos son: " + acceptedValues)
                    .addConstraintViolation();
        }
        return isValid;
    }
}
