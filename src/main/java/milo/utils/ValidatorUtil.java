package milo.utils;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.groups.Default;
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