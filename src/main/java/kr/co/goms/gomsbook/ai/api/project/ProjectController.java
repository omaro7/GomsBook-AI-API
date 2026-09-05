package kr.co.goms.gomsbook.ai.api.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import kr.co.goms.gomsbook.ai.project.CurrentProjectStore;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private static final String OEBPS_DIRECTORY = "OEBPS";
    private static final String PACKAGE_FILE = "content.opf";

    private final Path projectRoot;
    private final CurrentProjectStore currentProjectStore;


    public ProjectController(@Value("${gomsbook.ai.project-root}") String projectRoot, CurrentProjectStore currentProjectStore) {

        this.projectRoot = Path.of(projectRoot).toAbsolutePath().normalize();
        this.currentProjectStore = currentProjectStore;
    }


    @GetMapping
    public ProjectListResponse getProjects() {

        validateProjectRoot();

        try {

            List<ProjectItemResponse> projects = Files.list(projectRoot)
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .map(this::toProjectItem)
                    .toList();

            return new ProjectListResponse(projectRoot.toString(), projects);

        } catch (IOException e) {

            throw new IllegalStateException("Failed to read EPUB project directories: " + projectRoot, e);
        }
    }


    @GetMapping("/current")
    public CurrentProjectResponse getCurrentProject() {

        Path currentProjectRoot = currentProjectStore.getCurrentProjectRoot();

        if (currentProjectRoot == null) {

            return new CurrentProjectResponse(null, null);
        }

        Path normalized = currentProjectRoot.toAbsolutePath().normalize();

        return new CurrentProjectResponse(normalized.getFileName().toString(), normalized.toString());
    }


    @PutMapping("/current")
    public CurrentProjectResponse switchCurrentProject(@RequestBody SwitchCurrentProjectRequest request) {

        String projectName = requireProjectName(request);
        Path projectPath = resolveProjectPath(projectName);

        validateProject(projectPath);

        currentProjectStore.setCurrentProjectRoot(projectPath);

        return new CurrentProjectResponse(projectName, projectPath.toString());
    }


    private ProjectItemResponse toProjectItem(Path projectPath) {

        Path normalized = projectPath.toAbsolutePath().normalize();
        Path currentProjectRoot = currentProjectStore.getCurrentProjectRoot();
        boolean current = currentProjectRoot != null && normalized.equals(currentProjectRoot.toAbsolutePath().normalize());

        return new ProjectItemResponse(normalized.getFileName().toString(), normalized.toString(), current);
    }


    private Path resolveProjectPath(String projectName) {

        Path resolved = projectRoot.resolve(projectName).toAbsolutePath().normalize();

        if (!resolved.startsWith(projectRoot)) {

            throw new IllegalArgumentException("Project path is outside project-root: " + projectName);
        }

        return resolved;
    }


    private void validateProject(Path projectPath) {

        if (!Files.isDirectory(projectPath)) {

            throw new IllegalArgumentException("EPUB project directory does not exist: " + projectPath);
        }

        Path packageDocument = projectPath.resolve(OEBPS_DIRECTORY).resolve(PACKAGE_FILE);

        if (!Files.isRegularFile(packageDocument)) {

            throw new IllegalArgumentException("EPUB package document does not exist: " + packageDocument);
        }
    }


    private void validateProjectRoot() {

        if (!Files.isDirectory(projectRoot)) {

            throw new IllegalStateException("Project root does not exist or is not a directory: " + projectRoot);
        }
    }


    private String requireProjectName(SwitchCurrentProjectRequest request) {

        if (request == null || request.projectName() == null || request.projectName().isBlank()) {

            throw new IllegalArgumentException("projectName must not be blank");
        }

        return request.projectName().trim();
    }


    public record ProjectListResponse(String projectRoot, List<ProjectItemResponse> projects) {
    }


    public record ProjectItemResponse(String projectName, String projectPath, boolean current) {
    }


    public record CurrentProjectResponse(String projectName, String projectPath) {
    }


    public record SwitchCurrentProjectRequest(String projectName) {
    }
}