package com.poemspace.dm4.migrations;

import java.util.List;

import com.poemspace.dm4.CriteriaCache;
import static com.poemspace.dm4.MigrationUtils.*;

import de.deepamehta.core.service.Migration;

public class Migration3 extends Migration {

    @Override
    public void run() {
        // configure individual icons
        changeIcon(dms, "dm4.contacts.institution",
                "/com.poemspace.dm4-poemspace/images/birdhouse.png");
        changeIcon(dms, "dm4.contacts.person", "/com.poemspace.dm4-poemspace/images/bird.png");
        changeIcon(dms, "dm4.notes.note", "/com.poemspace.dm4-poemspace/images/pen.png");
        changeIcon(dms, "dm4.webclient.search", "/com.poemspace.dm4-poemspace/images/bullseye.png");

        // add criteria type aggregations
        List<String> criteriaTypeUris = new CriteriaCache(dms).getTypeUris();
        addCriteriaAssocDefs(dms, "dm4.poemspace.campaign", criteriaTypeUris);
        addCriteriaAssocDefs(dms, "dm4.contacts.person", criteriaTypeUris);
        addCriteriaAssocDefs(dms, "dm4.contacts.institution", criteriaTypeUris);
    }

}
