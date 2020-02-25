/**
 *
 */
package org.jboss.projectmanipulator.npm;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Test class for {@link NpmPackageVersionManipulator}.
 *
 * @author pkocandr
 */
public class NpmPackageVersionManipulatorTest {

    /**
     * Tests that the {@link NpmPackageVersionManipulator#findHighestIncrementalNum(String, Set)} returns 0 when the available
     * version set is empty.
     */
    @Test
    public void findHighestIncrementalNumWithNoAvailableVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator("jboss", 5, null, null);

        Set<String> availableSet = new HashSet<>();
        int highestNum = manipulator.findHighestIncrementalNum("1.0.0", availableSet);

        assertThat(0, is(highestNum));
    }

    /**
     * Tests that the {@link NpmPackageVersionManipulator#findHighestIncrementalNum(String, Set)} returns 0 when the available
     * version set is not empty, but contains only versions with non-matching major-minor-patch combination or with matching one,
     * but non-matching suffix.
     */
    @Test
    public void findHighestIncrementalNumWithNoMatchingAvailableVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator("jboss", 5, null, null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.1-jboss-1");
        availableSet.add("1.0.0-ncl-1");
        int highestNum = manipulator.findHighestIncrementalNum("1.0.0", availableSet);

        assertThat(0, is(highestNum));
    }

    /**
     * Tests that the {@link NpmPackageVersionManipulator#findHighestIncrementalNum(String, Set)} returns 2 when the available
     * version set is not empty and contains a version with non-matching suffix and 2 matching ones with 00001 and 00002 after it.
     */
    @Test
    public void findHighestIncrementalNumWithMatchingAvailableVersion() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator("jboss", 5, null, null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.0-jboss-00001");
        availableSet.add("1.0.0-jboss-00002");
        availableSet.add("1.0.0-ncl-1");
        int highestNum = manipulator.findHighestIncrementalNum("1.0.0", availableSet);

        assertThat(2, is(highestNum));
    }

    /**
     * Tests generation of new version when there is no pre-existing suffixed version. For version 1.0.0 it expects to get
     * 1.0.0-jboss-00001.
     */
    @Test
    public void generateNewVersionWhenNoSuffixedExists() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator("jboss", 5, null, null);

        String newVersion = manipulator.generateNewVersion("1.0.0", Collections.emptySet());

        assertThat("1.0.0-jboss-00001", is(newVersion));
    }

    /**
     * Tests generation of new version when there are pre-existing suffixed versions - jboss-1 and jboss-00002. For version 1.0.0 it
     * expects to get 1.0.0-jboss-00003.
     */
    @Test
    public void generateNewVersionWithAvailableSuffixedVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator("jboss", 5, null, null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.0-jboss-1");
        availableSet.add("1.0.0-jboss-00002");
        String newVersion = manipulator.generateNewVersion("1.0.0", availableSet);

        assertThat("1.0.0-jboss-00003", is(newVersion));
    }

    /**
     * Tests generation of new version for version that already contains the suffix when there are pre-existing suffixed versions -
     * jboss-1 and jboss-00002. It expects that the original suffix will be removed and replaced by the generated one based on the
     * highest available version, so for version 1.0.0-jboss-00004 it expects to get 1.0.0-jboss-00003.
     */
    @Test
    public void generateNewVersionForSuffixedVersionWithPreexistingSuffixedVersions() {
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator("jboss", 5, null, null);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.0-jboss-1");
        availableSet.add("1.0.0-jboss-00002");
        String newVersion = manipulator.generateNewVersion("1.0.0-jboss-00004", availableSet);

        assertThat("1.0.0-jboss-00003", is(newVersion));
    }

    /**
     * Tests getting new version when the complete version is set to an override. It sets also versionSuffix and
     * versionSuffixOverride, which should be both ignored and the result should be only the overriden version.
     */
    @Test
    public void getNewVersionOverride() {
        String versionOverride = "2.0.0-foo-001";
        NpmPackageVersionManipulator manipulator = new NpmPackageVersionManipulator(
                "jboss",
                5,
                "bar-02",
                versionOverride);

        Set<String> availableSet = new HashSet<>();
        availableSet.add("1.0.0-jboss-1");
        String newVersion = manipulator.getNewVersion("1.0.0", availableSet);

        assertThat(versionOverride, is(newVersion));
    }
}
