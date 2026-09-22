package com.paddling.exception;

/**
 * 同一支队伍同一个月重复封账时抛出。
 * HTTP 409 Conflict —— 后点的那次明确失败，由前端展示后端给出的失败原因，不做静默覆盖。
 */
public class SettlementAlreadySealedException extends BusinessException {

    public SettlementAlreadySealedException(String message) {
        super(409, 409, message);
    }
}
