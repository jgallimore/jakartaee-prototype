/********************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: (EPL-2.0 OR Apache-2.0)
 ********************************************************************************/

package org.eclipse.transformer.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.transformer.Transformer;
import org.eclipse.transformer.jakarta.JakartaTransformer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a Maven plugin which runs the Eclipse Transformer on build artifacts as part of the build.
 */
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM, defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true)
public class TransformMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "false", property = "transformer-plugin.invert", required = true)
    private Boolean invert;

    @Parameter(defaultValue = "true", property = "transformer-plugin.overwrite", required = true)
    private Boolean overwrite;

    @Parameter(property = "transformer-plugin.renames", defaultValue = "")
    private String rulesRenamesUri;

    @Parameter(property = "transformer-plugin.versions", defaultValue = "")
    private String rulesVersionUri;

    @Parameter(property = "transformer-plugin.bundles", defaultValue = "")
    private String rulesBundlesUri;

    @Parameter(property = "transformer-plugin.direct", defaultValue = "")
    private String rulesDirectUri;

    @Parameter(property = "transformer-plugin.xml", defaultValue = "")
    private String rulesXmlsUri;

    @Parameter(defaultValue = "")
    private String classifier;

    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;

    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Main execution point of the plugin. This looks at the attached artifacts, and runs the transformer on them.
     * @throws MojoFailureException Thrown if there is an error during plugin execution
     */
    public void execute() throws MojoFailureException {
        final Transformer transformer = getTransformer();

        final Artifact[] sourceArtifacts = getSourceArtifacts();
        for (final Artifact sourceArtifact : sourceArtifacts) {
            transform(transformer, sourceArtifact);
        }
    }

    /**
     * This runs the transformation process on the source artifact with the transformer provided.
     * The transformed artifact is attached to the project.
     * @param transformer The Transformer to use for the transformation
     * @param sourceArtifact The Artifact to transform
     * @throws MojoFailureException if plugin execution fails
     */
    public void transform(final Transformer transformer, final Artifact sourceArtifact) throws MojoFailureException {

		try {
			final String sourceClassifier = sourceArtifact.getClassifier();

			String targetClassifier = null;

        /*
         If a classifier is specified, the plugin will attach the transformed artifact as new artifact with the classifier
         to the project.

         If it isn't, we'll simply overwrite the artifact.
         */
			if (this.classifier != null) {
				targetClassifier =  (sourceClassifier == null || sourceClassifier.length() == 0) ?
					this.classifier : sourceClassifier + "-" + this.classifier;
			}

			File sourceFile;
			File targetFile;

			if (targetClassifier == null) {

				final File src = sourceArtifact.getFile();
				final File dest = new File(src.getParent(), "original-" + src.getName());
				Files.move(src.toPath(), dest.toPath());

				sourceFile = dest;
				targetFile = src;
			} else {
				sourceFile = sourceArtifact.getFile();
				targetFile = new File(outputDirectory,
					sourceArtifact.getArtifactId() + "-" +
						targetClassifier + "-" + sourceArtifact.getVersion() + "."
						+ sourceArtifact.getType());
			}

			final List<String> args = new ArrayList<>();
			args.add(sourceFile.getAbsolutePath());
			args.add(targetFile.getAbsolutePath());

			if (this.overwrite) {
				args.add("-o");
			}

			transformer.setArgs(args.toArray(new String[0]));
			int rc = transformer.run();

			if (rc != 0) {
				throw new MojoFailureException("Transformer failed with an error: " + Transformer.RC_DESCRIPTIONS[rc]);
			}

			if (targetClassifier != null) {
				projectHelper.attachArtifact(
					project,
					sourceArtifact.getType(),
					targetClassifier,
					targetFile
				);
			}
		} catch (IOException e) {
			throw new MojoFailureException("Transformer failed with an IO error: " + e.getMessage());
		}
	}

    /**
     * Builds a configured transformer for the specified source and target artifacts
     * @return A configured transformer
     */
    public Transformer getTransformer() {
        final Transformer transformer = new Transformer(System.out, System.err);
        transformer.setOptionDefaults(JakartaTransformer.class, getOptionDefaults());
        return transformer;
    }

    /**
     * Gets the source artifacts that should be transformed
     * @return an array to artifacts to be transformed
     */
    public Artifact[] getSourceArtifacts() {
        List<Artifact> artifactList = new ArrayList<Artifact>();
        if (project.getArtifact() != null && project.getArtifact().getFile() != null) {
            artifactList.add(project.getArtifact());
        }

        for (final Artifact attachedArtifact : project.getAttachedArtifacts()) {
            if (attachedArtifact.getFile() != null) {
                artifactList.add(attachedArtifact);
            }
        }

        return artifactList.toArray(new Artifact[0]);
    }

    private Map<Transformer.AppOption, String> getOptionDefaults() {
        HashMap<Transformer.AppOption, String> optionDefaults = new HashMap();
        optionDefaults.put(Transformer.AppOption.RULES_RENAMES, isEmpty(rulesRenamesUri) ? "jakarta-renames.properties" : rulesRenamesUri);
        optionDefaults.put(Transformer.AppOption.RULES_VERSIONS, isEmpty(rulesVersionUri) ? "jakarta-versions.properties" : rulesVersionUri);
        optionDefaults.put(Transformer.AppOption.RULES_BUNDLES, isEmpty(rulesBundlesUri) ? "jakarta-bundles.properties" : rulesBundlesUri);
        optionDefaults.put(Transformer.AppOption.RULES_DIRECT, isEmpty(rulesDirectUri) ? "jakarta-direct.properties" : rulesDirectUri);
        optionDefaults.put(Transformer.AppOption.RULES_MASTER_TEXT, isEmpty(rulesXmlsUri) ? "jakarta-text-master.properties" : rulesXmlsUri);
        return optionDefaults;
    }

    private boolean isEmpty(final String input) {
        return input == null || input.trim().length() == 0;
    }

    void setProject(MavenProject project) {
        this.project = project;
    }

    void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    MavenProjectHelper getProjectHelper() {
        return projectHelper;
    }

    void setProjectHelper(MavenProjectHelper projectHelper) {
        this.projectHelper = projectHelper;
    }

    void setOverwrite(Boolean overwrite) {
        this.overwrite = overwrite;
    }

    void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
}