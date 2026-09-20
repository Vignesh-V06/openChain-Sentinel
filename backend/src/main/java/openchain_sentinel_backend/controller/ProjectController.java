package openchain_sentinel_backend.controller;

import openchain_sentinel_backend.model.Project;
import openchain_sentinel_backend.service.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // CREATE
    @PostMapping
    public Project createProject(@RequestBody Project project) {
        return projectService.createProject(project);
    }

    // READ ALL
    @GetMapping
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    // READ ONE
    @GetMapping("/{id}")
    public Project getProjectById(@PathVariable String id) {
        return projectService.getProjectById(id);
    }

    // UPDATE
    @PutMapping("/{id}")
    public Project updateProject(
            @PathVariable String id,
            @RequestBody Project project) {

        return projectService.updateProject(id, project);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public String deleteProject(@PathVariable String id) {

        boolean deleted = projectService.deleteProject(id);

        if (!deleted) {
            return "Project not found";
        }

        return "Project deleted successfully";
    }
}