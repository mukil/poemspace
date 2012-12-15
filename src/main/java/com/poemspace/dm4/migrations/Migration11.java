package com.poemspace.dm4.migrations;

import java.util.Arrays;
import java.util.List;

import static com.poemspace.dm4.MigrationUtils.*;

import de.deepamehta.core.service.Migration;

public class Migration11 extends Migration {

    @Override
    public void run() {
        // add criteria type aggregations
        List<String> criteriaTypeUris = Arrays.asList(new String[] { "dm4.poemspace.project", //
                "dm4.poemspace.year", //
                "dm4.poemspace.affiliation", //
                "dm4.poemspace.workarea", //
                "dm4.poemspace.media" });
        addCriteriaAssocDefs(dms, "dm4.poemspace.campaign", criteriaTypeUris);
        addCriteriaAssocDefs(dms, "dm4.contacts.person", criteriaTypeUris);
        addCriteriaAssocDefs(dms, "dm4.contacts.institution", criteriaTypeUris);
    }
}
