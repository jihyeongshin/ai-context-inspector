package io.github.jihyeongshin.aicontextinspector.extractor;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;

import java.util.Arrays;
import java.util.List;

public class ProjectClassFinder {

    public PsiClass findByQualifiedName(Project project, String qualifiedName) {
        if (project == null || qualifiedName == null || qualifiedName.isBlank()) {
            return null;
        }
        return JavaPsiFacade.getInstance(project)
                .findClass(qualifiedName, GlobalSearchScope.projectScope(project));
    }

    public List<PsiClass> findBySimpleName(Project project, String simpleName) {
        if (project == null || simpleName == null || simpleName.isBlank()) {
            return List.of();
        }
        // JavaPsiFacade
        PsiClass[] classes = PsiShortNamesCache.getInstance(project)
                .getClassesByName(simpleName, GlobalSearchScope.projectScope(project));
        return Arrays.asList(classes);
    }

    public PsiClass findInSamePackageFirst(Project project, String packageName, String simpleName) {
        List<PsiClass> candidates = findBySimpleName(project, simpleName);

        for (PsiClass candidate : candidates) {
            String qualifiedName = candidate.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }

            int lastDot = qualifiedName.lastIndexOf('.');
            String candidatePackage = lastDot >= 0 ? qualifiedName.substring(0, lastDot) : "";
            if (candidatePackage.equals(packageName)) {
                return candidate;
            }
        }

        return candidates.isEmpty() ? null : candidates.getFirst();
    }
}
