package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import io.github.jihyeongshin.aicontextinspector.model.RelatedFileContext;
import io.github.jihyeongshin.aicontextinspector.model.RelatedFlow;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RelatedFlowCollector {

    // TODO: legacy service-chain / potential cyclic dependency handling
    // MAX_FLOW_COUNT 값을 올려보면 레거시 코드 서비스 체인의 안티패턴을 볼 수 있다.
    private static final int MAX_FLOW_COUNT = 1;
    private static final int MAX_DEPTH = 3;

    private final RelatedContextCollector relatedContextCollector = new RelatedContextCollector();
    private final ProjectClassFinder projectClassFinder = new ProjectClassFinder();

    public List<RelatedFlow> collect(Project project, PsiJavaFile javaFile, PsiClass sourceClass) {
        if (project == null || javaFile == null || sourceClass == null) {
            return List.of();
        }

        List<RelatedFileContext> firstLevel = relatedContextCollector.collect(project, javaFile, sourceClass);
        if (firstLevel.isEmpty()) {
            return List.of();
        }

        List<RelatedFlow> flows = new ArrayList<>();
        int limit = Math.min(MAX_FLOW_COUNT, firstLevel.size());

        for (int i = 0; i < limit; i++) {
            RelatedFileContext first = firstLevel.get(i);

            // flow 편입 순간 즉시 추가
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

                List<RelatedFileContext> nextLevel = relatedContextCollector.collect(project, currentJavaFile, currentClass);
                RelatedFileContext next = pickBestNext(nextLevel, visited);

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
        }

        flows.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return flows;
    }

    private RelatedFileContext pickBestNext(List<RelatedFileContext> candidates, Set<String> visited) {
        RelatedFileContext best = null;
        int bestPriority = Integer.MIN_VALUE;

        for (RelatedFileContext candidate : candidates) {
            String qualifiedName = buildQualifiedName(candidate);
            if (visited.contains(qualifiedName)) {
                continue;
            }
            int priority = nextRolePriority(candidate.classRole()) + candidate.score();
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

    private int nextRolePriority(String classRole) {
        if (classRole == null) return 0;

        switch (classRole) {
            case "Facade":
                return 50;
            case "UseCase":
                return 45;
            case "Service":
                return 40;
            case "Repository":
                return 35;
            case "Entity":
                return 30;
            case "RequestDTO":
            case "ResponseDTO":
            case "DTO":
                return 15;
            case "Exception":
                return 10;
            case "Config":
                return 5;
            default:
                return 0;
        }
    }

}
