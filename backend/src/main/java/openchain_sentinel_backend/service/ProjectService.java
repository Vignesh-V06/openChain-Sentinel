package openchain_sentinel_backend.service;

import openchain_sentinel_backend.model.Project;
import openchain_sentinel_backend.repository.ProjectRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(
            ProjectRepository projectRepository) {

        this.projectRepository = projectRepository;
    }

    /*
     * =====================================================
     * CREATE
     * =====================================================
     */

    public Project createProject(
            Project project,
            String userId) {

        project.setUserId(userId);

        if (project.getCreatedAt() == null) {
            project.setCreatedAt(LocalDateTime.now());
        }

        return projectRepository.save(project);
    }


    /*
     * =====================================================
     * READ ALL - USER SCOPED
     * =====================================================
     */

    public List<Project> getAllProjects(
            String userId) {

        return projectRepository.findByUserId(userId);
    }


    /*
     * =====================================================
     * READ ONE - USER SCOPED
     * =====================================================
     */

    public Project getProjectById(
            String id,
            String userId) {

        return projectRepository
                .findByIdAndUserId(id, userId)
                .orElse(null);
    }


    /*
     * =====================================================
     * UPDATE - USER SCOPED
     * =====================================================
     */

    public Project updateProject(
            String id,
            Project updatedProject,
            String userId) {

        Project existingProject =
                projectRepository
                        .findByIdAndUserId(
                                id,
                                userId
                        )
                        .orElse(null);

        if (existingProject == null) {
            return null;
        }

        existingProject.setName(
                updatedProject.getName()
        );

        existingProject.setRepositoryUrl(
                updatedProject.getRepositoryUrl()
        );

        /*
         * IMPORTANT:
         *
         * Never allow the client to change
         * ownership through the request body.
         */
        existingProject.setUserId(userId);

        return projectRepository.save(
                existingProject
        );
    }


    /*
     * =====================================================
     * DELETE - USER SCOPED
     * =====================================================
     */

    public boolean deleteProject(
            String id,
            String userId) {

        Project existingProject =
                projectRepository
                        .findByIdAndUserId(
                                id,
                                userId
                        )
                        .orElse(null);

        if (existingProject == null) {
            return false;
        }

        projectRepository.delete(
                existingProject
        );

        return true;
    }


    /*
     * =====================================================
     * INTERNAL / LEGACY METHODS
     * =====================================================
     *
     * These are intentionally retained so that existing
     * backend services do not suddenly break because of
     * the Phase B ownership change.
     *
     * Controllers should use the user-scoped methods above.
     */

    public Project createProject(Project project) {

        return projectRepository.save(project);
    }

    public List<Project> getAllProjects() {

        return projectRepository.findAll();
    }

    public Project getProjectById(String id) {

        return projectRepository
                .findById(id)
                .orElse(null);
    }

    public Project updateProject(
            String id,
            Project updatedProject) {

        Project existingProject =
                projectRepository
                        .findById(id)
                        .orElse(null);

        if (existingProject == null) {
            return null;
        }

        existingProject.setName(
                updatedProject.getName()
        );

        existingProject.setRepositoryUrl(
                updatedProject.getRepositoryUrl()
        );

        return projectRepository.save(
                existingProject
        );
    }

    public boolean deleteProject(String id) {

        if (!projectRepository.existsById(id)) {
            return false;
        }

        projectRepository.deleteById(id);

        return true;
    }
}