package com.faultlens.analyzer.engine;

public record AnalysisResult(
        String rootCause,
        String suggestion,
        String affectedDeployment,
        Double confidenceScore,
        String engineType,
        String matchedRule
) {
    public AnalysisResult(String rootCause, String suggestion, String affectedDeployment, Double confidenceScore, String engineType) {
        this(rootCause, suggestion, affectedDeployment, confidenceScore, engineType, null);
    }

    /**
     * Creates an unknown-cause result.
     */
    public static AnalysisResult unknown() {
        return new AnalysisResult(
                "Bilinmeyen hata",
                "Log detaylarini, son deployment kayitlarini ve bagimli servis durumlarini birlikte incele.",
                null,
                0.1,
                "RULE_BASED",
                null
        );
    }

    /**
     * Returns a copy with adjusted deployment and confidence values.
     */
    public AnalysisResult withCorrelation(String deployment, double confidence) {
        return new AnalysisResult(rootCause, suggestion, deployment, confidence, engineType, matchedRule);
    }
}
