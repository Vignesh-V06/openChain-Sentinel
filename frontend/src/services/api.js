import axios from "axios";


const api = axios.create({
  baseURL: "http://localhost:8080/api",

  headers: {
    "Content-Type": "application/json",
  },
});


/* =====================================================
   HEALTH
===================================================== */

export const getHealth = async () => {

  const response =
    await api.get("/health");

  return response.data;
};


/* =====================================================
   PROJECTS
===================================================== */

export const getProjects = async () => {

  const response =
    await api.get("/projects");

  return response.data;
};


export const getProject = async (
  projectId
) => {

  const response =
    await api.get(
      `/projects/${projectId}`
    );

  return response.data;
};


export const createProject = async (
  project
) => {

  const response =
    await api.post(
      "/projects",
      project
    );

  return response.data;
};


export const updateProject = async (
  projectId,
  project
) => {

  const response =
    await api.put(
      `/projects/${projectId}`,
      project
    );

  return response.data;
};


export const deleteProject = async (
  projectId
) => {

  const response =
    await api.delete(
      `/projects/${projectId}`
    );

  return response.data;
};


/* =====================================================
   SCANS
===================================================== */

export const createScan = async (
  projectId
) => {

  const response =
    await api.post(
      `/projects/${projectId}/scans`
    );

  return response.data;
};


export const getScans = async (
  projectId
) => {

  const response =
    await api.get(
      `/projects/${projectId}/scans`
    );

  return response.data;
};


export const getScan = async (
  projectId,
  scanId
) => {

  const response =
    await api.get(
      `/projects/${projectId}/scans/${scanId}`
    );

  return response.data;
};


/* =====================================================
   DEPENDENCIES
===================================================== */

export const getScanDependencies = async (
  projectId,
  scanId
) => {

  const response =
    await api.get(
      `/projects/${projectId}/scans/${scanId}/dependencies`
    );

  return response.data;
};


/* =====================================================
   VULNERABILITIES
===================================================== */

export const getVulnerabilities = async (
  projectId,
  scanId
) => {

  const response =
    await api.get(
      `/projects/${projectId}/scans/${scanId}/vulnerabilities`
    );

  return response.data;
};


/* =====================================================
   RISK
===================================================== */

export const getRiskAssessments = async (
  projectId,
  scanId
) => {

  const response =
    await api.get(
      `/projects/${projectId}/scans/${scanId}/risk`
    );

  return response.data;
};


/* =====================================================
   SUMMARY
===================================================== */

export const getScanSummary = async (
  projectId,
  scanId
) => {

  const response =
    await api.get(
      `/projects/${projectId}/scans/${scanId}/summary`
    );

  return response.data;
};


export default api;