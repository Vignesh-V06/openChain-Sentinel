import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  Handle,
  Position,
} from "@xyflow/react";

import "@xyflow/react/dist/style.css";

import {
  AlertTriangle,
  Box,
  GitBranch,
  Layers3,
  Search,
  ShieldAlert,
  Target,
  ZoomIn,
} from "lucide-react";

import {
  getProjects,
  getScans,
  getDependencyGraph,
} from "../services/api";


/* =========================================================
   FILTERS
   ========================================================= */

const FILTERS = {
  ALL: "all",
  DIRECT: "direct",
  TRANSITIVE: "transitive",
  VULNERABLE: "vulnerable",
};


/* =========================================================
   HELPERS
   ========================================================= */

function safeString(value, fallback = "") {
  if (
    value === null ||
    value === undefined
  ) {
    return fallback;
  }

  return String(value);
}


function getNodeLabel(node) {
  if (!node) {
    return "Unknown";
  }

  if (node.type === "project") {
    return (
      node.label ||
      node.name ||
      "Project"
    );
  }

  if (node.type === "vulnerability") {
    return (
      node.vulnerabilityId ||
      node.label ||
      "Vulnerability"
    );
  }

  return (
    node.coordinate ||
    node.label ||
    "Dependency"
  );
}


/* =========================================================
   KEEP ANCESTORS FOR VULNERABLE FILTER
   ========================================================= */

function getAncestorNodeIds(
  targetIds,
  nodes,
  edges
) {
  const required =
    new Set(targetIds);

  let changed = true;

  while (changed) {
    changed = false;

    for (const edge of edges) {
      if (
        required.has(edge.target) &&
        !required.has(edge.source)
      ) {
        required.add(
          edge.source
        );

        changed = true;
      }
    }
  }

  const existingIds =
    new Set(
      nodes.map(
        (node) => node.id
      )
    );

  return new Set(
    [...required].filter(
      (id) =>
        existingIds.has(id)
    )
  );
}


/* =========================================================
   BUILD DEPENDENCY PATH
   ========================================================= */

function buildDependencyPath(
  nodeId,
  nodes,
  edges
) {
  const nodeMap =
    new Map(
      nodes.map(
        (node) => [
          node.id,
          node,
        ]
      )
    );

  const incoming =
    new Map();

  for (const edge of edges) {
    if (
      !incoming.has(
        edge.target
      )
    ) {
      incoming.set(
        edge.target,
        []
      );
    }

    incoming
      .get(edge.target)
      .push(
        edge.source
      );
  }

  const path = [];

  let current =
    nodeId;

  const visited =
    new Set();

  while (
    current &&
    !visited.has(current)
  ) {
    visited.add(current);

    const node =
      nodeMap.get(
        current
      );

    if (!node) {
      break;
    }

    path.unshift(
      getNodeLabel(node)
    );

    const parents =
      incoming.get(
        current
      ) || [];

    current =
      parents[0];
  }

  return path;
}


/* =========================================================
   COLLISION-SAFE RADIAL LAYOUT
   ========================================================= */

/*
 * This is the important part of the graph.
 *
 * Project:
 *      center
 *
 * Dependencies:
 *      one complete ring per depth
 *
 * Vulnerabilities:
 *      outer ring
 *
 * Unlike the previous version, children DO NOT inherit
 * almost the same angle as their parent.
 *
 * Every ring calculates its own radius based on the
 * number of nodes it contains.
 *
 * Therefore:
 *
 *   more nodes = larger ring
 *   fewer nodes = smaller ring
 *
 * This prevents cards from sitting on top of each other.
 */

