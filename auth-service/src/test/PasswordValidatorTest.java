import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordValidatorTest {

    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordValidator();
    }

    @Test
    void strongPasswordIsValid() {
        PasswordValidator.ValidationResult result = validator.validate("Str0ng!Pass");
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void nullPasswordIsRejected() {
        PasswordValidator.ValidationResult result = validator.validate(null);
        assertFalse(result.isValid());
    }

    @Test
    void emptyPasswordIsRejected() {
        PasswordValidator.ValidationResult result = validator.validate("");
        assertFalse(result.isValid());
    }

    @Test
    void shortPasswordIsRejected() {
        PasswordValidator.ValidationResult result = validator.validate("Ab1!");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("at least 8")));
    }

    @Test
    void passwordWithoutUppercaseIsRejected() {
        PasswordValidator.ValidationResult result = validator.validate("lowercase1!");
        assertFalse(result.isValid());
    }

    @Test
    void passwordWithoutLowercaseIsRejected() {
        PasswordValidator.ValidationResult result = validator.validate("UPPERCASE1!");
        assertFalse(result.isValid());
    }

    @Test
    void passwordWithoutDigitIsRejected() {
        PasswordValidator.ValidationResult result = validator.validate("NoDigits!!");
        assertFalse(result.isValid());
    }

    @Test
    void passwordWithoutSpecialCharIsRejected() {
        PasswordValidator.ValidationResult result = validator.validate("NoSpecial1A");
        assertFalse(result.isValid());
    }
}
