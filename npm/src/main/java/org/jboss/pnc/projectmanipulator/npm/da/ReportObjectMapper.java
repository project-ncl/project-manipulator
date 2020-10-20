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

import com.mashape.unirest.http.ObjectMapper;

import org.commonjava.atlas.npm.ident.ref.NpmPackageRef;

import java.util.List;
import java.util.Map;

public interface ReportObjectMapper extends ObjectMapper {

    @Override
    Map<NpmPackageRef, List<String>> readValue(String s);

    @Override
    String writeValue(Object value);

    String getErrorString();

}
