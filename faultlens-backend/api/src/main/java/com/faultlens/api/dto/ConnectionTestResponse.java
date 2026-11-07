package com.faultlens.api.dto;

/** Result of a source connection test: {@code {"connected": true}}. */
public record ConnectionTestResponse(boolean connected) {}
