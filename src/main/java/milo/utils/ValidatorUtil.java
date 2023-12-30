package milo.utils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.groups.Default;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidatorUtil {

	private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

	public static <T> void validate(final T instance) {
		final Validator validator = factory.getValidator();
		final Set<ConstraintViolation<T>> violations = validator.validate(instance, Default.class);
		if (!violations.isEmpty()) {
			final Set<ConstraintViolation<?>> constraints = new HashSet<>(violations.size());
			constraints.addAll(violations.stream().collect(Collectors.toList()));
			throw new ConstraintViolationException(constraints);
		}
	}
}
