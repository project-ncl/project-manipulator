/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018-2020 Red Hat, Inc., and individual contributors
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
package org.jboss.pnc.projectmanipulator.npm.da;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

public class SuffixedNVSchema {

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public String versionSuffix;

    /**
     * @deprecated this was used to pass in the Indy repository group, which is not used in DA any more, instead you
     *             have to fill in the mode
     */
    @Deprecated
    public String repositoryGroup;

    /**
     * The mode telling DA which packages to select.
     */
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public String mode;

    public List<Map<String, Object>> packages;

    public SuffixedNVSchema() {
    }

    public SuffixedNVSchema(
            String repositoryGroup,
            String versionSuffix,
            String mode,
            List<Map<String, Object>> packages) {
        this.repositoryGroup = repositoryGroup;
        this.versionSuffix = versionSuffix;
        this.mode = mode;
        this.packages = packages;
    }

    @Override
    public String toString() {
        return "RepositoryGroup '" + repositoryGroup + "' :: versionSuffix '" + versionSuffix + "' :: mode '" + mode
                + "' :: packages " + packages;
    }
}
