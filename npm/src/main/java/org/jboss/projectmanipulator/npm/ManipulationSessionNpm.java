/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.projectmanipulator.npm;

import org.jboss.projectmanipulator.core.ManipulationSession;
import org.jboss.projectmanipulator.core.Manipulator;
import org.jboss.projectmanipulator.core.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ManipulationSessionNpm implements ManipulationSession {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Properties properties;
    private File pkg;
    private Properties userProps;

    public ManipulationSessionNpm(File pkg, Properties properties, Properties userProps) {
        this.pkg = pkg;
        this.properties = properties;
        this.userProps = userProps;
    }

    @Override
    public List<Manipulator> getActiveManipulators() {
        List<Manipulator> result = new ArrayList<>(1);
        // result.add(new NpmProjectVersionManipulator());
        return result;
    }

    @Override
    public List<Project> getProjects() {
        List<Project> result = new ArrayList<>();

        File packageLock = null;
        File packageFile = null;
        String packageDirPath = null;

        if (pkg.exists()) {
            if (pkg.isFile()) {
                if ("package.json".equals(pkg.getName())) {
                    packageFile = pkg;
                } else if ("npm-shrinkwrap.json".equals(pkg.getName())) {
                    packageLock = pkg;
                } else if ("package-lock.json".equals(pkg.getName())) {
                    packageLock = pkg;
                }

                packageDirPath = pkg.getParent();
            } else {
                packageDirPath = pkg.getPath();
            }

            if (packageFile == null) {
                packageFile = new File(packageDirPath + File.separator + "package.json");
            }
            if (packageLock == null) {
                packageFile = new File(packageDirPath + File.separator + "package-lock.json");
            }

            result.add(new NpmProject(packageFile, packageLock));
        } else {
            logger.error("Given package path %s does not exist.", pkg);
        }

        return result;
    }

    public Properties getProperties() {
        return properties;
    }

    public File getTarget() {
        return pkg;
    }

    public Properties getUserProps() {
        return userProps;
    }

}
