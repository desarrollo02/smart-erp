package py.com.logixone.plugins.referencedata.application.policy;

/** Raised when the persisted policy no longer has the version observed by the actor. */
public final class ConcurrentReferenceDataPolicyChangeException extends RuntimeException {

    public ConcurrentReferenceDataPolicyChangeException() {
        super("Reference data policy version conflict");
    }
}
