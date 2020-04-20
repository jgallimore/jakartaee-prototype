package org.eclipse.transformer.maven;

import com.ibm.ws.jakarta.transformer.JakartaTransformer;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.transformer.Transformer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM, defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true)
public class TransformMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(defaultValue = "false", readonly = true, required = true)
    private Boolean invert;

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

    @Parameter(defaultValue = "transformed")
    private String classifier;

    @Parameter(defaultValue = "${project.build.directory}", required = true )
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.finalName}", readonly = true )
    private String finalName;

    @Component
    private MavenProjectHelper projectHelper;


    public void execute() throws MojoExecutionException {
        final File sourceFile = project.getArtifact().getFile();
        final File targetFile = getTargetFile();

        final Transformer transformer = new Transformer(System.out, System.err);
        transformer.setOptionDefaults(JakartaTransformer.class, getOptionDefaults());
        transformer.setArgs(new String[]{sourceFile.getAbsolutePath()});

        int rc = transformer.run();
        projectHelper.attachArtifact(project, project.getArtifact().getType(), classifier, targetFile);
    }

    protected File getTargetFile() {
        final StringBuilder fileName = new StringBuilder(finalName);
        fileName.append("-").append(classifier);

        fileName.append(".").append(this.project.getArtifact().getType());
        return new File(outputDirectory, fileName.toString());
    }

    private Map<Transformer.AppOption, String> getOptionDefaults() {
        HashMap<Transformer.AppOption, String> optionDefaults = new HashMap();
        optionDefaults.put(Transformer.AppOption.RULES_RENAMES, isEmpty(rulesRenamesUri) ? "jakarta-renames.properties" : rulesRenamesUri);
        optionDefaults.put(Transformer.AppOption.RULES_VERSIONS, isEmpty(rulesVersionUri) ? "jakarta-versions.properties" : rulesVersionUri);
        optionDefaults.put(Transformer.AppOption.RULES_BUNDLES, isEmpty(rulesBundlesUri) ? "jakarta-bundles.properties" : rulesBundlesUri);
        optionDefaults.put(Transformer.AppOption.RULES_DIRECT, isEmpty(rulesDirectUri) ? "jakarta-direct.properties" : rulesDirectUri);
        optionDefaults.put(Transformer.AppOption.RULES_MASTER_XML, isEmpty(rulesXmlsUri) ? "jakarta-xml.properties" : rulesXmlsUri);
        return optionDefaults;
    }

    private boolean isEmpty(final String input) {
        return input == null || input.trim().length() == 0;
    }
}
