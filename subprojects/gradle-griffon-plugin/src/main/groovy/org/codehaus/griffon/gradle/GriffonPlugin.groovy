/*
 * Copyright 2008-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.griffon.gradle

import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.BuildAdapter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.Copy
import org.gradle.tooling.BuildException

/**
 * @author Andres Almiray
 */
class GriffonPlugin implements Plugin<Project> {
    private static final boolean MACOSX = System.getProperty('os.name').contains('Mac OS')

    @Override
    void apply(Project project) {
        GriffonExtension extension = project.extensions.create('griffon', GriffonExtension, project)

        applyDefaultPlugins(project)

        // enable jcenter by default
        project.repositories.jcenter()
        // enable griffon-plugins @ bintray
        project.repositories.maven { url 'http://dl.bintray.com/griffon/griffon-plugins' }

        applyDefaultDependencies(project)

        String sourceSetName = project.plugins.hasPlugin('groovy') ? 'groovy' : 'java'

        configureDefaultSourceSets(project, 'java')
        if (sourceSetName != 'java') configureDefaultSourceSets(project, sourceSetName)
        adjustJavadocClasspath(project, sourceSetName)
        adjustIdeClasspaths(project)
        createDefaultDirectoryStructure(project, 'java')
        if (sourceSetName != 'java') createDefaultDirectoryStructure(project, sourceSetName)

        registerBuildListener(project, extension)
    }

    private void applyDefaultDependencies(final Project project) {
        // add compile time configurations
        project.configurations.maybeCreate('compileOnly')
        project.configurations.maybeCreate('testCompileOnly')
        project.configurations.maybeCreate('griffon').visible = false

        // wire up classpaths with compile time dependencies
        project.sourceSets.main.compileClasspath += [project.configurations.compileOnly]
        project.sourceSets.test.compileClasspath += [project.configurations.testCompileOnly]

        GriffonPluginResolutionStrategy.applyTo(project)
    }

    private void applyDefaultPlugins(Project project) {
        project.apply(plugin: 'idea')
        project.apply(plugin: 'java')
        if (!project.hasProperty('griffonPlugin') || !project.griffonPlugin) {
            project.apply(plugin: 'application')
        }
    }

    private void adjustJavadocClasspath(Project project, String sourceSetName) {
        project.javadoc.classpath += [project.configurations.compileOnly]
        if (sourceSetName == 'groovy') {
            project.groovydoc.classpath += [project.configurations.compileOnly]
        }
    }

    private void adjustIdeClasspaths(Project project) {
        // adjust Eclipse classpath, but only if EclipsePlugin is applied
        project.plugins.withId('eclipse') {
            project.eclipse.classpath.plusConfigurations += [project.configurations.compileOnly]
            project.eclipse.classpath.plusConfigurations += [project.configurations.testCompileOnly]
        }

        // adjust IntelliJ classpath
        project.idea.module.scopes.PROVIDED.plus += [project.configurations.compileOnly]
        project.idea.module.scopes.PROVIDED.plus += [project.configurations.testCompileOnly]
    }

    private void configureDefaultSourceSets(Project project, String sourceSetName) {
        // configure default source directories
        project.sourceSets.main[sourceSetName].srcDirs = [
            'griffon-app/conf',
            'griffon-app/controllers',
            'griffon-app/models',
            'griffon-app/views',
            'griffon-app/services',
            'griffon-app/lifecycle',
            'src/main/' + sourceSetName
        ]
        // configure default resource directories
        project.sourceSets.main.resources.srcDirs = [
            'griffon-app/resources',
            'griffon-app/i18n',
            'src/main/resources'
        ]
    }

    private static String resolveApplicationName(Project project) {
        if (project.hasProperty('applicationName')) {
            return project.applicationName
        }
        return project.name
    }

