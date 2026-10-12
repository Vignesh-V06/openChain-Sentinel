
import axios from "axios";

const TOKEN_KEY = "openchain_token";

const api = axios.create({
  baseURL: "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
});

/* =========================================================
   JWT AUTHENTICATION
   ========================================================= */

// Attach the saved JWT to protected API requests.
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY);

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Clear an invalid/expired session.
// Do not redirect here; the authentication layer will handle
// navigation so that existing API calls remain reusable.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY);
    }

    return Promise.reject(error);
  }
);

// Register a new account.
export const registerUser = async (userData) => {
  const response = await api.post("/auth/register", userData);
  return response.data;
};

// Log in with the credentials expected by your backend.
export const loginUser = async (credentials) => {
  const response = await api.post("/auth/login", credentials);
  return response.data;
};

// Fetch the currently authenticated user.
export const getCurrentUser = async () => {
  const response = await api.get("/auth/me");
  return response.data;
};

// Save or remove the JWT using one shared storage key.
export const saveAuthToken = (token) => {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
};

export const getAuthToken = () => {
  return localStorage.getItem(TOKEN_KEY);
};

export const clearAuthToken = () => {
  localStorage.removeItem(TOKEN_KEY);
};

/* =========================================================
   HEALTH
   ========================================================= */

export const getHealth = async () => {
  const response = await api.get("/health");
  return response.data;
};

/* =========================================================
   PROJECTS
   ========================================================= */

export const getProjects = async () => {
  const response = await api.get("/projects");
  return response.data;
};

export const getProject = async (projectId) => {
  const response = await api.get(`/projects/${projectId}`);
  return response.data;
};

export const createProject = async (projectData) => {
  const response = await api.post("/projects", projectData);
  return response.data;
};

export const updateProject = async (projectId, projectData) => {
  const response = await api.put(
    `/projects/${projectId}`,
    projectData
  );
  return response.data;
};

export const deleteProject = async (projectId) => {
  const response = await api.delete(`/projects/${projectId}`);
  return response.data;
};

/* =========================================================
   SCANS
   ========================================================= */

// Start a scan. The backend runs the scan asynchronously.
export const createScan = async (projectId) => {
  const response = await api.post(
    `/projects/${projectId}/scans`
  );
  return response.data;
};

// Get all scans belonging to a project.
export const getScans = async (projectId) => {
  const response = await api.get(
    `/projects/${projectId}/scans`
  );
  return response.data;
};

// Get a single scan.
export const getScan = async (projectId, scanId) => {
  const response = await api.get(
    `/projects/${projectId}/scans/${scanId}`
  );
  return response.data;
};

// Manually update scan status (development/testing).
export const updateScanStatus = async (
  projectId,
  scanId,
  status
) => {
  const response = await api.put(
    `/projects/${projectId}/scans/${scanId}/status`,
    null,
    {
      params: { status },
    }
  );

  return response.data;
};

/* =========================================================
   DEPENDENCIES
   ========================================================= */

export const getScanDependencies = async (
  projectId,
  scanId
) => {
  const response = await api.get(
    `/projects/${projectId}/scans/${scanId}/dependencies`
  );

  return response.data;
};

/* =========================================================
   VULNERABILITIES
   ========================================================= */

export const getVulnerabilities = async (
  projectId,
  scanId
) => {
  const response = await api.get(
    `/projects/${projectId}/scans/${scanId}/vulnerabilities`
  );

  return response.data;
};

/* =========================================================
   RISK ANALYSIS
   ========================================================= */

export const getRiskAssessments = async (
  projectId,
  scanId
) => {
  const response = await api.get(
    `/projects/${projectId}/scans/${scanId}/risk`
  );

  return response.data;
};

// Overall risk summary for a scan.
export const getScanSummary = async (
  projectId,
  scanId
) => {
  const response = await api.get(
    `/projects/${projectId}/scans/${scanId}/summary`
  );

  return response.data;
};

/* =========================================================
   DEPENDENCY GRAPH
   ========================================================= */

// Fetch the dependency graph from Neo4j.
export const getDependencyGraph = async (
  projectId,
  scanId
) => {
  const response = await api.get(
    `/projects/${projectId}/scans/${scanId}/graph`
  );

  return response.data;
};

/* =========================================================
   OSV / DEVELOPMENT ENDPOINTS
   ========================================================= */

// Query OSV directly for a package version.
export const testOsv = async (
  groupId,
  artifactId,
  version
) => {
  const response = await api.get("/osv/test", {
    params: {
      groupId,
      artifactId,
      version,
    },
  });

  return response.data;
};

// Direct OSV scan endpoint for development/testing.
export const scanRepositoryWithOsv = async (
  url,
  projectId,
  scanId
) => {
  const response = await api.post(
    "/osv/scan",
    null,
    {
      params: {
        url,
        projectId,
        scanId,
      },
    }
  );

  return response.data;
};

/* =========================================================
   DEFAULT AXIOS INSTANCE
   ========================================================= */

export default api;
