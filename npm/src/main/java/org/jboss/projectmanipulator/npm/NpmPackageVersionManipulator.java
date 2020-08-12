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

import org.jboss.projectmanipulator.core.ManipulationException;
import org.jboss.projectmanipulator.core.ManipulationSession;
import org.jboss.projectmanipulator.core.Manipulator;
import org.jboss.projectmanipulator.core.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNumeric;
import static org.apache.commons.lang.StringUtils.leftPad;
import static org.apache.commons.lang.StringUtils.substring;
import static org.apache.commons.lang.math.NumberUtils.createInteger;

/**
 * {@link Manipulator} implementation that can modify an NPM project's version with either static or calculated,
 * incremental version qualifier.
 */
public class NpmPackageVersionManipulator implements Manipulator<NpmResult> {

    /**
     * The separator that's used between the original version and the generated or provided suffix in HYPHENED
     * versioning strategy.
     */
    public static final String HYPHENED_SEPARATOR = "-";

    /**
     * The separator that's used between the version numbers and the pre-release suffix in SEMVER versioning strategy.
     */
    public static final String SEMVER_PRERELEASE_SEPARATOR = "-";

    /**
     * The separator that's used between the pre-release suffix and the generated build number in SEMVER versioning
     * strategy.
     */
    public static final String SEMVER_PRERELEASE_BUILDNUM_SEPARATOR = ".";

    /**
     * Version pattern matching the semantic versioning format for a final release version.
     *
     * <p>
     * Groups are:
     * <ul>
     * <li>1 - major version</li>
     * <li>2 - minor version</li>
     * <li>3 - patch version</li>
     * </ul>
     */
    private static final String SEMVER_FINAL_PATTERN = "^(\\d+)\\.(\\d+)\\.(\\d+)$";

    /**
     * Version pattern matching the semantic versioning format for apre-release version.
     *
     * <p>
     * Groups are:
     * <ul>
     * <li>1 - major version</li>
     * <li>2 - minor version</li>
     * <li>3 - patch version</li>
     * <li>4 - optional pre-release identifier</li>
     * <li>5 - optional pre-release build number</li>
     * </ul>
     */
    private static final String SEMVER_PRERELEASE_PATTERN = "^(\\d+)\\.(\\d+)\\.(\\d+)(?:-(\\w+)\\.(\\d+))?$";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String versionIncrementalSuffix;

    private Integer versionIncrementalSuffixPadding;

    private String repositoryGroup;

    private String restUrl;

    private String versionSuffixOverride;

    private String versionOverride;

    private VersioningStrategy versioningStrategy;

    private ManipulationSession<NpmResult> session;

    private List<Class<? extends Manipulator<NpmResult>>> manipulatorDependencies;

    /**
     * The default public constructor.
     */
    public NpmPackageVersionManipulator() {
    }

    /**
     * Constructor used in tests.
     *
     * @param versionIncrementalSuffix initial value
     * @param versionIncrementalSuffixPadding initial value
     * @param versionSuffixOverride initial value
     * @param versionOverride initial value
     */
    NpmPackageVersionManipulator(
            VersioningStrategy versioningStrategy,
            String versionIncrementalSuffix,
            Integer versionIncrementalSuffixPadding,
            String versionSuffixOverride,
            String versionOverride) {
        this.versioningStrategy = versioningStrategy;
        this.versionIncrementalSuffix = versionIncrementalSuffix;
        this.versionIncrementalSuffixPadding = versionIncrementalSuffixPadding;
        this.versionSuffixOverride = versionSuffixOverride;
        this.versionOverride = versionOverride;
        this.session = new NpmManipulationSession();
    }

