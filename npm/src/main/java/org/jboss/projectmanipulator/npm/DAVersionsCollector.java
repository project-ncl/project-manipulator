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
package org.jboss.projectmanipulator.npm;

import com.github.zafarkhaja.semver.Version;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.apache.commons.codec.binary.Base32;
import org.commonjava.atlas.npm.ident.ref.NpmPackageRef;
import org.jboss.projectmanipulator.core.ManipulationException;
import org.jboss.projectmanipulator.core.ManipulationSession;
import org.jboss.projectmanipulator.core.Manipulator;
import org.jboss.projectmanipulator.core.Project;
import org.jboss.projectmanipulator.npm.da.DAException;
import org.jboss.projectmanipulator.npm.da.ReportMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * This Manipulator collects data from an external service while doesn't do any manipulations to the project
 * definitions. It makes a REST call to loadRemoteOverrides the NVs to align the project version and dependencies to. It
 * will prepopulate package versions into the state under key {@link #AVAILABLE_VERSIONS} in case the restURL was
 * provided and versionOverride and versionSuffixOverride values is empty.
 */
public class DAVersionsCollector implements Manipulator<NpmResult> {

    public static final String AVAILABLE_VERSIONS = "availableVersions";

    private static final Random RANDOM = new Random();

    private static final Base32 CODEC = new Base32();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ManipulationSession<NpmResult> session;

    private String restURL;

    private String repositoryGroup;

    private String versionIncrementalSuffix;

    @Override
    public boolean init(final ManipulationSession<NpmResult> session) throws ManipulationException {
        this.session = session;
        Properties userProps = session.getUserProps();
        String versionOverride = userProps.getProperty("versionOverride");
        if (isEmpty(versionOverride)) {
            String versionSuffixOverride = userProps.getProperty("versionSuffixOverride");
            if (isEmpty(versionSuffixOverride)) {
                restURL = userProps.getProperty("restURL");
                if (!isEmpty(restURL)) {
                    repositoryGroup = userProps.getProperty("repositoryGroup");
                    if (!isEmpty(repositoryGroup)) {
                        versionIncrementalSuffix = userProps.getProperty("versionIncrementalSuffix");
                        if (!isEmpty(versionIncrementalSuffix)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Prescans the Project to build up a list of Project names.
     */
    private void collect(final List<Project> projects) throws ManipulationException {
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> availableVersions = session.getState(AVAILABLE_VERSIONS, Map.class);
        if (availableVersions == null) {
            availableVersions = new HashMap<>();
            session.setState(AVAILABLE_VERSIONS, availableVersions);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<NpmPackage> npmPackages = (List) projects;
        final ArrayList<NpmPackageRef> npmPackageRefs = new ArrayList<>();

        for (final NpmPackage npmPackage : npmPackages) {
            npmPackageRefs.add(new NpmPackageRef(npmPackage.getName(), Version.valueOf(npmPackage.getVersion())));
        }

        final ArrayList<NpmPackageRef> restParam = new ArrayList<>(npmPackageRefs);

        // Call the REST to populate the result.
        logger.debug("Passing {} projects following into the REST client api {} ", restParam.size(), restParam);
        logger.info("Calling REST client...");
        long start = System.nanoTime();
        Map<NpmPackageRef, List<String>> restResult = null;

        try {
            restResult = getAvailableVersions(restParam);
        } finally {
            printFinishTime(start, (restResult != null));
        }
        logger.info("DA Client returned {} ", restResult);

        parseVersions(availableVersions, npmPackageRefs, restResult);
    }

    private void init(ObjectMapper objectMapper) {
        // According to https://github.com/Mashape/unirest-java the default connection timeout is 10000
        // and the default socketTimeout is 60000.
        // We have increased the first to 30 seconds and the second to 10 minutes.
        Unirest.setTimeouts(30000, 600000);
        Unirest.setObjectMapper(objectMapper);
    }

    @SuppressWarnings("unchecked")
    private Map<NpmPackageRef, List<String>> getAvailableVersions(ArrayList<NpmPackageRef> restParam) {
        ReportMapper mapper = new ReportMapper(repositoryGroup, versionIncrementalSuffix);
        init(mapper);

        @SuppressWarnings("rawtypes")
        HttpResponse<Map> r;
        int status;
        Map<NpmPackageRef, List<String>> result;

        String url = restURL + (restURL.endsWith("/") ? "" : '/');
        if (!url.endsWith("v-1/")) {
            url += "v-1/";
        }
        url += "reports/lookup/npm";

        try {
            r = Unirest.post(url)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Log-Context", getHeaderContext())
                    .body(restParam)
                    .asObject(Map.class);

            status = r.getStatus();
            if (status == SC_OK) {
                result = r.getBody();
            } else {
                throw new DAException(
                        "Received response status " + status + " with message: " + mapper.getErrorString());
            }
        } catch (UnirestException ex) {
            throw new DAException(
                    "An exception was thrown when requesting the NPM versions for " + restParam + " with message "
                            + ex.getMessage(),
                    ex);
        }

        return result;
    }

    private String getHeaderContext() {
        String headerContext;

        if (isNotEmpty(MDC.get("LOG-CONTEXT"))) {
            headerContext = MDC.get("LOG-CONTEXT");
        } else {
            // If we have no MDC PME has been used as the entry point. Dummy one up for DA.
            byte[] randomBytes = new byte[20];
            RANDOM.nextBytes(randomBytes);
            headerContext = "npman-" + CODEC.encodeAsString(randomBytes);
        }

        return headerContext;
    }

    /**
     * Parse the rest result for the project names and store them in versioning state for use there by incremental
     * suffix calculation.
     */
    private void parseVersions(
            Map<String, Set<String>> state,
            ArrayList<NpmPackageRef> npmPackageRefs,
            Map<NpmPackageRef, List<String>> restResult) throws ManipulationException {
        for (final NpmPackageRef p : npmPackageRefs) {
            if (restResult.containsKey(p)) {
                Set<String> versions;
                if (state.containsKey(p.getName())) {
                    versions = state.get(p.getName());
                } else {
                    versions = new HashSet<>();
                    state.put(p.getName(), versions);
                }
                versions.addAll(restResult.get(p));
            }
        }
        logger.debug("Added the following NpmProjectRef:Version from REST call into {} {}", AVAILABLE_VERSIONS, state);

        // TODO sort out blacklisted artifacts (for dependencies)
    }

    /**
     * No-op in this case - any changes, if configured, would happen in Versioning or Dependency Manipulators.
     */
    @Override
    public Set<Project> applyChanges(final List<Project> projects) throws ManipulationException {
        collect(projects);

        return Collections.emptySet();
    }

    private void printFinishTime(long start, boolean finished) {
        long finish = System.nanoTime();
        long minutes = TimeUnit.NANOSECONDS.toMinutes(finish - start);
        long seconds = TimeUnit.NANOSECONDS.toSeconds(finish - start) - (minutes * 60);
        logger.info(
                "REST client finished {}... (took {} min, {} sec, {} millisec)",
                (finished ? "successfully" : "with failures"),
                minutes,
                seconds,
                (TimeUnit.NANOSECONDS.toMillis(finish - start) - (minutes * 60 * 1000) - (seconds * 1000)));
    }

    @Override
    public Collection<Class<? extends Manipulator<NpmResult>>> getDependencies() {
        return Collections.emptyList();
    }

}
