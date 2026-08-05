package py.com.logixone.tests.jta;

final class ExpectedRollbackException extends RuntimeException {

    ExpectedRollbackException() {
        super("EXPECTED_ROLLBACK");
    }
}
