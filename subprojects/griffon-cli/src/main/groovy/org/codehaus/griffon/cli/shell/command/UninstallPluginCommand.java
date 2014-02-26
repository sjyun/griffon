/*
 * Copyright 2012-2014 the original author or authors.
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

package org.codehaus.griffon.cli.shell.command;

import org.codehaus.griffon.cli.shell.AbstractGriffonCommand;
import org.codehaus.griffon.cli.shell.Argument;
import org.codehaus.griffon.cli.shell.Command;
import org.codehaus.griffon.cli.shell.Option;

/**
 * @author Andres Almiray
 * @since 0.9.5
 */
@Command(scope = "griffon",
        name = "uninstall-plugin",
        description = "Uninstalls a Griffon application plugin")
public class UninstallPluginCommand extends AbstractGriffonCommand {
    @Argument(index = 0,
            name = "name",
            description = "The name of the archetype to uninstall.",
            required = true)
    private String name;

    @Argument(index = 1,
            name = "version",
            description = "The version of the archetype to install.",
            required = false)
    private String version;

    @Option(name = "--dry-run",
        description = "Display the uninstall plan but do not execute it.",
        required = false)
    private boolean dryRun;

    @Option(name = "--force",
        description = "Forces dependent plugins to be uninstalled, even if required by other plugins.",
        required = false)
    private boolean force;

    @Option(name = "--framework",
            description = "Uninstalls the plugin only if installed as a framework plugin.",
            required = false)
    private boolean framework;
}