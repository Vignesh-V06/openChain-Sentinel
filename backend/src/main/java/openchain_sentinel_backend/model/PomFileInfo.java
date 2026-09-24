package openchain_sentinel_backend.model;

public class PomFileInfo {

    private String path;
    private String sha;
    private long size;

    public PomFileInfo() {
    }

    public PomFileInfo(String path, String sha, long size) {
        this.path = path;
        this.sha = sha;
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}