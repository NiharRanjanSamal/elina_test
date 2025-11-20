package com.elina.authorization.rule;

/**
 * Exception thrown when a business rule validation fails.
 * 
 * This exception is thrown by rule validators when a business rule is violated.
 * It contains the rule number, message, and hint for the user.
 */
public class BusinessRuleException extends RuntimeException {

    private final Integer ruleNumber;
    private final String hint;

    public BusinessRuleException(Integer ruleNumber, String message, String hint) {
        super(message);
        this.ruleNumber = ruleNumber;
        this.hint = hint;
    }

    public BusinessRuleException(Integer ruleNumber, String message) {
        this(ruleNumber, message, null);
    }

    public Integer getRuleNumber() {
        return ruleNumber;
    }

    public String getHint() {
        return hint;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BusinessRuleException[ruleNumber=").append(ruleNumber);
        sb.append(", message=").append(getMessage());
        if (hint != null) {
            sb.append(", hint=").append(hint);
        }
        sb.append("]");
        return sb.toString();
    }
}

