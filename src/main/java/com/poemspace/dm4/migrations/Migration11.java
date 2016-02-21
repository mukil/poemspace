package com.poemspace.dm4.migrations;

import static com.poemspace.dm4.MigrationUtils.*;
import de.deepamehta.core.service.Migration;

public class Migration11 extends Migration {

    @Override
    public void run() {
        // add criteria type aggregations
        String[] criteriaTypeUris = { "dm4.poemspace.project", //
                "dm4.poemspace.year", //
                "dm4.poemspace.affiliation", //
                "dm4.poemspace.press", //
                "dm4.poemspace.education", //
                "dm4.poemspace.public" };
        addCriteriaAssocDefs(dms, "dm4.poemspace.campaign", criteriaTypeUris);
        addCriteriaAssocDefs(dms, "dm4.contacts.person", criteriaTypeUris);
        addCriteriaAssocDefs(dms, "dm4.contacts.institution", criteriaTypeUris);

    }
}
