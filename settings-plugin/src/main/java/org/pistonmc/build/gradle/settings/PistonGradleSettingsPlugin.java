package org.pistonmc.build.gradle.settings;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.ExtensionAware;
import org.jetbrains.annotations.NotNull;
import org.pistonmc.build.gradle.settings.api.PistonGradleSettingsExtension;

public class PistonGradleSettingsPlugin implements Plugin<ExtensionAware> {
    @Override
    public void apply(@NotNull ExtensionAware target) {
        var ext = target.getExtensions().create(PistonGradleSettingsExtension.EXTENSION_NAME, PistonGradleSettingsExtension.class);
        if (target instanceof Settings settings) {
            settings.getGradle().rootProject(rootProject -> {
                rootProject.getPluginManager().apply(PistonGradleSettingsPlugin.class);
                rootProject.getExtensions().getByType(PistonGradleSettingsExtension.class).from(ext);
            });
        } else if (target instanceof Project project) {
            var rootProject = project.getRootProject();
            if (project != rootProject) {
                var rootExt = rootProject.getExtensions().findByType(PistonGradleSettingsExtension.class);
                if (rootExt != null) {
                    ext.from(rootExt);
                }
            }
        } else throw new UnsupportedOperationException("Unsupported target: " + target);
    }
}