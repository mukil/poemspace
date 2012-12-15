package com.poemspace.dm4.migrations;

import java.util.List;

import com.poemspace.dm4.CriteriaCache;
import com.poemspace.dm4.MigrationUtils;

import de.deepamehta.core.service.Migration;

public class Migration3 extends Migration {

    private void changeIcon(String typeUri, String iconPath) {
        dms.getTopicType(typeUri, null).getViewConfig()
                .addSetting("dm4.webclient.view_config", "dm4.webclient.icon", iconPath);
    }

    @Override
    public void run() {
        // configure individual icons
        changeIcon("dm4.contacts.institution", "/com.poemspace.dm4-poemspace/images/birdhouse.png");
        changeIcon("dm4.contacts.person", "/com.poemspace.dm4-poemspace/images/bird.png");
        changeIcon("dm4.notes.note", "/com.poemspace.dm4-poemspace/images/pen.png");
        changeIcon("dm4.webclient.search", "/com.poemspace.dm4-poemspace/images/bullseye.png");

        // add criteria type aggregations
        List<String> criteriaTypeUris = new CriteriaCache(dms).getTypeUris();
        MigrationUtils.addCriteriaAssocDefs(dms, "dm4.poemspace.campaign", criteriaTypeUris);
        MigrationUtils.addCriteriaAssocDefs(dms, "dm4.contacts.person", criteriaTypeUris);
        MigrationUtils.addCriteriaAssocDefs(dms, "dm4.contacts.institution", criteriaTypeUris);
    }

}
