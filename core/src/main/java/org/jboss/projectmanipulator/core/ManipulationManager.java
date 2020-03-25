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
package org.jboss.projectmanipulator.core;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class ManipulationManager<R> {

    public static final String MANIPULATION_DISABLE_PROPERTY = "manipulation.disable";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private List<Manipulator<R>> manipulators;

    public void init(ManipulationSession<R> session) throws ManipulationException {
        this.manipulators = session.getActiveManipulators();
    }

    public void scanAndApply(final ManipulationSession<R> session) throws ManipulationException {
        // get project files list
        List<Project> projects = session.getProjects();

        Properties userProps = session.getUserProps();
        boolean manipulationDisabled = false;
        if (userProps.containsKey(MANIPULATION_DISABLE_PROPERTY)) {
            String manipDisableValue = userProps.getProperty(MANIPULATION_DISABLE_PROPERTY);
            manipulationDisabled = StringUtils.isEmpty(manipDisableValue) || "true".equalsIgnoreCase(manipDisableValue);
        }

        Set<Project> changed;
        if (manipulationDisabled) {
            logger.info("All manipulation disabled by property {}.", MANIPULATION_DISABLE_PROPERTY);
            changed = Collections.emptySet();
        } else {
            // apply manipulators on project files list and get changed ones back
            changed = applyManipulations(projects);

            // process the changes
            processChanges(changed, session);
        }

        session.writeResult();
    }

    private void processChanges(Set<Project> changed, ManipulationSession<R> session) throws ManipulationException {
        for (Project project : changed) {
            project.update();
        }
    }

    /**
     * Applies any modifications on projects. It resolves the order of manipulators being performed by checking
     * dependencies' status.
     *
     * @param projects the list of Projects to apply the changes to
     * @return a set of the changed projects, never {@code null}
     * @throws ManipulationException if an error occurs.
     */
    private Set<Project> applyManipulations(final List<Project> projects) throws ManipulationException {
        final Set<Project> changed = new HashSet<>();
        final Set<Manipulator<R>> todo = new HashSet<>(manipulators);
        int done;
        do {
            done = 0;
            for (Manipulator<R> manipulator : new ArrayList<>(todo)) {
                if (dependenciesDone(manipulator, todo)) {
                    final Set<Project> mChanged = manipulator.applyChanges(projects);

                    if (mChanged != null) {
                        changed.addAll(mChanged);
                    }

                    todo.remove(manipulator);
                    done++;
                }
            }
        } while (!todo.isEmpty() && done > 0);

        if (!todo.isEmpty()) {
            throw new ManipulationException(
                    "A dependency cycle has been found, so manipulation cannot be finished. "
                            + "Remaining manipulators are: %s",
                    null,
                    todo);
        }

        if (changed.isEmpty()) {
            logger.info("No changes.");
        }

        return changed;
    }

    /**
     * Checks if dependencies of the provided manipulator are done, so the manipulation can be performed.
     *
     * @param manipulator checked manipulator
     * @param todo manipulators to be done
     * @return true if none of the dependencies is in the todo set, otherwise false
     */
    private boolean dependenciesDone(Manipulator<R> manipulator, Set<Manipulator<R>> todo) {
        for (Class<? extends Manipulator<R>> dependencyClass : manipulator.getDependencies()) {
            for (Manipulator<R> todoMan : todo) {
                if (dependencyClass.isAssignableFrom(todoMan.getClass())) {
                    return false;
                }
            }
        }
        return true;
    }
}
