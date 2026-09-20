package openchain_sentinel_backend.service;

import openchain_sentinel_backend.model.Project;
import openchain_sentinel_backend.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // CREATE
    public Project createProject(Project project) {
        return projectRepository.save(project);
    }

    // READ ALL
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // READ ONE
    public Project getProjectById(String id) {
        return projectRepository.findById(id).orElse(null);
    }

    // UPDATE
    public Project updateProject(String id, Project updatedProject) {

        Project existingProject = projectRepository.findById(id).orElse(null);

        if (existingProject == null) {
            return null;
        }

        existingProject.setName(updatedProject.getName());
        existingProject.setRepositoryUrl(updatedProject.getRepositoryUrl());

        return projectRepository.save(existingProject);
    }

    // DELETE
    public boolean deleteProject(String id) {

        if (!projectRepository.existsById(id)) {
            return false;
        }

        projectRepository.deleteById(id);
        return true;
    }
}