package com.faultlens.api.alarm;

/** Published after a rule is created, changed or deleted, so the evaluator reloads at once. */
public record AlarmRulesChangedEvent() {
}
