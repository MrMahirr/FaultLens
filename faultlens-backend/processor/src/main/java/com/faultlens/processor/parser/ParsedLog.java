package com.faultlens.processor.parser;

import com.faultlens.common.enums.Severity;
import java.util.Map;

public record ParsedLog(String parsedMessage, String stackTrace, Severity severity, Map<String, String> metadata) {
}
