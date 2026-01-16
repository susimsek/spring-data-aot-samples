package io.github.susimsek.springdataaotsamples.service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.susimsek.springdataaotsamples.service.validation.constraints.EnumValue;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintViolationBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EnumValueValidatorTest {

    private enum SampleEnum {
        FOO,
        BAR
    }

    @Test
    void isValidShouldReturnTrueForNull() {
        EnumValueValidator validator = validatorFor(SampleEnum.class);

        assertThat(validator.isValid(null, mock(ConstraintValidatorContext.class))).isTrue();
    }

    @Test
    void isValidShouldReturnTrueWhenValueIsInEnum() {
        EnumValueValidator validator = validatorFor(SampleEnum.class);
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

        assertThat(validator.isValid("FOO", context)).isTrue();
        verify(context, never()).disableDefaultConstraintViolation();
    }

    @Test
    void isValidShouldReturnFalseAndAddAllowedValuesWhenValueIsNotInEnum() {
        EnumValueValidator validator = validatorFor(SampleEnum.class);

        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        HibernateConstraintValidatorContext hibernateContext =
                mock(HibernateConstraintValidatorContext.class);
        HibernateConstraintViolationBuilder builder =
                mock(HibernateConstraintViolationBuilder.class);

        when(context.unwrap(HibernateConstraintValidatorContext.class)).thenReturn(hibernateContext);
        when(hibernateContext.addMessageParameter(eq("allowedValues"), anyString()))
                .thenReturn(hibernateContext);
        when(hibernateContext.getDefaultConstraintMessageTemplate()).thenReturn("{message}");
        when(hibernateContext.buildConstraintViolationWithTemplate("{message}")).thenReturn(builder);
        when(builder.addConstraintViolation()).thenReturn(context);

        boolean valid = validator.isValid("BAZ", context);

        assertThat(valid).isFalse();
        verify(context).disableDefaultConstraintViolation();

        ArgumentCaptor<String> allowedValuesCaptor = ArgumentCaptor.forClass(String.class);
        verify(hibernateContext)
                .addMessageParameter(eq("allowedValues"), allowedValuesCaptor.capture());
        assertThat(allowedValuesCaptor.getValue()).contains("FOO").contains("BAR");
    }

    private static EnumValueValidator validatorFor(Class<? extends Enum<?>> enumClass) {
        EnumValue annotation = mock(EnumValue.class);
        // Use doReturn to avoid generic capture issues in Mockito stubbing.
        doReturn(enumClass).when(annotation).enumClass();
        EnumValueValidator validator = new EnumValueValidator();
        validator.initialize(annotation);
        return validator;
    }
}
