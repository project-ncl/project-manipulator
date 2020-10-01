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