    @Override
    public boolean init(final ManipulationSession<NpmResult> session) {
        this.session = session;

        Properties userProps = session.getUserProps();
        if (userProps != null) {
            String verStrategyStr = userProps.getProperty("versioningStrategy");
            if (!isEmpty(verStrategyStr)) {
                try {
                    versioningStrategy = VersioningStrategy.valueOf(verStrategyStr);
                } catch (RuntimeException ex) {
                    logger.error(
                            "Unknown versioning strategy: \'{}\'. Only version override will be applied.",
                            verStrategyStr);
                    logger.debug("Error was: " + ex.getMessage(), ex);
                }
            }
            versionOverride = userProps.getProperty("versionOverride");
            versionSuffixOverride = userProps.getProperty("versionSuffixOverride");
            restUrl = userProps.getProperty("restURL");
            repositoryGroup = userProps.getProperty("repositoryGroup");
            versionIncrementalSuffix = userProps.getProperty("versionIncrementalSuffix");
            try {
                versionIncrementalSuffixPadding = createInteger(
                        userProps.getProperty("versionIncrementalSuffixPadding"));
            } catch (NumberFormatException ex) {
                logger.warn(
                        "Invalid number provided in versionIncrementalSuffixPadding \'"
                                + userProps.getProperty("versionIncrementalSuffixPadding") + "\'. Using 1.");
                logger.debug("Error was: " + ex.getMessage(), ex);
            }
            if (versionIncrementalSuffixPadding == null) {
                versionIncrementalSuffixPadding = 1;
            }

            return !isEmpty(versionOverride) || !isEmpty(versionSuffixOverride)
                    || !isEmpty(restUrl) && !isEmpty(repositoryGroup) && !isEmpty(versionIncrementalSuffix);
        }

        return false;
    }

    @Override
    public Set<Project> applyChanges(final List<Project> projects) throws ManipulationException {
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> availableVersions = session
                .getState(DAVersionsCollector.AVAILABLE_VERSIONS, Map.class);

        Set<Project> changed = new HashSet<>();
        for (Project project : projects) {
            if (project instanceof NpmPackage) {
                NpmPackage npmPackage = (NpmPackage) project;

                String origVersion = npmPackage.getVersion();
                Set<String> availablePkgVersions = availableVersions == null ? Collections.emptySet()
                        : availableVersions.get(npmPackage.getName());
                String newVersion = getNewVersion(origVersion, availablePkgVersions);

                if (!origVersion.equals(newVersion)) {
                    npmPackage.setVersion(newVersion);
                    session.getResult().setVersion(newVersion);
                    changed.add(npmPackage);
                }
            } else {
                throw new ManipulationException(
                        "Manipulation failed, because project type %s is not supported by NPM manipulation.",
                        project.getClass());
            }
        }
        return changed;
    }

    String getNewVersion(String origVersion, Set<String> availablePkgVersions) {
        String newVersion = null;
        if (isEmpty(versionOverride)) {
            if (isEmpty(versionSuffixOverride)) {
                if (versioningStrategy != null) {
                    switch (versioningStrategy) {
                        case HYPHENED:
                            newVersion = generateNewHyphenedVersion(origVersion, availablePkgVersions);
                            break;

                        case SEMVER:
                            newVersion = generateNewSemverVersion(origVersion, availablePkgVersions);
                            break;
                    }
                } else {
                    logger.warn("No version strategy defined and no override provided. Skipping version manipulation.");
                }
            } else {
                newVersion = origVersion + HYPHENED_SEPARATOR + versionSuffixOverride;
            }
        } else {
            newVersion = versionOverride;
        }
        return newVersion;
    }

    /**
     * Generates a new hyphened version based on original version, available version for the given package, suffix
     * string and suffix padding settings.
     *
     * @param origVersion the original version
     * @param availablePkgVersions set of available versions of this package
     * @return the generated version
     */
    String generateNewHyphenedVersion(String origVersion, Set<String> availablePkgVersions) {
        String bareVersion = origVersion;
        if (origVersion.matches(".+" + HYPHENED_SEPARATOR + versionIncrementalSuffix + HYPHENED_SEPARATOR + "\\d+")) {
            bareVersion = origVersion
                    .replaceFirst(HYPHENED_SEPARATOR + versionIncrementalSuffix + HYPHENED_SEPARATOR + "\\d+", "");
        }
        int suffixNum = findHighestIncrementalNum(bareVersion, availablePkgVersions) + 1;
        String versionSuffix = versionIncrementalSuffix + HYPHENED_SEPARATOR
                + leftPad(String.valueOf(suffixNum), versionIncrementalSuffixPadding, '0');
        String newVersion = bareVersion + HYPHENED_SEPARATOR + versionSuffix;
        return newVersion;
    }

