package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import io.github.jihyeongshin.aicontextinspector.model.RelatedFileContext;
import io.github.jihyeongshin.aicontextinspector.model.RelatedFlow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RelatedFlowCollector {

    private static final int MAX_FLOW_COUNT = 2;
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

            List<String> classNames = new ArrayList<>();
            classNames.add(sourceClass.getName());
            classNames.add(first.className());

            int totalScore = first.score();
            Set<String> visited = new HashSet<>();
            visited.add(sourceClass.getQualifiedName());

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
        for (RelatedFileContext candidate : candidates) {
            String qualifiedName = buildQualifiedName(candidate);
            if (!visited.contains(qualifiedName)) {
                return candidate;
            }
        }
        return null;
    }

    private String buildQualifiedName(RelatedFileContext context) {
        return context.packageName() + "." + context.className();
    }

}
