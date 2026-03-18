package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import io.github.jihyeongshin.aicontextinspector.model.RelatedFileContext;
import io.github.jihyeongshin.aicontextinspector.model.RelatedFlow;

import java.util.*;

public class RelatedFlowCollector {

    private static final int MAX_DEPTH = 3;

    private final RelatedContextCollector relatedContextCollector = new RelatedContextCollector();
    private final ProjectClassFinder projectClassFinder = new ProjectClassFinder();
    private final ClassRoleClassifier classRoleClassifier = new ClassRoleClassifier();

    /**
     * return type: List<RelatedFlow>
     * 결과 값은 Method Call Trace가 아닙니다.
     * field dependency + related context scoring 기반입니다.
     * architecture-prioritized representative chain 입니다.
     *
     */
    public List<RelatedFlow> collect(Project project, PsiJavaFile javaFile, PsiClass sourceClass) {
        if (project == null || javaFile == null || sourceClass == null) {
            return List.of();
        }

        List<RelatedFileContext> firstLevel = relatedContextCollector.collect(project, javaFile, sourceClass);
        if (firstLevel.isEmpty()) {
            return List.of();
        }
        String sourceClassName = sourceClass.getName();
        String sourceClassRole = classRoleClassifier.classify(sourceClass, javaFile.getPackageName()).classRole();
        List<RelatedFileContext> flowStarts = firstLevel.stream()
                .filter(it -> isAllowedFlowStart(sourceClassRole, it.classRole()))
                .toList();
        if (flowStarts.isEmpty()) {
            return List.of();
        }

        List<RelatedFlow> flows = new ArrayList<>();

        RelatedFileContext first = pickBestNext(
                sourceClassRole,
                sourceClassName,
                flowStarts,
                Collections.emptySet()
        );

        Set<String> visited = new LinkedHashSet<>();
        visited.add(sourceClass.getQualifiedName());

        List<String> classNames = new ArrayList<>();
        classNames.add(sourceClass.getName());

        String firstQualifiedName = buildQualifiedName(first);
        visited.add(firstQualifiedName);

        classNames.add(first.className());
        int totalScore = first.score();

        PsiClass currentClass = projectClassFinder.findByQualifiedName(project, buildQualifiedName(first));
        int depth = 2;

        while (currentClass != null && depth < MAX_DEPTH) {
            PsiJavaFile currentJavaFile = currentClass.getContainingFile() instanceof PsiJavaFile
                    ? (PsiJavaFile) currentClass.getContainingFile()
                    : null;

            if (currentJavaFile == null) {
                break;
            }


            String currentClassRole = classRoleClassifier
                    .classify(currentClass, currentJavaFile.getPackageName())
                    .classRole();
            String currentClassName = currentClass.getName();

            List<RelatedFileContext> nextLevel = relatedContextCollector.collect(project, currentJavaFile, currentClass);
            List<RelatedFileContext> nextFlowStarts = nextLevel.stream()
                    .filter(it -> isAllowedFlowStart(currentClassRole, it.classRole()))
                    .toList();

            RelatedFileContext next = pickBestNext(
                    currentClassRole,
                    currentClassName,
                    nextFlowStarts,
                    visited
            );

            if (next == null) {
                break;
            }
            String nextQualifiedName = buildQualifiedName(next);
            visited.add(nextQualifiedName);

            classNames.add(next.className());
            totalScore += next.score();

            visited.add(currentClass.getQualifiedName());
            currentClass = projectClassFinder.findByQualifiedName(project, buildQualifiedName(next));
            depth++;
        }

        flows.add(new RelatedFlow(classNames, totalScore));


        flows.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return flows;
    }

    /**
     * legacy web-core service graph may contain service-to-service chains
     * reciprocal dependencies may exist in field injection based legacy code
     * current flow is dependency-based summary, not runtime-safe execution graph
     *
     */
    private RelatedFileContext pickBestNext(
            String currentClassRole,
            String currentClassName,
            List<RelatedFileContext> candidates,
            Set<String> visited
    ) {
        RelatedFileContext best = null;
        int bestPriority = Integer.MIN_VALUE;

        for (RelatedFileContext candidate : candidates) {
            String qualifiedName = buildQualifiedName(candidate);
            if (visited.contains(qualifiedName)) {
                continue;
            }

            int priority = nextRolePriority(currentClassRole, candidate.classRole())
                    + candidate.score()
                    + nameAffinityScore(currentClassName, candidate.className());

            if (best == null || priority > bestPriority) {
                best = candidate;
                bestPriority = priority;
            }
        }
        return best;
    }

    private String buildQualifiedName(RelatedFileContext context) {
        return context.packageName() + "." + context.className();
    }

    private int nextRolePriority(String sourceClassRole, String classRole) {
        int priority = 0;

        if (classRole == null) return priority;

        switch (classRole) {
            case "Facade":
                priority = 50;
                break;
            case "UseCase":
                priority = 45;
                break;
            case "Service":
                priority = 40;
                break;
            case "Repository":
                priority = 35;
                break;
            case "Entity":
                priority = 25;
                break;
            case "RequestDTO":
            case "ResponseDTO":
            case "DTO":
                priority = 10;
                break;
            case "Exception":
                priority = 5;
                break;
            case "Config":
                priority = 0;
                break;
        }

        if (sourceClassRole.equals(classRole)) {
            priority -= 10;
        }
        if ("Controller".equals(sourceClassRole)) {
            if ("UseCase".equals(classRole) || "Service".equals(classRole) || "Repository".equals(classRole)) {
                priority -= 10;
            }
        }
        if ("Facade".equals(sourceClassRole)) {
            if ("Service".equals(classRole)) {
                priority -= 10;
            }
        }
        return priority;
    }

    private boolean isAllowedFlowStart(String sourceClassRole, String relatedClassRole) {
        if (sourceClassRole == null || relatedClassRole == null) {
            return false;
        }

        switch (sourceClassRole) {
            case "Controller":
                return "Facade".equals(relatedClassRole)
                        || "UseCase".equals(relatedClassRole)
                        || "Service".equals(relatedClassRole);

            case "Facade":
                return "UseCase".equals(relatedClassRole)
                        || "Service".equals(relatedClassRole);

            case "UseCase":
                return "Service".equals(relatedClassRole)
                        || "Repository".equals(relatedClassRole);

            case "Service":
                return "Repository".equals(relatedClassRole);

            default:
                return false;
        }
    }

    // 동위 레벨일 때 도메인 연관성 고려
    private int nameAffinityScore(String sourceClassName, String candidateClassName) {
        if (sourceClassName == null || candidateClassName == null) {
            return 0;
        }

        String normalizedSource = normalizeDomainToken(sourceClassName);
        String normalizedCandidate = normalizeDomainToken(candidateClassName);

        if (normalizedSource.isBlank() || normalizedCandidate.isBlank()) {
            return 0;
        }

        if (normalizedCandidate.startsWith(normalizedSource)) {
            return 20;
        }

        if (normalizedCandidate.contains(normalizedSource)) {
            return 10;
        }

        return 0;
    }

    private String normalizeDomainToken(String className) {
        if (className == null) {
            return "";
        }

        return className
                .replace("Controller", "")
                .replace("Facade", "")
                .replace("UseCase", "")
                .replace("Service", "")
                .replace("Repository", "")
                .replace("Request", "")
                .replace("Response", "")
                .replace("Dto", "")
                .replace("DTO", "")
                .trim();
    }

}
