import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionServiceTest {

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService();
    }

    @Test
    void depositIncreasesBalance() {
        double result = service.deposit("ACC001", 200.00);
        assertEquals(1200.00, result, 0.001);
    }

    @Test
    void depositRejectsNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> service.deposit("ACC001", -50.00));
    }
}