function createRadialLayout(
  nodes,
  edges
) {
  if (
    !nodes ||
    nodes.length === 0
  ) {
    return [];
  }


  /* -------------------------------------------------------
     Layout dimensions
     ------------------------------------------------------- */

  const NODE_WIDTH = 260;
  const NODE_HEIGHT = 78;

  const CENTER_X = 0;
  const CENTER_Y = 0;

  /*
   * Minimum distance between the centres of two cards.
   *
   * This is deliberately larger than the actual card width
   * so that neighbouring nodes have breathing room.
   */
  const NODE_SPACING = 340;

  /*
   * Minimum distance from project to first dependency ring.
   */
  const FIRST_RING_RADIUS = 520;

  /*
   * Additional separation between dependency rings.
   */
  const RING_GAP = 430;

  /*
   * Vulnerability nodes sit outside the dependency rings.
   */
  const VULNERABILITY_GAP = 420;


  const positions =
    new Map();


  /* -------------------------------------------------------
     Separate node types
     ------------------------------------------------------- */

  const projectNodes =
    nodes.filter(
      (node) =>
        node.type === "project"
    );

  const dependencyNodes =
    nodes.filter(
      (node) =>
        node.type === "version"
    );

  const vulnerabilityNodes =
    nodes.filter(
      (node) =>
        node.type ===
        "vulnerability"
    );


  /* -------------------------------------------------------
     PROJECT
     ------------------------------------------------------- */

  projectNodes.forEach(
    (node) => {

      positions.set(
        node.id,
        {
          x:
            CENTER_X -
            NODE_WIDTH / 2,

          y:
            CENTER_Y -
            NODE_HEIGHT / 2,
        }
      );

    }
  );


  /* -------------------------------------------------------
     GROUP DEPENDENCIES BY DEPTH
     ------------------------------------------------------- */

  const depthGroups =
    new Map();

  dependencyNodes.forEach(
    (node) => {

      const depth =
        Math.max(
          1,
          Number(
            node.depth
          ) || 1
        );

      if (
        !depthGroups.has(
          depth
        )
      ) {
        depthGroups.set(
          depth,
          []
        );
      }

      depthGroups
        .get(depth)
        .push(node);

    }
  );


  const depths =
    [...depthGroups.keys()]
      .sort(
        (a, b) =>
          a - b
      );


  /* -------------------------------------------------------
     RADIUS CALCULATION
     ------------------------------------------------------- */

  /*
   * Calculate the radius required to distribute N nodes
   * around a complete circle without overlap.
   *
   * Circumference >= N × minimum node spacing
   *
   * radius >= circumference / 2π
   */

  function calculateRingRadius(
    nodeCount,
    depth
  ) {
    if (
      nodeCount <= 1
    ) {
      return (
        FIRST_RING_RADIUS +
        (depth - 1) *
          RING_GAP
      );
    }

    const requiredRadius =
      (
        nodeCount *
        NODE_SPACING
      ) /
      (
        2 *
        Math.PI
      );

    const normalRadius =
      FIRST_RING_RADIUS +
      (depth - 1) *
        RING_GAP;

    return Math.max(
      normalRadius,
      requiredRadius
    );
  }


  /* -------------------------------------------------------
     PLACE EACH DEPTH ON ITS OWN RING
     ------------------------------------------------------- */

  const dependencyAngles =
    new Map();


  depths.forEach(
    (depth) => {

      const group =
        depthGroups.get(
          depth
        ) || [];

      const count =
        group.length;

      const radius =
        calculateRingRadius(
          count,
          depth
        );


      /*
       * Rotate each ring slightly.
       *
       * This prevents all dependency nodes from sitting
       * directly above one another across different rings.
       */

      const rotation =
        depth % 2 === 0
          ? Math.PI /
            Math.max(
              2,
              count
            )
          : 0;


      /*
       * Start from the top of the circle.
       */

      const startAngle =
        -Math.PI / 2 +
        rotation;


      const angleStep =
        (
          Math.PI * 2
        ) /
        Math.max(
          1,
          count
        );


      group.forEach(
        (
          node,
          index
        ) => {

          const angle =
            startAngle +
            index *
              angleStep;


          dependencyAngles.set(
            node.id,
            angle
          );


          positions.set(
            node.id,
            {
              x:
                CENTER_X +
                Math.cos(angle) *
                  radius -
                NODE_WIDTH / 2,

              y:
                CENTER_Y +
                Math.sin(angle) *
                  radius -
                NODE_HEIGHT / 2,
            }
          );

        }
      );

    }
  );


  /* -------------------------------------------------------
     FIND MAX DEPENDENCY RADIUS
     ------------------------------------------------------- */

  let maxDependencyRadius =
    FIRST_RING_RADIUS;

  depths.forEach(
    (depth) => {

      const group =
        depthGroups.get(
          depth
        ) || [];

      maxDependencyRadius =
        Math.max(
          maxDependencyRadius,
          calculateRingRadius(
            group.length,
            depth
          )
        );

    }
  );


  /* -------------------------------------------------------
     VULNERABILITY RING
     ------------------------------------------------------- */

  /*
   * IMPORTANT:
   *
   * Vulnerabilities are NOT placed directly beside their
   * affected package anymore.
   *
   * That was causing the red cards to pile up.
   *
   * Instead, all vulnerability nodes live on their own
   * outer ring.
   *
   * Their edge still tells us exactly which dependency
   * they belong to.
   */

  if (
    vulnerabilityNodes.length >
    0
  ) {

    const vulnerabilityCount =
      vulnerabilityNodes.length;


    const vulnerabilityRadius =
      Math.max(
        maxDependencyRadius +
          VULNERABILITY_GAP,

        (
          vulnerabilityCount *
          NODE_SPACING
        ) /
        (
          2 *
          Math.PI
        )
      );


    const angleStep =
      (
        Math.PI * 2
      ) /
      Math.max(
        1,
        vulnerabilityCount
      );


    vulnerabilityNodes.forEach(
      (
        node,
        index
      ) => {

        /*
         * Rotate the vulnerability ring so that
         * vulnerability cards don't always line up
         * directly with dependency cards.
         */

        const angle =
          -Math.PI / 2 +
          index *
            angleStep +
          Math.PI /
            Math.max(
              3,
              vulnerabilityCount
            );


        positions.set(
          node.id,
          {
            x:
              CENTER_X +
              Math.cos(angle) *
                vulnerabilityRadius -
              NODE_WIDTH / 2,

            y:
              CENTER_Y +
              Math.sin(angle) *
                vulnerabilityRadius -
              NODE_HEIGHT / 2,
          }
        );

      }
    );
  }


  /* -------------------------------------------------------
     CREATE REACT FLOW NODES
     ------------------------------------------------------- */

  return nodes.map(
    (node) => {

      const position =
        positions.get(
          node.id
        ) || {
          x: 0,
          y: 0,
        };


      return {
        id: String(
          node.id
        ),

        type:
          "graphNode",

        position,

        data: {
          type:
            node.type,

          label:
            getNodeLabel(
              node
            ),

          coordinate:
            node.coordinate,

          depth:
            node.depth,

          vulnerable:
            node.vulnerable ===
            true,

          severity:
            node.severity,

          vulnerabilityId:
            node.vulnerabilityId,

          aliases:
            node.aliases,
        },
      };

    }
  );
}


