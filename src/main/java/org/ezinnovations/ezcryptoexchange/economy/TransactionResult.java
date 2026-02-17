package org.ezinnovations.ezcryptoexchange.economy;

public record TransactionResult(boolean success, Reason reason, String message) {
    public enum Reason {
        SUCCESS,
        INSUFFICIENT,
        FAILED
    }

    public static TransactionResult success() {
        return new TransactionResult(true, Reason.SUCCESS, "");
    }

    public static TransactionResult insufficient(String message) {
        return new TransactionResult(false, Reason.INSUFFICIENT, message);
    }

    public static TransactionResult failed(String message) {
        return new TransactionResult(false, Reason.FAILED, message);
    }
}
