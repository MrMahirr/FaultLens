package com.faultlens.api.dto;

/** Result of clearing a source's logs: {@code {"cleared": true}}. */
public record ClearedResponse(boolean cleared) {}