/* =========================================================
   CUSTOM NODE
   ========================================================= */

function GraphNode({
  data,
}) {

  const {
    type,
    label,
    coordinate,
    depth,
    vulnerable,
    severity,
  } = data;


  const isProject =
    type === "project";

  const isVulnerability =
    type ===
    "vulnerability";


  let className =
    "graph-node-card";


  if (isProject) {
    className +=
      " graph-node-project";
  }


  if (isVulnerability) {
    className +=
      " graph-node-vulnerability";
  }


  if (
    vulnerable &&
    !isVulnerability
  ) {
    className +=
      " is-vulnerable";
  }


  return (
    <div
      className={
        className
      }
    >

      {!isProject && (
        <Handle
          type="target"
          position={
            Position.Left
          }
        />
      )}


      <div className="graph-node-icon">

        {isProject ? (

          <Target
            size={17}
          />

        ) : isVulnerability ? (

          <ShieldAlert
            size={17}
          />

        ) : (

          <Box
            size={17}
          />

        )}

      </div>


      <div className="graph-node-copy">

        <strong>
          {label}
        </strong>


        {isProject ? (

          <span>
            Application
          </span>

        ) : isVulnerability ? (

          <span>
            {severity ||
              "UNKNOWN"}{" "}
            vulnerability
          </span>

        ) : (

          <span>
            {vulnerable
              ? `${
                  severity ||
                  "Vulnerable"
                } · depth ${
                  depth ??
                  "—"
                }`
              : `Dependency · depth ${
                  depth ??
                  "—"
                }`}
          </span>

        )}

      </div>


      {!isVulnerability && (
        <Handle
          type="source"
          position={
            Position.Right
          }
        />
      )}

    </div>
  );
}


const nodeTypes = {
  graphNode:
    GraphNode,
};


/* =========================================================
   MAIN COMPONENT
   ========================================================= */