    private void processMainResources(Project project, GriffonExtension extension) {
        project.processResources {
            from(project.sourceSets.main.resources.srcDirs) {
                exclude '**/*.properties'
                exclude '**/*.groovy'
                exclude '**/*.html'
                exclude '**/*.xml'
                exclude '**/*.txt'
            }
            from(project.sourceSets.main.resources.srcDirs) {
                include '**/*.properties'
                include '**/*.groovy'
                include '**/*.html'
                include '**/*.xml'
                include '**/*.txt'
                filter(ReplaceTokens, tokens: [
                    'application.name'   : resolveApplicationName(project),
                    'application.version': project.version,
                    'griffon.version'    : extension.version
                ])
            }
        }
    }

    private void processTestResources(Project project, GriffonExtension extension) {
        project.processTestResources {
            from(project.sourceSets.test.resources.srcDirs) {
                exclude '**/*.properties'
                exclude '**/*.groovy'
                exclude '**/*.html'
                exclude '**/*.xml'
                exclude '**/*.txt'
            }
            from(project.sourceSets.test.resources.srcDirs) {
                include '**/*.properties'
                include '**/*.groovy'
                include '**/*.html'
                include '**/*.xml'
                include '**/*.txt'
                filter(ReplaceTokens, tokens: [
                    'application.name'   : resolveApplicationName(project),
                    'application.version': project.version,
                    'griffon.version'    : extension.version
                ])
            }
        }
    }

    private void configureApplicationSettings(Project project, GriffonExtension extension) {
        Task createDistributionFiles = project.tasks.create(name: 'createDistributionFiles', type: Copy, group: 'Application') {
            destinationDir = project.file("${project.buildDir}/assemble/distribution")
            from(project.file('src/media')) {
                into 'resources'
                include '*.icns', '*.ico'
            }
            from(project.file('.')) {
                include 'README*', 'INSTALL*', 'LICENSE*'
            }
        }
        project.applicationDistribution.from(createDistributionFiles)

        if (MACOSX) {
            List jvmArgs = project.applicationDefaultJvmArgs
            if (!(jvmArgs.find { it.startsWith('-Xdock:name=') })) {
                jvmArgs << "-Xdock:name=${resolveApplicationName(project)}"
            }
            if (!(jvmArgs.find { it.startsWith('-Xdock:icon=') })) {
                jvmArgs << ('-Xdock:icon=$APP_HOME/resources/' + extension.applicationIconName)
            }

            Task runTask = project.tasks.findByName('run')
            jvmArgs = (project.applicationDefaultJvmArgs + runTask.jvmArgs).unique()
            if (!(jvmArgs.find { it.startsWith('-Xdock:name=') })) {
                jvmArgs << "-Xdock:name=${resolveApplicationName(project)}"
            }

            String iconElem = jvmArgs.find { it.startsWith('-Xdock:icon=$APP_HOME/resources') }
            jvmArgs -= iconElem
            if (!(jvmArgs.find { it.startsWith('-Xdock:icon=') })) {
                File iconFile = project.file("src/media/${extension.applicationIconName}")
                if (!iconFile.exists()) iconFile = project.file('src/media/griffon.icns')
                jvmArgs << "-Xdock:icon=${iconFile.canonicalPath}"
            }
            runTask.jvmArgs = jvmArgs
        }
    }

    private void createDefaultDirectoryStructure(Project project, String sourceSetName) {
        project.gradle.taskGraph.whenReady {
            def createIfNotExists = { File dir ->
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }
            project.sourceSets.main[sourceSetName].srcDirs.each(createIfNotExists)
            project.sourceSets.test[sourceSetName].srcDirs.each(createIfNotExists)
            project.sourceSets.main.resources.srcDirs.each(createIfNotExists)
            project.sourceSets.test.resources.srcDirs.each(createIfNotExists)
        }
    }

