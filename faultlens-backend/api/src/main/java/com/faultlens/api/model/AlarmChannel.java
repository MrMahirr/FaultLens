package com.faultlens.api.model;

/** Where a rule's alarms are announced (each user's own Slack/e-mail settings still apply). */
public enum AlarmChannel {
    IN_APP,
    SLACK,
    EMAIL
}
