package com.poemspace.dm4;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import com.poemspace.dm4.service.PoemSpaceService;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.DeepaMehtaService;

@Path("/poemspace")
@Produces("application/json")
public class PoemSpacePlugin extends PluginActivator implements PoemSpaceService {

    private Logger log = Logger.getLogger(getClass().getName());

    private CriteriaCache criteria = null;

    /**
     * get all criteria associations of a topic
     * 
     * @param topicId
     * @return the topic criterion list, all associated criteria sub-types
     */
    @GET
    @Path("/criteria/{id}")
    @Override
    public List<Topic> getCriteria(@PathParam("id") long topicId) {
        log.info("get topic " + topicId + " criteria");
        try {
            List<Topic> criterionList = new ArrayList<Topic>();
            Topic topic = dms.getTopic(topicId, false, null);
            for (String typeUri : criteria.getTypeUris()) {
                for (RelatedTopic relatedTopic : topic.getRelatedTopics(//
                        "dm4.core.association", null, null, typeUri, false, false, 0, null)) {
                    criterionList.add(relatedTopic);
                }
            }
            return criterionList;
        } catch (Throwable e) {
            throw new WebApplicationException(new RuntimeException(//
                    "reveal criteria of topic \"" + topicId + "\" failed", e));
        }
    }

    @GET
    @Path("/criteria-types")
    @Override
    public List<Topic> getCriteriaTypes() {
        return criteria.getTypes();
    }

    /**
     * Initialize criteria cache.
     */
    @Override
    public void setCoreService(DeepaMehtaService dms) {
        super.setCoreService(dms);
        // TODO add update listener to reload cache (create, update, delete)
        log.info("core service reference change");
        criteria = new CriteriaCache(dms);
    }

}
