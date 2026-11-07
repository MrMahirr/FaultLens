package com.faultlens.processor.event;

import com.faultlens.common.enums.Severity;

/**
 * A log group was created: an error/message never seen before (NEW_GROUP alarm rules, ADR 0001).
 * Published inside the transaction that stored the group; listeners should run after commit.
 */
public record LogGroupCreatedEvent(Long groupId, Severity severity, Long sourceId, String firstMessage) {
}
