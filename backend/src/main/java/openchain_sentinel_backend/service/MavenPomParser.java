package openchain_sentinel_backend.service;

import openchain_sentinel_backend.model.MavenProject;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class MavenPomParser {

    public MavenProject parse(
            String pomXml,
            String pomPath) {

        try {

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(true);

            Document document =
                    factory
                            .newDocumentBuilder()
                            .parse(
                                    new ByteArrayInputStream(
                                            pomXml.getBytes(
                                                    StandardCharsets.UTF_8
                                            )
                                    )
                            );

            document.getDocumentElement()
                    .normalize();

            MavenProject project =
                    new MavenProject();

            project.setPomPath(pomPath);

            project.setGroupId(
                    getChildText(
                            document.getDocumentElement(),
                            "groupId"
                    )
            );

            project.setArtifactId(
                    getChildText(
                            document.getDocumentElement(),
                            "artifactId"
                    )
            );

            project.setVersion(
                    getChildText(
                            document.getDocumentElement(),
                            "version"
                    )
            );

            project.setPackaging(
                    getChildText(
                            document.getDocumentElement(),
                            "packaging"
                    )
            );

            Node parentNode =
                    getDirectChild(
                            document.getDocumentElement(),
                            "parent"
                    );

            if (parentNode != null) {

                project.setParentGroupId(
                        getChildText(
                                parentNode,
                                "groupId"
                        )
                );

                project.setParentArtifactId(
                        getChildText(
                                parentNode,
                                "artifactId"
                        )
                );

                project.setParentVersion(
                        getChildText(
                                parentNode,
                                "version"
                        )
                );
            }

            Node modulesNode =
                    getDirectChild(
                            document.getDocumentElement(),
                            "modules"
                    );

            if (modulesNode != null) {

                NodeList children =
                        modulesNode.getChildNodes();

                List<String> modules =
                        new ArrayList<>();

                for (int i = 0;
                     i < children.getLength();
                     i++) {

                    Node child =
                            children.item(i);

                    if (isElement(
                            child,
                            "module")) {

                        modules.add(
                                child.getTextContent()
                                        .trim()
                        );
                    }
                }

                project.setModules(modules);
            }

            return project;

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to parse pom.xml: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private String getChildText(
            Node parent,
            String childName) {

        Node child =
                getDirectChild(
                        parent,
                        childName
                );

        if (child == null) {
            return null;
        }

        String value =
                child.getTextContent().trim();

        return value.isEmpty()
                ? null
                : value;
    }

    private Node getDirectChild(
            Node parent,
            String childName) {

        NodeList children =
                parent.getChildNodes();

        for (int i = 0;
             i < children.getLength();
             i++) {

            Node child =
                    children.item(i);

            if (isElement(
                    child,
                    childName)) {

                return child;
            }
        }

        return null;
    }

    private boolean isElement(
            Node node,
            String expectedName) {

        return node.getNodeType()
                == Node.ELEMENT_NODE
                && node.getLocalName() != null
                && node.getLocalName()
                        .equals(expectedName);
    }
} 