package com.clearbank.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionServiceTest {

    private static final double DELTA = 0.0001;

    private static final String ACC_A = "ACC001";
    private static final String ACC_B = "ACC002";
    private static final String ACC_C = "ACC003";
    private static final String UNKNOWN = "ACC999";

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService();
    }

    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Test
        @DisplayName("credits the account and returns the updated balance")
        void depositsToExistingAccount() {
            double updated = service.deposit(ACC_A, 250.00);

            assertEquals(1250.00, updated, DELTA);
            assertEquals(1250.00, service.getBalance(ACC_A), DELTA);
        }

        @Test
        @DisplayName("rejects a zero amount as a positive-amount violation")
        void rejectsZeroAmount() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.deposit(ACC_A, 0.0)
            );
            assertEquals("Amount must be positive", ex.getMessage());
            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
        }

        @Test
        @DisplayName("rejects a negative amount")
        void rejectsNegativeAmount() {
            assertThrows(
                IllegalArgumentException.class,
                () -> service.deposit(ACC_A, -1.00)
            );
            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
        }

        @Test
        @DisplayName("multiple deposits accumulate")
        void multipleDepositsAccumulate() {
            service.deposit(ACC_A, 100.00);
            service.deposit(ACC_A, 200.00);
            service.deposit(ACC_A, 300.00);

            assertEquals(1600.00, service.getBalance(ACC_A), DELTA);
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("debits the account and returns the updated balance")
        void withdrawsFromExistingAccount() {
            double updated = service.withdraw(ACC_A, 250.00);

            assertEquals(750.00, updated, DELTA);
            assertEquals(750.00, service.getBalance(ACC_A), DELTA);
        }

        @Test
        @DisplayName("rejects a zero amount as a positive-amount violation")
        void rejectsZeroAmount() {
            assertThrows(
                IllegalArgumentException.class,
                () -> service.withdraw(ACC_A, 0.0)
            );
            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
        }

        @Test
        @DisplayName("rejects a negative amount")
        void rejectsNegativeAmount() {
            assertThrows(
                IllegalArgumentException.class,
                () -> service.withdraw(ACC_A, -50.00)
            );
            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
        }

        @Test
        @DisplayName("rejects a withdrawal that would overdraft the account")
        void rejectsOverdraft() {
            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.withdraw(ACC_A, 5000.00)
            );
            assertTrue(
                ex.getMessage().contains(ACC_A),
                "Error message should reference the offending account"
            );
            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
        }

        @Test
        @DisplayName("allows withdrawing the entire balance to zero")
        void withdrawsEntireBalance() {
            double updated = service.withdraw(ACC_A, 1000.00);

            assertEquals(0.00, updated, DELTA);
            assertEquals(0.00, service.getBalance(ACC_A), DELTA);
        }
    }

    @Nested
    @DisplayName("transfer")
    class Transfer {

        @Test
        @DisplayName("debits the source and credits the destination atomically")
        void transfersBetweenAccounts() {
            boolean result = service.transfer(ACC_A, ACC_B, 200.00);

            assertTrue(result);
            assertEquals(800.00, service.getBalance(ACC_A), DELTA);
            assertEquals(700.00, service.getBalance(ACC_B), DELTA);
        }

        @Test
        @DisplayName("rejects a transfer to the same account")
        void rejectsSelfTransfer() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.transfer(ACC_A, ACC_A, 50.00)
            );
            assertEquals("Cannot transfer to the same account", ex.getMessage());
            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
        }

        @Test
        @DisplayName("rejects a non-positive amount")
        void rejectsNonPositiveAmount() {
            assertThrows(
                IllegalArgumentException.class,
                () -> service.transfer(ACC_A, ACC_B, 0.0)
            );
            assertThrows(
                IllegalArgumentException.class,
                () -> service.transfer(ACC_A, ACC_B, -10.00)
            );

            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
            assertEquals(500.00, service.getBalance(ACC_B), DELTA);
        }

        @Test
        @DisplayName("does not credit the destination when the source overdrafts")
        void overdraftLeavesDestinationUnchanged() {
            assertThrows(
                IllegalStateException.class,
                () -> service.transfer(ACC_A, ACC_B, 5000.00)
            );

            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
            assertEquals(500.00, service.getBalance(ACC_B), DELTA);
        }
    }

    @Nested
    @DisplayName("getBalance")
    class GetBalance {

        @Test
        @DisplayName("returns the current balance for a known account")
        void returnsBalanceForExistingAccount() {
            assertEquals(1000.00, service.getBalance(ACC_A), DELTA);
            assertEquals(500.00, service.getBalance(ACC_B), DELTA);
            assertEquals(2500.00, service.getBalance(ACC_C), DELTA);
        }

        @Test
        @DisplayName("rejects a lookup against an unknown account")
        void rejectsUnknownAccount() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.getBalance(UNKNOWN)
            );
            assertTrue(
                ex.getMessage().contains(UNKNOWN),
                "Error message should reference the missing account id"
            );
        }
    }
}