    private void validateToolkit(Project project, GriffonExtension extension) {
        if (extension.toolkit) {
            if (!GriffonExtension.TOOLKIT_NAMES.contains(extension.toolkit)) {
                throw new BuildException("The value of griffon.toolkit can only be one of ${GriffonExtension.TOOLKIT_NAMES.join(',')}",
                    new IllegalStateException(extension.toolkit))
            }
        }
    }

    private void registerBuildListener(
        final Project project, final GriffonExtension extension) {
        project.gradle.addBuildListener(new BuildAdapter() {
            @Override
            void projectsEvaluated(Gradle gradle) {
                project.repositories.mavenLocal()

                // add default dependencies
                appendDependency('core')
                appendDependency('core-compile')
                appendDependency('core-test')

                validateToolkit(project, extension)
                project.plugins.withId('application') { plugin ->
                    configureApplicationSettings(project, extension)
                }

                boolean groovyDependenciesEnabled = extension.includeGroovyDependencies?.toBoolean() ||
                    (project.plugins.hasPlugin('groovy') && extension.includeGroovyDependencies == null)

                if (extension.toolkit) {
                    appendDependency(extension.toolkit)
                    maybeIncludeGroovyDependency(groovyDependenciesEnabled, extension.toolkit + '-groovy')
                }
                maybeIncludeGroovyDependency(groovyDependenciesEnabled, 'groovy')
                maybeIncludeGroovyDependency(groovyDependenciesEnabled, 'groovy-compile')

                processMainResources(project, extension)
                processTestResources(project, extension)

                project.plugins.withId('org.kordamp.gradle.stats') { plugin ->
                    Task statsTask = project.tasks.findByName('stats')
                    statsTask.paths += [
                        model     : [name: 'Models', path: 'griffon-app/models'],
                        view      : [name: 'Views', path: 'griffon-app/views'],
                        controller: [name: 'Controllers', path: 'griffon-app/controllers'],
                        service   : [name: 'Services', path: 'griffon-app/services'],
                        config    : [name: 'Configuration', path: 'griffon-app/conf'],
                        lifecycle : [name: 'Lifecycle', path: 'griffon-app/lifecycle']
                    ]
                }

                // update Griffon environment settings
                project.gradle.taskGraph.whenReady {
                    if (project.gradle.taskGraph.hasTask(':startScripts')) {
                        if (project.hasProperty('griffonEnv')) {
                            project.applicationDefaultJvmArgs << "-Dgriffon.env=${project.griffonEnv}"
                        } else {
                            project.applicationDefaultJvmArgs << '-Dgriffon.env=prod'
                        }
                    } else {
                        Task runTask = project.tasks.findByName('run')
                        if( runTask != null) {
                            if (project.hasProperty('griffonEnv')) {
                                project.applicationDefaultJvmArgs << "-Dgriffon.env=${project.griffonEnv}"
                                runTask.jvmArgs << "-Dgriffon.env=${project.griffonEnv}"
                            } else {
                                runTask.jvmArgs << '-Dgriffon.env=dev'
                            }
                        }
                    }
                }
            }

            private boolean maybeIncludeGroovyDependency(boolean groovyDependenciesEnabled, String artifactId) {
                if (artifactId =~ /groovy/) {
                    if (groovyDependenciesEnabled) {
                        appendDependency(artifactId)
                    }
                    return true
                }
                false
            }

            private void appendDependency(String artifactId) {
                if (artifactId.endsWith('-compile')) {
                    project.dependencies.add('compileOnly', ['org.codehaus.griffon', 'griffon-' + artifactId, extension.version].join(':'))
                    project.dependencies.add('testCompileOnly', ['org.codehaus.griffon', 'griffon-' + artifactId, extension.version].join(':'))
                } else if (artifactId.endsWith('-test')) {
                    project.dependencies.add('testCompile', ['org.codehaus.griffon', 'griffon-' + artifactId, extension.version].join(':'))
                } else {
                    project.dependencies.add('compile', ['org.codehaus.griffon', 'griffon-' + artifactId, extension.version].join(':'))
                }
            }
        })
    }
}