function DependencyGraph() {

  /* -------------------------------------------------------
     PROJECT / SCAN
     ------------------------------------------------------- */

  const [projects, setProjects] =
    useState([]);

  const [scans, setScans] =
    useState([]);

  const [
    selectedProjectId,
    setSelectedProjectId,
  ] = useState("");

  const [
    selectedScanId,
    setSelectedScanId,
  ] = useState("");


  /* -------------------------------------------------------
     GRAPH
     ------------------------------------------------------- */

  const [graphData, setGraphData] =
    useState(null);

  const [loading, setLoading] =
    useState(false);

  const [error, setError] =
    useState(null);


  /* -------------------------------------------------------
     FILTER
     ------------------------------------------------------- */

  const [activeFilter, setActiveFilter] =
    useState(
      FILTERS.ALL
    );

  const [searchQuery, setSearchQuery] =
    useState("");


  /* -------------------------------------------------------
     NODE INSPECTOR
     ------------------------------------------------------- */

  const [selectedNode, setSelectedNode] =
    useState(null);


  /* -------------------------------------------------------
     REACT FLOW
     ------------------------------------------------------- */

  const [
    reactFlowInstance,
    setReactFlowInstance,
  ] = useState(null);


  /* =======================================================
     LOAD PROJECTS
     ======================================================= */

  useEffect(() => {

    const loadProjects =
      async () => {

        try {

          const data =
            await getProjects();

          const projectList =
            Array.isArray(data)
              ? data
              : [];

          setProjects(
            projectList
          );


          if (
            projectList.length >
            0
          ) {

            setSelectedProjectId(
              projectList[0].id
            );

          }

        } catch (err) {

          console.error(
            "Failed to load projects:",
            err
          );

          setError(
            "Unable to load projects."
          );

        }
      };


    loadProjects();

  }, []);


  /* =======================================================
     LOAD SCANS
     ======================================================= */

  useEffect(() => {

    if (
      !selectedProjectId
    ) {
      return;
    }


    const loadScans =
      async () => {

        try {

          setLoading(true);
          setError(null);

          const data =
            await getScans(
              selectedProjectId
            );

          const scanList =
            Array.isArray(data)
              ? data
              : [];

          setScans(
            scanList
          );


          const completed =
            scanList.filter(
              (scan) =>
                String(
                  scan.status
                ).toUpperCase() ===
                "COMPLETED"
            );


          const preferred =
            completed.length >
            0
              ? completed[
                  completed.length -
                    1
                ]
              : scanList[
                  scanList.length -
                    1
                ];


          setSelectedScanId(
            preferred?.id ||
              ""
          );

        } catch (err) {

          console.error(
            "Failed to load scans:",
            err
          );

          setScans([]);
          setSelectedScanId("");

          setError(
            "Unable to load scans."
          );

        } finally {

          setLoading(false);

        }

      };


    loadScans();

  }, [
    selectedProjectId,
  ]);


  /* =======================================================
     LOAD GRAPH
     ======================================================= */

  useEffect(() => {

    if (
      !selectedProjectId ||
      !selectedScanId
    ) {
      return;
    }


    const loadGraph =
      async () => {

        try {

          setLoading(true);
          setError(null);
          setSelectedNode(null);


          const data =
            await getDependencyGraph(
              selectedProjectId,
              selectedScanId
            );


          console.log(
            "Dependency graph API:",
            data
          );


          setGraphData(
            data
          );

        } catch (err) {

          console.error(
            "Failed to load dependency graph:",
            err
          );

          setGraphData(null);

          setError(
            "Unable to load the dependency graph from Neo4j."
          );

        } finally {

          setLoading(false);

        }

      };


    loadGraph();

  }, [
    selectedProjectId,
    selectedScanId,
  ]);


  /* =======================================================
     RAW GRAPH
     ======================================================= */

  const rawNodes =
    useMemo(
      () =>
        Array.isArray(
          graphData?.nodes
        )
          ? graphData.nodes
          : [],
      [graphData]
    );


  const rawEdges =
    useMemo(
      () =>
        Array.isArray(
          graphData?.edges
        )
          ? graphData.edges
          : [],
      [graphData]
    );


  /* =======================================================
     FILTER
     ======================================================= */

  const filteredGraph =
    useMemo(() => {

      if (
        rawNodes.length ===
        0
      ) {

        return {
          nodes: [],
          edges: [],
        };

      }


      let allowedIds =
        new Set(
          rawNodes.map(
            (node) =>
              node.id
          )
        );


      /* ---------------------------------------------------
         DIRECT
         --------------------------------------------------- */

      if (
        activeFilter ===
        FILTERS.DIRECT
      ) {

        allowedIds =
          new Set(
            rawNodes
              .filter(
                (node) =>
                  node.type ===
                    "project" ||
                  (
                    node.type ===
                      "version" &&
                    Number(
                      node.depth
                    ) === 1
                  )
              )
              .map(
                (node) =>
                  node.id
              )
          );

      }


      /* ---------------------------------------------------
         TRANSITIVE
         --------------------------------------------------- */

      else if (
        activeFilter ===
        FILTERS.TRANSITIVE
      ) {

        allowedIds =
          new Set(
            rawNodes
              .filter(
                (node) =>
                  node.type ===
                    "project" ||
                  (
                    node.type ===
                      "version" &&
                    Number(
                      node.depth
                    ) > 1
                  )
              )
              .map(
                (node) =>
                  node.id
              )
          );

      }


      /* ---------------------------------------------------
         VULNERABLE
         --------------------------------------------------- */

      else if (
        activeFilter ===
        FILTERS.VULNERABLE
      ) {

        const vulnerableIds =
          rawNodes
            .filter(
              (node) =>
                node.vulnerable ===
                  true ||
                node.type ===
                  "vulnerability"
            )
            .map(
              (node) =>
                node.id
            );


        allowedIds =
          getAncestorNodeIds(
            vulnerableIds,
            rawNodes,
            rawEdges
          );

      }


      /* ---------------------------------------------------
         SEARCH
         --------------------------------------------------- */

      const query =
        searchQuery
          .trim()
          .toLowerCase();


      if (query) {

        const matchingIds =
          rawNodes
            .filter(
              (node) => {

                const values = [
                  node.label,
                  node.coordinate,
                  node.version,
                  node.vulnerabilityId,
                  ...(Array.isArray(
                    node.aliases
                  )
                    ? node.aliases
                    : []),
                ];


                return values.some(
                  (value) =>
                    safeString(
                      value
                    )
                      .toLowerCase()
                      .includes(
                        query
                      )
                );

              }
            )
            .map(
              (node) =>
                node.id
            );


        const searchIds =
          new Set(
            matchingIds
          );


        for (
          const edge of rawEdges
        ) {

          if (
            searchIds.has(
              edge.source
            )
          ) {

            searchIds.add(
              edge.target
            );

          }


          if (
            searchIds.has(
              edge.target
            )
          ) {

            searchIds.add(
              edge.source
            );

          }

        }


        allowedIds =
          new Set(
            [
              ...allowedIds,
            ].filter(
              (id) =>
                searchIds.has(
                  id
                )
            )
          );

      }


      const nodes =
        rawNodes.filter(
          (node) =>
            allowedIds.has(
              node.id
            )
        );


      const nodeIds =
        new Set(
          nodes.map(
            (node) =>
              node.id
          )
        );


      const edges =
        rawEdges.filter(
          (edge) =>
            nodeIds.has(
              edge.source
            ) &&
            nodeIds.has(
              edge.target
            )
        );


      return {
        nodes,
        edges,
      };

    }, [
      rawNodes,
      rawEdges,
      activeFilter,
      searchQuery,
    ]);


  /* =======================================================
     RADIAL LAYOUT
     ======================================================= */

  const layoutedNodes =
    useMemo(
      () =>
        createRadialLayout(
          filteredGraph.nodes,
          filteredGraph.edges
        ),
      [
        filteredGraph,
      ]
    );


  /* =======================================================
     EDGES
     ======================================================= */

  const layoutedEdges =
    useMemo(() => {

      const visibleIds =
        new Set(
          layoutedNodes.map(
            (node) =>
              node.id
          )
        );


      return filteredGraph.edges
        .filter(
          (edge) =>
            visibleIds.has(
              String(
                edge.source
              )
            ) &&
            visibleIds.has(
              String(
                edge.target
              )
            )
        )
        .map(
          (edge) => {

            const vulnerable =
              edge.type ===
              "AFFECTED_BY";


            return {
              id:
                String(
                  edge.id
                ),

              source:
                String(
                  edge.source
                ),

              target:
                String(
                  edge.target
                ),

              type:
                "smoothstep",

              animated:
                vulnerable,

              className:
                vulnerable
                  ? "graph-edge-vulnerable"
                  : "",

              style: {
                stroke:
                  vulnerable
                    ? "#ff5c69"
                    : "#3b4657",

                strokeWidth:
                  vulnerable
                    ? 2
                    : 1.2,
              },

              markerEnd: {
                type:
                  "arrowclosed",

                color:
                  vulnerable
                    ? "#ff5c69"
                    : "#3b4657",
              },

            };

          }
        );

    }, [
      filteredGraph.edges,
      layoutedNodes,
    ]);


  /* =======================================================
     FIT GRAPH
     ======================================================= */

  const fitGraph =
    useCallback(() => {

      if (
        !reactFlowInstance ||
        layoutedNodes.length ===
          0
      ) {
        return;
      }


      reactFlowInstance.fitView({
        padding: 0.18,
        duration: 650,
        minZoom: 0.05,
        maxZoom: 1.1,
      });

    }, [
      reactFlowInstance,
      layoutedNodes,
    ]);


  useEffect(() => {

    if (
      !reactFlowInstance ||
      layoutedNodes.length ===
        0
    ) {
      return;
    }


    const timer =
      setTimeout(
        () => {

          reactFlowInstance.fitView({
            padding: 0.18,
            duration: 550,
            minZoom: 0.05,
            maxZoom: 1.1,
          });

        },
        120
      );


    return () =>
      clearTimeout(
        timer
      );

  }, [
    reactFlowInstance,
    layoutedNodes,
    layoutedEdges,
  ]);


  /* =======================================================
     NODE CLICK
     ======================================================= */

  const handleNodeClick =
    useCallback(
      (_, flowNode) => {

        const original =
          rawNodes.find(
            (node) =>
              String(
                node.id
              ) ===
              String(
                flowNode.id
              )
          );


        if (!original) {
          return;
        }


        const dependencyPath =
          buildDependencyPath(
            original.id,
            rawNodes,
            rawEdges
          );


        setSelectedNode({
          ...original,
          dependencyPath,
        });

      },
      [
        rawNodes,
        rawEdges,
      ]
    );


  /* =======================================================
     COUNTS
     ======================================================= */

  const dependencyCount =
    Number(
      graphData?.dependencyCount
    ) || 0;


  const directDependencyCount =
    Number(
      graphData?.directDependencyCount
    ) || 0;


  const transitiveDependencyCount =
    Number(
      graphData?.transitiveDependencyCount
    ) || 0;


  const vulnerableDependencyCount =
    Number(
      graphData?.vulnerableDependencyCount
    ) || 0;


  const maxDepth =
    Number(
      graphData?.maxDepth
    ) || 0;


  const selectedProject =
    projects.find(
      (project) =>
        String(
          project.id
        ) ===
        String(
          selectedProjectId
        )
    );


  const selectedScan =
    scans.find(
      (scan) =>
        String(
          scan.id
        ) ===
        String(
          selectedScanId
        )
    );


  /* =======================================================
     RENDER
     ======================================================= */

  return (
    <div className="page-shell graph-page">

      {/* =================================================
          HEADER
          ================================================= */}

      <div className="graph-page-header">

        <div>

          <div className="eyebrow">
            DEPENDENCY INTELLIGENCE
          </div>

          <h1>
            Dependency Graph
          </h1>

          <p>
            Explore how this project reaches
            each dependency and where known
            vulnerabilities sit in the chain.
          </p>

        </div>


        <div className="graph-header-status">

          <span className="live-dot" />

          Neo4j-backed graph

        </div>

      </div>


      {/* =================================================
          CONTROLS
          ================================================= */}

      <div className="panel graph-control-panel">

        <div className="graph-selector-group">

          <label>
            Project
          </label>

          <select
            value={
              selectedProjectId
            }
            onChange={(event) => {

              setSelectedProjectId(
                event.target.value
              );

              setSelectedScanId(
                ""
              );

              setGraphData(
                null
              );

            }}
          >

            {projects.map(
              (project) => (

                <option
                  key={
                    project.id
                  }
                  value={
                    project.id
                  }
                >
                  {project.name}
                </option>

              )
            )}

          </select>

        </div>


        <div className="graph-selector-group">

          <label>
            Scan
          </label>

          <select
            value={
              selectedScanId
            }
            onChange={(event) =>
              setSelectedScanId(
                event.target.value
              )
            }
          >

            {scans.map(
              (scan) => (

                <option
                  key={
                    scan.id
                  }
                  value={
                    scan.id
                  }
                >
                  Scan{" "}
                  {String(
                    scan.id
                  ).slice(
                    0,
                    8
                  )}{" "}
                  ·{" "}
                  {scan.createdAt
                    ? new Date(
                        scan.createdAt
                      ).toLocaleString()
                    : "Unknown date"}
                </option>

              )
            )}

          </select>

        </div>


        <div className="graph-search-wrap">

          <Search
            size={16}
          />

          <input
            type="text"
            value={
              searchQuery
            }
            onChange={(event) =>
              setSearchQuery(
                event.target.value
              )
            }
            placeholder="Search package or vulnerability..."
          />

        </div>


        <button
          type="button"
          className="graph-fit-button"
          onClick={
            fitGraph
          }
          disabled={
            layoutedNodes.length ===
            0
          }
        >

          <ZoomIn
            size={15}
          />

          Fit graph

        </button>

      </div>


      {/* =================================================
          STATS
          ================================================= */}

      <div className="graph-stat-grid">

        <div className="graph-stat-card">

          <div className="graph-stat-icon">
            <Layers3
              size={17}
            />
          </div>

          <div>

            <span>
              Dependencies
            </span>

            <strong>
              {dependencyCount}
            </strong>

          </div>

        </div>


        <div className="graph-stat-card">

          <div className="graph-stat-icon">
            <GitBranch
              size={17}
            />
          </div>

          <div>

            <span>
              Direct
            </span>

            <strong>
              {directDependencyCount}
            </strong>

          </div>

        </div>


        <div className="graph-stat-card">

          <div className="graph-stat-icon">
            <GitBranch
              size={17}
            />
          </div>

          <div>

            <span>
              Transitive
            </span>

            <strong>
              {transitiveDependencyCount}
            </strong>

          </div>

        </div>


        <div className="graph-stat-card danger">

          <div className="graph-stat-icon">
            <AlertTriangle
              size={17}
            />
          </div>

          <div>

            <span>
              Vulnerable
            </span>

            <strong>
              {vulnerableDependencyCount}
            </strong>

          </div>

        </div>


        <div className="graph-stat-card">

          <div className="graph-stat-icon">
            <Target
              size={17}
            />
          </div>

          <div>

            <span>
              Max depth
            </span>

            <strong>
              {maxDepth}
            </strong>

          </div>

        </div>

      </div>


      {/* =================================================
          FILTER BAR
          ================================================= */}

      <div className="panel graph-toolbar">

        <div className="graph-filter-tabs">

          <button
            type="button"
            className={
              activeFilter ===
              FILTERS.ALL
                ? "active"
                : ""
            }
            onClick={() =>
              setActiveFilter(
                FILTERS.ALL
              )
            }
          >
            All dependencies
          </button>


          <button
            type="button"
            className={
              activeFilter ===
              FILTERS.DIRECT
                ? "active"
                : ""
            }
            onClick={() =>
              setActiveFilter(
                FILTERS.DIRECT
              )
            }
          >
            Direct
          </button>


          <button
            type="button"
            className={
              activeFilter ===
              FILTERS.TRANSITIVE
                ? "active"
                : ""
            }
            onClick={() =>
              setActiveFilter(
                FILTERS.TRANSITIVE
              )
            }
          >
            Transitive
          </button>


          <button
            type="button"
            className={
              activeFilter ===
              FILTERS.VULNERABLE
                ? "active"
                : ""
            }
            onClick={() =>
              setActiveFilter(
                FILTERS.VULNERABLE
              )
            }
          >
            Vulnerable
          </button>

        </div>


        <div className="graph-toolbar-meta">

          <span>
            {layoutedNodes.length} visible nodes
          </span>

          <span>
            {layoutedEdges.length} visible relationships
          </span>

          <span className="scan-chip">
            {selectedScan?.status ||
              "READY"}
          </span>

        </div>

      </div>


      {/* =================================================
          ERROR
          ================================================= */}

      {error && (

        <div className="panel graph-error">

          <AlertTriangle
            size={18}
          />

          <div>

            <strong>
              Graph unavailable
            </strong>

            <p>
              {error}
            </p>

          </div>

        </div>

      )}


      {/* =================================================
          GRAPH CANVAS
          ================================================= */}

      <div className="panel graph-workspace">

        {loading ? (

          <div className="graph-loading">

            <div className="loading-spinner" />

            <strong>
              Loading dependency graph
            </strong>

            <span>
              Reading dependency relationships
              from Neo4j...
            </span>

          </div>

        ) : layoutedNodes.length ===
          0 ? (

          <div className="graph-empty">

            <GitBranch
              size={30}
            />

            <strong>
              No graph data
            </strong>

            <span>
              Select a completed scan
              with dependency relationships.
            </span>

          </div>

        ) : (

          <ReactFlow
            nodes={
              layoutedNodes
            }
            edges={
              layoutedEdges
            }
            nodeTypes={
              nodeTypes
            }
            onInit={
              setReactFlowInstance
            }
            onNodeClick={
              handleNodeClick
            }
            fitView
            minZoom={
              0.04
            }
            maxZoom={
              1.5
            }
            nodesDraggable
            nodesConnectable={
              false
            }
            elementsSelectable
            panOnDrag
            zoomOnScroll
            zoomOnPinch
            zoomOnDoubleClick
            proOptions={{
              hideAttribution:
                true,
            }}
          >

            <Background
              gap={34}
              size={1}
              color="#171d27"
            />

            <Controls
              showInteractive={
                false
              }
            />

            <MiniMap
              nodeColor={(node) => {

                if (
                  node.data?.type ===
                  "project"
                ) {
                  return "#65a7ff";
                }

                if (
                  node.data?.type ===
                  "vulnerability"
                ) {
                  return "#ff5c69";
                }

                if (
                  node.data?.vulnerable
                ) {
                  return "#ff5c69";
                }

                return "#59687c";

              }}
              maskColor="rgba(8, 10, 15, 0.72)"
            />

          </ReactFlow>

        )}

      </div>


      {/* =================================================
          BOTTOM PANELS
          ================================================= */}

      <div className="graph-bottom-grid">

        <div className="panel graph-legend">

          <div className="panel-header-simple">

            <div className="eyebrow">
              GRAPH LEGEND
            </div>

            <h3>
              Dependency relationships
            </h3>

          </div>


          <div className="legend-items">

            <div className="legend-item">

              <span className="legend-dot project" />

              <span>
                Project
              </span>

            </div>


            <div className="legend-item">

              <span className="legend-dot dependency" />

              <span>
                Dependency version
              </span>

            </div>


            <div className="legend-item">

              <span className="legend-dot vulnerable" />

              <span>
                Vulnerable dependency
              </span>

            </div>


            <div className="legend-item">

              <span className="legend-dot vulnerability" />

              <span>
                Vulnerability
              </span>

            </div>

          </div>

        </div>


        <div className="panel graph-selection">

          <div className="panel-header-simple">

            <div className="eyebrow">
              NODE INSPECTOR
            </div>

            <h3>
              Selected component
            </h3>

          </div>


          {selectedNode ? (

            <div className="selected-node-content">

              <div
                className={`selected-node-icon ${
                  selectedNode.type ===
                  "vulnerability"
                    ? "vulnerability"
                    : ""
                }`}
              >

                {selectedNode.type ===
                "vulnerability" ? (

                  <ShieldAlert
                    size={18}
                  />

                ) : (

                  <Box
                    size={18}
                  />

                )}

              </div>


              <div className="selected-node-copy">

                <strong>
                  {getNodeLabel(
                    selectedNode
                  )}
                </strong>


                <span>
                  {selectedNode.type ===
                  "project"
                    ? "Application"
                    : selectedNode.type ===
                      "vulnerability"
                    ? "Security finding"
                    : selectedNode.coordinate}
                </span>


                <div className="selected-node-meta">

                  {selectedNode.type ===
                    "version" && (

                    <span>
                      Depth{" "}
                      {selectedNode.depth ??
                        "—"}
                    </span>

                  )}


                  {selectedNode.type ===
                    "version" && (

                    <span
                      className={
                        selectedNode.vulnerable
                          ? "danger-text"
                          : ""
                      }
                    >
                      {selectedNode.vulnerable
                        ? "Vulnerable"
                        : "Dependency"}
                    </span>

                  )}


                  {selectedNode.type ===
                    "vulnerability" && (

                    <span className="danger-text">
                      {selectedNode.severity ||
                        "UNKNOWN"}
                    </span>

                  )}

                </div>


                {selectedNode.type ===
                  "vulnerability" &&
                  selectedNode.vulnerabilityId && (

                    <button
                      type="button"
                      className="text-link-button"
                      onClick={() => {

                        window.location.href =
                          `/vulnerabilities?vulnerability=${encodeURIComponent(
                            selectedNode.vulnerabilityId
                          )}`;

                      }}
                    >
                      Investigate vulnerability →
                    </button>

                  )}

              </div>

            </div>

          ) : (

            <div className="selection-placeholder">

              <Target
                size={16}
              />

              Click a node in the graph
              to inspect it.

            </div>

          )}

        </div>

      </div>


      {/* =================================================
          CONTEXT
          ================================================= */}

      {selectedProject &&
        selectedScan && (

          <div
            style={{
              marginTop:
                "10px",

              color:
                "var(--muted-2)",

              fontSize:
                "10px",

              textAlign:
                "right",
            }}
          >

            {selectedProject.name}

            {" · "}

            Scan{" "}

            {String(
              selectedScan.id
            ).slice(
              0,
              8
            )}

          </div>

        )}

    </div>
  );
}


export default DependencyGraph;