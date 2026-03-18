package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import io.github.jihyeongshin.aicontextinspector.model.ClassClassification;
import io.github.jihyeongshin.aicontextinspector.model.RelatedFileContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RelatedContextCollector {

    private static final int MAX_RELATED_CLASSES = 8;

    private final DependencyExtractor dependencyExtractor = new DependencyExtractor();
    private final ImportExtractor importExtractor = new ImportExtractor();
    private final ClassRoleClassifier classRoleClassifier = new ClassRoleClassifier();
    private final ProjectClassFinder projectClassFinder = new ProjectClassFinder();

    private final RelatedContextScorer relatedContextScorer = new RelatedContextScorer();

    public List<RelatedFileContext> collect(Project project, PsiJavaFile javaFile) {
        if (project == null || javaFile == null) {
            return List.of();
        }
        PsiClass sourceClass = javaFile.getClasses().length > 0 ? javaFile.getClasses()[0] : null;
        if (sourceClass == null) {
            return List.of();
        }
        String sourcePackageName = javaFile.getPackageName();
        String sourceClassRole = classRoleClassifier.classify(sourceClass, sourcePackageName).classRole();

        Set<String> visitedQualifiedNames = new LinkedHashSet<>();
        List<RelatedFileContext> result = new ArrayList<>();

        collectFromDependencies(project, sourceClass, sourceClassRole, sourcePackageName, visitedQualifiedNames, result);
        collectFromImports(project, javaFile, sourceClass, sourceClassRole, sourcePackageName, visitedQualifiedNames, result);

        if (result.size() > MAX_RELATED_CLASSES) {
            return result.subList(0, MAX_RELATED_CLASSES);
        }
        result.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return result;
    }

    private void collectFromDependencies(
            Project project,
            PsiClass sourceClass,
            String sourceClassRole,
            String sourcePackageName,
            Set<String> visitedQualifiedNames,
            List<RelatedFileContext> result
    ) {
        List<String> dependencyNames = dependencyExtractor.extract(sourceClass);

        for (String dependencyName : dependencyNames) {
            PsiClass candidate = projectClassFinder.findInSamePackageFirst(project, sourcePackageName, dependencyName);
            if (candidate == null) {
                continue;
            }

            if (shouldSkip(sourceClass, candidate, visitedQualifiedNames)) {
                continue;
            }

            RelatedFileContext context = toRelatedFileContext(sourceClassRole, sourcePackageName, candidate, "FIELD_DEPENDENCY");
            if (context == null) {
                continue;
            }

            visitedQualifiedNames.add(candidate.getQualifiedName());
            result.add(context);

            if (result.size() >= MAX_RELATED_CLASSES) {
                return;
            }
        }
    }

    private void collectFromImports(
            Project project,
            PsiJavaFile javaFile,
            PsiClass sourceClass,
            String sourceClassRole,
            String sourcePackageName,
            Set<String> visitedQualifiedNames,
            List<RelatedFileContext> result
    ) {
        List<String> imports = importExtractor.extract(javaFile);
        for (String importedName : imports) {
            if (!isProjectLikeImport(importedName)) {
                continue;
            }

            PsiClass candidate = projectClassFinder.findByQualifiedName(project, importedName);
            if (candidate == null) {
                continue;
            }

            if (shouldSkip(sourceClass, candidate, visitedQualifiedNames)) {
                continue;
            }

            RelatedFileContext context = toRelatedFileContext(sourceClassRole, sourcePackageName, candidate, "PROJECT_IMPORT");
            if (context == null) {
                continue;
            }

            visitedQualifiedNames.add(candidate.getQualifiedName());
            result.add(context);

            if (result.size() >= MAX_RELATED_CLASSES) {
                return;
            }
        }
    }

    private boolean shouldSkip(PsiClass sourceClass, PsiClass candidate, Set<String> visitedQualifiedNames) {
        if (candidate == null) {
            return true;
        }

        String sourceQualifiedName = sourceClass.getQualifiedName();
        String candidateQualifiedName = candidate.getQualifiedName();

        if (candidateQualifiedName == null || candidateQualifiedName.isBlank()) {
            return true;
        }

        if (candidateQualifiedName.equals(sourceQualifiedName)) {
            return true;
        }

        if (visitedQualifiedNames.contains(candidateQualifiedName)) {
            return true;
        }

        return false;
    }

    private RelatedFileContext toRelatedFileContext(String sourceClassRole, String sourcePackageName, PsiClass psiClass, String relationType) {
        String className = psiClass.getName();
        String qualifiedName = psiClass.getQualifiedName();

        if (className == null || qualifiedName == null) {
            return null;
        }
        PsiJavaFile javaFile = psiClass.getContainingFile() instanceof PsiJavaFile
                ? (PsiJavaFile) psiClass.getContainingFile()
                : null;

        if (javaFile == null) {
            return null;
        }

        String packageName = javaFile.getPackageName();
        ClassClassification classification = classRoleClassifier.classify(psiClass, packageName);

        if (shouldExcludeClassType(classification.classRole())) {
            return null;
        }

        VirtualFile virtualFile = javaFile.getVirtualFile();
        String filePath = virtualFile != null ? virtualFile.getPath() : "Unknown";

        boolean isSamePackage = packageName.equals(sourcePackageName);

        int score = relatedContextScorer.score(sourceClassRole, classification.classRole(), relationType, isSamePackage);

        return new RelatedFileContext(
                className,
                classification.classRole(),
                classification.springStereotype(),
                filePath,
                packageName,
                relationType,
                dependencyExtractor.extract(psiClass),
                score
        );

    }

    private boolean isProjectLikeImport(String importedName) {
        return !(importedName.startsWith("java.")
                || importedName.startsWith("javax.")
                || importedName.startsWith("jakarta.")
                || importedName.startsWith("kotlin.")
                || importedName.startsWith("org.springframework.")
                || importedName.startsWith("org.slf4j.")
                || importedName.startsWith("com.intellij."));
    }

    private boolean shouldExcludeClassType(String classType) {
        return "Unknown".equals(classType)
                || "Enum".equals(classType)
                || "Test".equals(classType);
    }

}