    int findHighestIncrementalNum(String origVersion, Set<String> availableVersions) {
        String lookupPrefix = origVersion + HYPHENED_SEPARATOR + versionIncrementalSuffix;
        int highestFoundNum = 0;
        for (String version : availableVersions) {
            if (version.startsWith(lookupPrefix)) {
                String incrementalPart = substring(version, lookupPrefix.length() + 1);
                if (isNumeric(incrementalPart)) {
                    int foundNum = Integer.valueOf(incrementalPart);
                    if (foundNum > highestFoundNum) {
                        highestFoundNum = foundNum;
                    }
                }
            }
        }
        return highestFoundNum;
    }

    /**
     * Generates a new SemVer version based on original version, available version for the given package, suffix string
     * and suffix padding settings.
     *
     * @param origVersion the original version
     * @param availablePkgVersions set of available versions of this package
     * @return the generated version
     */
    String generateNewSemverVersion(String origVersion, Set<String> availablePkgVersions) {
        int patchNum = findHighestFinalPatchVersion(origVersion, availablePkgVersions) + 1;
        String incrementedVersion = origVersion.replaceFirst(SEMVER_PRERELEASE_PATTERN, "$1.$2." + patchNum);

        String newVersion;
        if (!isEmpty(versionIncrementalSuffix)) {
            int suffixNum = findHighestIncrementalNum(incrementedVersion, availablePkgVersions) + 1;
            String versionSuffix = versionIncrementalSuffix + SEMVER_PRERELEASE_BUILDNUM_SEPARATOR + suffixNum;
            newVersion = incrementedVersion + HYPHENED_SEPARATOR + versionSuffix;
        } else {
            newVersion = incrementedVersion;
        }
        return newVersion;
    }

    /**
     * Finds the highest available final patch version.
     *
     * @param origVersion the orig version
     * @param availableVersions the set of available versions
     * @return found highest patch version, -1 if no patch with major.minor version is available
     */
    int findHighestFinalPatchVersion(String origVersion, Set<String> availableVersions) {
        String lookupPrefix = origVersion.replaceFirst(SEMVER_PRERELEASE_PATTERN, "$1.$2.");
        int highestPatch = -1;
        String highestVersion = null;
        Pattern finalSemverPattern = Pattern.compile(SEMVER_FINAL_PATTERN);
        for (String version : availableVersions) {
            if (version.startsWith(lookupPrefix)) {
                Matcher matcher = finalSemverPattern.matcher(version);
                if (matcher.matches()) {
                    String incrementalPart = matcher.group(3);
                    int foundNum = Integer.valueOf(incrementalPart);
                    if (foundNum > highestPatch) {
                        highestPatch = foundNum;
                        highestVersion = version;
                    }
                }
            }
        }
        return highestPatch;
    }

    @Override
    public Collection<Class<? extends Manipulator<NpmResult>>> getManipulatorDependencies() {
        if (manipulatorDependencies == null) {
            manipulatorDependencies = new ArrayList<>();
            if (isEmpty(versionOverride) && isEmpty(versionSuffixOverride) && !isEmpty(restUrl)
                    && !isEmpty(repositoryGroup)) {
                manipulatorDependencies.add(DAVersionsCollector.class);
            }
        }
        return manipulatorDependencies;
    }

    /** Enum of available versioning strategies. */
    public enum VersioningStrategy {
        /**
         * Adds hyphen-separated suffix to the current version and adds optionally zero-padded highest existing number +
         * 1.
         */
        HYPHENED,
        /**
         * Semantic versioning compliant strategy. Rewrites patch number to the highest existing number + 1, in case of
         * prerelease also adds prerelease suffix and increments the build number to the highest existing number + 1.
         */
        SEMVER,
        /**
         * Overriding strategy indicating that either the whole version or the suffix is provided by caller, so no
         * automatic version incrementing is done.
         */
        OVERRIDE;
    }

}
