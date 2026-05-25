package com.loglens.processor.parser;

import com.loglens.common.enums.Severity;
import java.util.Map;

public record ParsedLog(String parsedMessage, String stackTrace, Severity severity, Map<String, String> metadata) {
}
