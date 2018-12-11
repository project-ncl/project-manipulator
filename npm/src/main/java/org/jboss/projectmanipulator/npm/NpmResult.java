package org.jboss.projectmanipulator.npm;

/**
 * Represents result of manipulation.
 */
public class NpmResult {

    /** Resulting package name. */
    private String name;

    /** Resulting package version. */
    private String version;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
