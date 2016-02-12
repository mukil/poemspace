package com.poemspace.dm4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.service.DeepaMehtaService;

public class CriteriaCache {

    private static Logger log = Logger.getLogger(CriteriaCache.class.getName());

    private List<Topic> types = null;
    private List<String> typeUris = null;

    private final DeepaMehtaService dms;

    public CriteriaCache(DeepaMehtaService dms) {
        this.dms = dms;
    }

    /**
     * Returns all criteria types.
     * 
     * @return list of all criteria associated type topics
     */
    public List<Topic> getTypes() {
        if (types == null) {
            types = new ArrayList<Topic>();

            log.info("reveal criteria types");
            Map<String, Topic> typesByUri = new HashMap<String, Topic>();
            TopicType criteriaType = dms.getTopicType("dm4.poemspace.criteria.type");
            for (RelatedTopic type : criteriaType.getRelatedTopics("dm4.core.association", null, null, null, 0)) {
                typesByUri.put(type.getUri(), type);
            }

            // use order of person aggregates
            TopicType personType = dms.getTopicType("dm4.contacts.person");
            Collection<AssociationDefinition> assocDefs = personType.getAssocDefs();

            for (AssociationDefinition assocDef : assocDefs) {
                if (assocDef.getTypeUri().equals("dm4.core.aggregation_def")) {
                    if (typesByUri.containsKey(assocDef.getChildTypeUri())) {
                        log.info("use criteria uri " + assocDef.getChildTypeUri());
                        types.add(typesByUri.get(assocDef.getChildTypeUri()));
                    }
                }
            }
        }
        return types;
    }

    /**
     * Returns all criteria type URIs.
     * 
     * @return list of all criteria associated type topic URIs
     */
    public List<String> getTypeUris() {
        if (typeUris == null) {
            log.info("reveal criteria type URIs");
            typeUris = new ArrayList<String>();
            for (Topic type : getTypes()) {
                typeUris.add(type.getUri());
            }
        }
        return typeUris;
    }

}
