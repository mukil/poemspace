package com.poemspace.dm4.migrations;

import java.util.List;

import com.poemspace.dm4.CriteriaCache;

import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.service.Migration;

public class Migration3 extends Migration {

    private void changeIcon(String typeUri, String iconPath) {
        dms.getTopicType(typeUri, null).getViewConfig()
                .addSetting("dm4.webclient.view_config", "dm4.webclient.icon", iconPath);
    }

    private void addCriteriaAssocDefs(String typeUri, List<String> criteriaTypeUris) {
        TopicType topicType = dms.getTopicType(typeUri, null);
        for (String uri : criteriaTypeUris) {
            topicType.addAssocDef(new AssociationDefinitionModel("dm4.core.aggregation_def",//
                    typeUri, uri, "dm4.core.one", "dm4.core.many"));
        }
    }

    @Override
    public void run() {

        // configure individual icons
        changeIcon("dm4.contacts.person", "com.poemspace.dm4-poemspace/images/bird.png");
        changeIcon("dm4.contacts.institution", "com.poemspace.dm4-poemspace/images/birdhouse.png");
        changeIcon("dm4.webclient.search", "com.poemspace.dm4-poemspace/images/bullseye.png");

        // add criteria type aggregations
        List<String> criteriaTypeUris = new CriteriaCache(dms).getTypeUris();
        addCriteriaAssocDefs("dm4.poemspace.campaign", criteriaTypeUris);
        addCriteriaAssocDefs("dm4.contacts.person", criteriaTypeUris);
        addCriteriaAssocDefs("dm4.contacts.institution", criteriaTypeUris);
    }

}
