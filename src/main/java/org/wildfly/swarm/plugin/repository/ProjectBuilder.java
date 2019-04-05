package org.wildfly.swarm.plugin.repository;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ken Finnigan
 * @author Michal Szynkiewicz
 */
public class ProjectBuilder {

    public ProjectBuilder(MavenProject project,
                          Log log,
                          File template,
                          String[] skipBomDependencies) {
        this.project = project;
        this.log = log;
        this.template = template;
        this.skipBomDependencies = skipBomDependencies;
    }

    public File generateProject(File... bomFiles) throws MojoExecutionException {
        log.info("Generating pom.xml from BOMs: " + Arrays.toString(bomFiles));

        try {
            // Initialize the project dir first
            String targetDir = this.project.getBuild().getDirectory();
            File projectDir = new File(targetDir, projectName(bomFiles));
            if (!projectDir.mkdirs()) {
                throw new MojoFailureException("Unable to create " + projectDir.getCanonicalPath());
            }

            // There is no pom.xml specified - generate one from BOM
            if (!template.exists()) {
                throw new MojoFailureException("Unable to proceed without a `template` specified for generating a project pom.xml.");
            }

            File pomFile = BomProjectBuilder.generateProject(projectDir, template, project, bomFiles, skipBomDependencies);
            if (!pomFile.exists()) {
                throw new MojoFailureException("Failed to create project pom.xml");
            }
            log.info("Generated pom.xml: " + pomFile.getAbsolutePath());

            return projectDir;
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected String projectName(File[] bomFiles) {
        return "generated-project_" + Stream.of(bomFiles)
                .map(bomFile -> PomUtils.extract(bomFile, "/project/artifactId/text()").asString())
                .collect(Collectors.joining("_"));
    }

    private final File template;
    private final String[] skipBomDependencies;
    private final MavenProject project;
    private final Log log;

}
