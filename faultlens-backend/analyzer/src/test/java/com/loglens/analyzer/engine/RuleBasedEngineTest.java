package com.loglens.analyzer.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.loglens.analyzer.rule.ClassNotFoundRule;
import com.loglens.analyzer.rule.ConnectionTimeoutRule;
import com.loglens.analyzer.rule.DatabaseConnectionRule;
import com.loglens.analyzer.rule.NullPointerRule;
import com.loglens.analyzer.rule.OutOfMemoryRule;
import com.loglens.analyzer.rule.StackOverflowRule;
import com.loglens.common.dto.LogEntryDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleBasedEngineTest {
    @Test
    void appliesFirstMatchingRuleByPriority() {
        RuleBasedEngine engine = new RuleBasedEngine(List.of(
                new ConnectionTimeoutRule(),
                new DatabaseConnectionRule(),
                new NullPointerRule(),
                new StackOverflowRule(),
                new OutOfMemoryRule(),
                new ClassNotFoundRule()));
        LogEntryDto entry = LogEntryDto.builder()
                .message("java.lang.NullPointerException")
                .stackTrace("at com.example.PaymentService.pay(PaymentService.java:42)")
                .build();

        AnalysisResult result = engine.analyze(entry, null);

        assertThat(result.matchedRule()).isEqualTo("NullPointerRule");
        assertThat(result.confidenceScore()).isEqualTo(0.9);
    }

    @Test
    void returnsUnknownWhenNoRuleMatches() {
        RuleBasedEngine engine = new RuleBasedEngine(List.of(new NullPointerRule()));

        AnalysisResult result = engine.analyze(LogEntryDto.builder().message("plain log").build(), null);

        assertThat(result.rootCause()).isEqualTo("Bilinmeyen hata");
        assertThat(result.confidenceScore()).isEqualTo(0.1);
    }

    @Test
    void detectsRequiredRules() {
        LogEntryDto stackOverflow = LogEntryDto.builder().message("java.lang.StackOverflowError").build();
        LogEntryDto classpath = LogEntryDto.builder().message("java.lang.NoClassDefFoundError: com/example/Missing").build();
        LogEntryDto database = LogEntryDto.builder().message("SQLException: too many connections").build();

        assertThat(new StackOverflowRule().matches(stackOverflow)).isTrue();
        assertThat(new ClassNotFoundRule().matches(classpath)).isTrue();
        assertThat(new DatabaseConnectionRule().matches(database)).isTrue();
    }
}
