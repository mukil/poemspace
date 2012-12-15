package com.poemspace.dm4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.service.DeepaMehtaService;

public class MigrationUtils {

    public static final String AFFILIATION = "dm4.poemspace.affiliation";

    public static final String AREA = "dm4.poemspace.workarea";

    public static final String ART = "dm4.poemspace.art";

    public static final String LIST = "dm4.poemspace.list";

    public static final String MEDIA = "dm4.poemspace.media";

    public static final String PROJECT = "dm4.poemspace.project";

    public static final String YEAR = "dm4.poemspace.year";

    public static String[] CONTACT_URIS = { "dm4.contacts.person", "dm4.contacts.institution" };

    public static void addCriteriaAssocDefs(DeepaMehtaService dms, String typeUri,
            List<String> criteriaTypeUris) {
        TopicType topicType = dms.getTopicType(typeUri, null);
        for (String uri : criteriaTypeUris) {
            topicType.addAssocDef(new AssociationDefinitionModel("dm4.core.aggregation_def",//
                    typeUri, uri, "dm4.core.one", "dm4.core.many"));
        }
    }

    public static Map<String, Long> getIdsByValue(DeepaMehtaService dms, String typeUri) {
        HashMap<String, Long> map = new HashMap<String, Long>();
        for (RelatedTopic topic : dms.getTopics(typeUri, false, 0, null).getItems()) {
            map.put(topic.getSimpleValue().toString(), topic.getId());
        }
        return map;
    }

}
