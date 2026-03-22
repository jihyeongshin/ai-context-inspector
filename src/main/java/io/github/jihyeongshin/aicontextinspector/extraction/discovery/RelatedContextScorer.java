package io.github.jihyeongshin.aicontextinspector.extraction.discovery;

public class RelatedContextScorer {

    public int score(
            String sourceClassRole,
            String relatedClassRole,
            String relationType,
            boolean samePackage
    ) {
        int score = 0;

        score += scoreByRelationType(relationType);
        score += scoreByRole(sourceClassRole, relatedClassRole);

        if (samePackage) {
            score += 20;
        }

        return score;
    }

    private int scoreByRelationType(String relationType) {
        if ("FIELD_DEPENDENCY".equals(relationType)) {
            return 100;
        }
        if ("PROJECT_IMPORT".equals(relationType)) {
            return 50;
        }
        return 0;
    }

    private int scoreByRole(String sourceClassRole, String relatedClassRole) {
        if (sourceClassRole == null || relatedClassRole == null) {
            return 0;
        }

        switch (sourceClassRole) {
            case "Controller":
                return scoreForController(relatedClassRole);
            case "Facade":
                return scoreForFacade(relatedClassRole);
            case "UseCase":
                return scoreForUseCase(relatedClassRole);
            case "Service":
                return scoreForService(relatedClassRole);
            default:
                return 0;
        }
    }

    private int scoreForController(String relatedClassRole) {
        switch (relatedClassRole) {
            case "Facade":
                return 40;
            case "UseCase":
                return 30;
            case "Service":
                return 20;
            case "RequestDTO":
            case "ResponseDTO":
                return 15;
            case "Repository":
                return 5;
            case "Exception":
                return 3;
            default:
                return 0;
        }
    }

    private int scoreForFacade(String relatedClassRole) {
        switch (relatedClassRole) {
            case "UseCase":
                return 40;
            case "Service":
                return 25;
            case "RequestDTO":
            case "ResponseDTO":
                return 15;
            case "Exception":
                return 10;
            case "Repository":
                return 5;
            default:
                return 0;
        }
    }

    private int scoreForUseCase(String relatedClassRole) {
        switch (relatedClassRole) {
            case "Service":
                return 35;
            case "Repository":
                return 30;
            case "Entity":
                return 20;
            case "DTO":
            case "RequestDTO":
            case "ResponseDTO":
                return 10;
            default:
                return 0;
        }
    }

    private int scoreForService(String relatedClassRole) {
        switch (relatedClassRole) {
            case "Repository":
                return 35;
            case "Entity":
                return 20;
            case "DTO":
            case "RequestDTO":
            case "ResponseDTO":
                return 10;
            default:
                return 0;
        }
    }

}
