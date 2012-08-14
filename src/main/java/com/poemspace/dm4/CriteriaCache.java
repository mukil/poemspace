package com.poemspace.dm4;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
     * get all criteria types
     * 
     * @return a list of all criteria type associated type topics
     */
    public List<Topic> getTypes() {
        if (types == null) {
            log.info("reveal criteria types");
            types = new ArrayList<Topic>();
            TopicType criteriaType = dms.getTopicType("dm4.poemspace.criteria.type", null);
            for (RelatedTopic type : criteriaType.getRelatedTopics("dm4.core.association", null,
                    null, null, false, false, 0, null)) {
                types.add(type);
            }
        }
        return types;
    }

    /**
     * get all criteria type URIs
     * 
     * @return a list of all criteria type associated type topic URIs
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
