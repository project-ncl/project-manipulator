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

import org.jboss.projectmanipulator.core.ManipulationException;
import org.jboss.projectmanipulator.core.Project;

public interface NpmPackage extends Project {

    /**
     * Retrieves package name from loaded package file.
     *
     * @return retrieved name
     * @throws ManipulationException in case the project cannot be loaded or does not have correct structure
     */
    String getName() throws ManipulationException;

    /**
     * Retrieves package version from loaded package file.
     *
     * @return retrieved version
     * @throws ManipulationException in case the project cannot be loaded or does not have correct structure
     */
    String getVersion() throws ManipulationException;

    /**
     * Updates package version in the loaded package file and package-lock file.
     *
     * @param version the version to be set
     * @throws ManipulationException in case the project cannot be loaded or does not have correct structure
     */
    void setVersion(String version) throws ManipulationException;

}
