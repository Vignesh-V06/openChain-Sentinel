package openchain_sentinel_backend.model;

public class GitHubRepositoryMetadata {

    private String owner;
    private String repository;
    private String defaultBranch;
    private boolean isPrivate;
    private String htmlUrl;

    public GitHubRepositoryMetadata() {
    }

    public GitHubRepositoryMetadata(
            String owner,
            String repository,
            String defaultBranch,
            boolean isPrivate,
            String htmlUrl) {

        this.owner = owner;
        this.repository = repository;
        this.defaultBranch = defaultBranch;
        this.isPrivate = isPrivate;
        this.htmlUrl = htmlUrl;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }
}