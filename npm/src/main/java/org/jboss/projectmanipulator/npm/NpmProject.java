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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;
import org.jboss.projectmanipulator.core.ManipulationException;
import org.jboss.projectmanipulator.core.Project;

import java.io.File;
import java.io.IOException;

public class NpmProject implements Project {

    private File packageFile;
    private File packageLockFile;

    private JsonNode packageJson;
    private JsonNode packageLockJson;

    private ObjectMapper mapper;

    public NpmProject(File packageFile, File packageLockFile) {
        super();
        this.packageFile = packageFile;
        this.packageLockFile = packageLockFile;
        mapper = new ObjectMapper();
    }

    /**
     * Provides the JsonNode tree parsed from package.json referenced by packageFile.
     *
     * @return read JsonNode or null in case of file does not exist
     * @throws ManipulationException
     *             in case of an error when reading package file or the package file does not exist
     */
    public JsonNode getPackage() throws ManipulationException {
        if (packageJson == null) {
            if (packageFile.exists()) {
                String packageContents;
                try {
                    packageContents = FileUtils.readFileToString(packageFile, "utf-8");
                    packageJson = mapper.readTree(packageContents);
                } catch (IOException ex) {
                    throw new ManipulationException("Error reading file %s", ex, packageFile);
                }
            } else {
                throw new ManipulationException("Package file %s does not exist", packageFile.toString());
            }
        }
        return packageJson;
    }

    /**
     * Provides the JsonNode tree parsed from package-lock.json referenced by packageFile.
     *
     * @return read JsonNode or null in case of file does not exist
     * @throws ManipulationException
     *             in case of an error when reading package file
     */
    public JsonNode getPackageLock() throws ManipulationException {
        if (packageLockJson == null) {
            if (packageLockFile.exists()) {
                String packageLockContents;
                try {
                    packageLockContents = FileUtils.readFileToString(packageLockFile, "utf-8");
                    packageLockJson = mapper.readTree(packageLockContents);
                } catch (IOException ex) {
                    throw new ManipulationException("Error reading file %s", ex, packageLockFile);
                }
            }
        }
        return packageLockJson;
    }

    @Override
    public void update() throws ManipulationException {
        JsonFactory factory = new JsonFactory();

        if (packageJson != null) {
            JsonGenerator generator;
            try {
                generator = factory.createGenerator(packageFile, JsonEncoding.UTF8);
                mapper.writeTree(generator, packageJson);
            } catch (IOException ex) {
                throw new ManipulationException("Error writing the package file %s.", ex, packageFile);
            }
        }

        if (packageLockJson != null) {
            JsonGenerator generator;
            try {
                generator = factory.createGenerator(packageLockFile, JsonEncoding.UTF8);
                mapper.writeTree(generator, packageLockJson);
            } catch (IOException ex) {
                throw new ManipulationException("Error writing the package lock file %s.", ex, packageFile);
            }
        }
    }

}
