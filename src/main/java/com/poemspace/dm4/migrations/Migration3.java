package com.poemspace.dm4.migrations;

import static com.poemspace.dm4.MigrationUtils.*;
import de.deepamehta.core.service.Migration;

public class Migration3 extends Migration {

    @Override
    public void run() {
        // configure individual icons
        changeIcon(dm4, "dm4.contacts.institution",
                "/com.poemspace.dm4-poemspace/images/birdhouse.png");
        changeIcon(dm4, "dm4.contacts.person", "/com.poemspace.dm4-poemspace/images/bird.png");
        changeIcon(dm4, "dm4.notes.note", "/com.poemspace.dm4-poemspace/images/pen.png");
        changeIcon(dm4, "dm4.webclient.search", "/com.poemspace.dm4-poemspace/images/basket.png");

        // add criteria type aggregations
        String[] criteriaTypeUris = { "dm4.poemspace.art", //
                "dm4.poemspace.bezirk", //
                "dm4.poemspace.gattung", //
                "dm4.poemspace.kiez" };
        addCriteriaAssocDefs(dm4, mf, "dm4.poemspace.campaign", criteriaTypeUris);
        addCriteriaAssocDefs(dm4, mf, "dm4.contacts.person", criteriaTypeUris);
        addCriteriaAssocDefs(dm4, mf, "dm4.contacts.institution", criteriaTypeUris);
    }
}
