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
package org.jboss.projectmanipulator.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ManipulationManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private List<Manipulator> manipulators;

    public void init(ManipulationSession session) {
        this.manipulators = session.getActiveManipulators();
    }

    public void scanAndApply(final ManipulationSession session) throws ManipulationException {
        // get project files list
        List<Project> projects = session.getProjects();

        // apply manipulators on project files list and get changed ones back
        Set<Project> changed = applyManipulations(projects);

        // process the changes
        processChanges(changed, session);
    }

    private void processChanges(Set<Project> changed, ManipulationSession session) throws ManipulationException {
        for (Project project : changed) {
            project.update();
        }
    }

    /**
     * After projects are scanned for modifications, apply any modifications. This method performs the following:
     * <ul>
     * <li>read the raw models (uninherited, with only a bare minimum interpolation) from disk to escape any
     * interpretation happening during project-building</li>
     * <li>apply any manipulations</li>
     * </ul>
     *
     * @param projects
     *            the list of Projects to apply the changes to
     * @return a set of the changed projects, never {@code null}
     * @throws ManipulationException
     *             if an error occurs.
     */
    private Set<Project> applyManipulations(final List<Project> projects) throws ManipulationException {
        final Set<Project> changed = new HashSet<>();
        for (final Manipulator manipulator : manipulators) {
            final Set<Project> mChanged = manipulator.applyChanges(projects);

            if (mChanged != null) {
                changed.addAll(mChanged);
            }
        }

        if (changed.isEmpty()) {
            logger.info("No changes.");
        }

        return changed;
    }
}
