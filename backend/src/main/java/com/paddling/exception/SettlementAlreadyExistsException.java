package com.paddling.exception;

/**
 * 同一支队伍同一月份重复封账时抛出，语义是冲突（HTTP 409），
 * 提示“已封账”而不是静默覆盖。
 */
public class SettlementAlreadyExistsException extends BusinessException {
    public SettlementAlreadyExistsException(String message) {
        super(message);
    }
}
