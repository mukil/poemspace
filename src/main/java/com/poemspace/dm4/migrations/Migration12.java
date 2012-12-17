package com.poemspace.dm4.migrations;

import java.util.Arrays;
import java.util.List;

import static com.poemspace.dm4.MigrationUtils.*;

import de.deepamehta.core.service.Migration;

public class Migration12 extends Migration {

    @Override
    public void run() {
        // add criteria type aggregations
        List<String> criteriaTypeUris = Arrays.asList(new String[] { "dm4.poemspace.project", //
                "dm4.poemspace.year", //
                "dm4.poemspace.affiliation", //
                "dm4.poemspace.press", //
                "dm4.poemspace.education", //
                "dm4.poemspace.public" });
        addCriteriaAssocDefs(dms, "dm4.poemspace.campaign", criteriaTypeUris);
        addCriteriaAssocDefs(dms, "dm4.contacts.person", criteriaTypeUris);
        addCriteriaAssocDefs(dms, "dm4.contacts.institution", criteriaTypeUris);

        changeIcon(dms, "dm4.webclient.search", "/com.poemspace.dm4-poemspace/images/basket.png");
    }
}
