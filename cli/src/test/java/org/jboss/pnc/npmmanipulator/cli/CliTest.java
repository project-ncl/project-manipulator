package org.jboss.pnc.npmmanipulator.cli;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.org.webcompere.systemstubs.SystemStubs.tapSystemErrAndOut;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CliTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testResolveScripts() throws IOException {
        File target = folder.newFile();
        Cli cli = new Cli();

        String[] toTest = new String[2];
        toTest[0] = "file://" + target.getAbsolutePath();
        toTest[1] = "https://raw.githubusercontent.com/project-ncl/project-manipulator/refs/heads/master/README.md";

        List<File> result = cli.resolveScripts(toTest);
        assertEquals(result.get(0), target);
        assertTrue(
                FileUtils.readFileToString(result.get(1), Charset.defaultCharset())
                        .contains(
                                "Project Manipulator is a tool used to manipulate project definition files. Current focus is on NPM support"));
    }

    @Test
    public void testExecuteScript() throws Exception {
        File target = folder.newFile();
        //noinspection ResultOfMethodCallIgnored
        target.setExecutable(true);
        FileUtils.writeStringToFile(
                target,
                "#!/bin/sh\necho \"### HELLO!\"\n",
                Charset.defaultCharset());
        Cli cli = new Cli();
        String text = tapSystemErrAndOut(() -> cli.executeScript(target));
        assertTrue(text.contains("### HELLO!"));
    }

    @Test
    public void testExecuteScriptWithFailures() throws Exception {
        File target = folder.newFile();
        //noinspection ResultOfMethodCallIgnored
        target.setExecutable(true);
        FileUtils.writeStringToFile(
                target,
                "#!/bin/sh\necho \"### HELLO!\"\nexit 1\n",
                Charset.defaultCharset());
        Cli cli = new Cli();
        String text = tapSystemErrAndOut(() -> {
            try {
                cli.executeScript(target);
                fail("No exception thrown");
            } catch (RuntimeException e) {
                assertTrue(e.getMessage().contains("Problem executing script"));
            }
        });
        assertTrue(text.contains("### HELLO!"));
    }
}