import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class TransactionServiceTest {

    private static final String ACC_A = "ACC001"; // seeded with 1000.00
    private static final String ACC_B = "ACC002"; // seeded with 500.00
    private static final String ACC_C = "ACC003"; // seeded with 2500.00
    private static final String UNKNOWN = "ACC999";

    private static final double DELTA = 0.0001;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService();
    }

    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Test
        void increasesBalanceForKnownAccount() {
            double newBalance = service.deposit(ACC_A, 250.50);

            assertEquals(1250.50, newBalance, DELTA);
            assertEquals(1250.50, service.getBalance(ACC_A), DELTA);
        }

        @Test
        void initializesBalanceForUnknownAccount() {
            double newBalance = service.deposit(UNKNOWN, 100.00);

            assertEquals(100.00, newBalance, DELTA);
            assertEquals(100.00, service.getBalance(UNKNOWN), DELTA);
        }

        @Test
        void rejectsZeroAmount() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.deposit(ACC_A, 0.0));

            assertEquals("Amount must be positive", ex.getMessage());
            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
        }

        @Test
        void rejectsNegativeAmount() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.deposit(ACC_A, -50.00));

            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        void decreasesBalanceForKnownAccount() {
            double newBalance = service.withdraw(ACC_A, 200.00);

            assertEquals(800.00, newBalance, DELTA);
            assertEquals(800.00, service.getBalance(ACC_A), DELTA);
        }

        @Test
        void allowsWithdrawingEntireBalance() {
            double newBalance = service.withdraw(ACC_B, 500.00);

            assertEquals(0.00, newBalance, DELTA);
            assertEquals(0.00, service.getBalance(ACC_B), DELTA);
        }

        @Test
        void rejectsZeroAmount() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.withdraw(ACC_A, 0.0));

            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
        }

        @Test
        void rejectsNegativeAmount() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.withdraw(ACC_A, -1.0));

            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
        }

        @Test
        void rejectsAmountExceedingBalance() {
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> service.withdraw(ACC_B, 500.01));

            assertTrue(ex.getMessage().contains(ACC_B));
            assertEquals(500.00, service.getBalance(ACC_B), DELTA);
        }

        @Test
        void rejectsWithdrawalFromUnknownAccount() {
            // Unknown accounts default to a zero balance, so any positive
            // amount triggers the insufficient-funds branch.
            assertThrows(
                    IllegalStateException.class,
                    () -> service.withdraw(UNKNOWN, 1.00));
        }
    }

    @Nested
    @DisplayName("transfer")
    class Transfer {

        @Test
        void movesFundsBetweenAccountsOnHappyPath() {
            assertTrue(service.transfer(ACC_A, ACC_B, 300.00));

            assertEquals(700.00, service.getBalance(ACC_A), DELTA);
            assertEquals(800.00, service.getBalance(ACC_B), DELTA);
        }

        @Test
        void rejectsTransferToSameAccount() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.transfer(ACC_A, ACC_A, 100.00));

            assertEquals("Cannot transfer to the same account", ex.getMessage());
            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
        }

        @Test
        void rejectsZeroAmount() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.transfer(ACC_A, ACC_B, 0.0));

            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
            assertEquals(500.00, service.getBalance(ACC_B), DELTA);
        }

        @Test
        void rejectsNegativeAmount() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.transfer(ACC_A, ACC_B, -10.00));

            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
            assertEquals(500.00, service.getBalance(ACC_B), DELTA);
        }

        @Test
        void propagatesInsufficientFundsAndLeavesBalancesUnchanged() {
            assertThrows(
                    IllegalStateException.class,
                    () -> service.transfer(ACC_B, ACC_A, 600.00));

            assertEquals(500.00, service.getBalance(ACC_B), DELTA);
            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
        }

        @Test
        void invokesWithdrawThenDepositInOrder() {
            TransactionService spy = spy(new TransactionService());

            spy.transfer(ACC_A, ACC_B, 100.00);

            InOrder inOrder = inOrder(spy);
            inOrder.verify(spy).withdraw(ACC_A, 100.00);
            inOrder.verify(spy).deposit(ACC_B, 100.00);
        }

        @Test
        void doesNotDepositWhenWithdrawFails() {
            TransactionService spy = spy(new TransactionService());

            assertThrows(
                    IllegalStateException.class,
                    () -> spy.transfer(ACC_B, ACC_A, 600.00));

            verify(spy).withdraw(ACC_B, 600.00);
            verify(spy, never()).deposit(anyString(), anyDouble());
        }
    }

    @Nested
    @DisplayName("getBalance")
    class GetBalance {

        @Test
        void returnsSeededBalances() {
            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
            assertEquals(500.00, service.getBalance(ACC_B), DELTA);
            assertEquals(2500.00, service.getBalance(ACC_C), DELTA);
        }

        @Test
        void throwsWhenAccountNotFound() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> service.getBalance(UNKNOWN));

            assertTrue(ex.getMessage().contains(UNKNOWN));
        }
    }
}
