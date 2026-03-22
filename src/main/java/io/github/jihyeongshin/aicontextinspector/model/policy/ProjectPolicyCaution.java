package io.github.jihyeongshin.aicontextinspector.model.policy;

public enum ProjectPolicyCaution {
    AMBIGUITY_PRESENT("ambiguity_present"),
    HOTSPOT_PRESENT("hotspot_present"),
    LOW_CONFIDENCE_PRESENT("low_confidence_present"),
    UNKNOWN_AFFINITY_PRESENT("unknown_affinity_present"),
    RULE_INPUT_MISSING("rule_input_missing");

    private final String code;

    ProjectPolicyCaution(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
