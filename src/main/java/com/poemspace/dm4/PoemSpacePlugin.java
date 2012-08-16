package com.poemspace.dm4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.listener.PluginServiceArrivedListener;
import de.deepamehta.core.service.listener.PluginServiceGoneListener;
import de.deepamehta.plugins.mail.service.MailService;

@Path("/poemspace")
@Produces("application/json")
public class PoemSpacePlugin extends PluginActivator implements PluginServiceArrivedListener,
        PluginServiceGoneListener {

    private Logger log = Logger.getLogger(getClass().getName());

    private CriteriaCache criteria = null;

    private MailService mailService;

    private ResultSet<Topic> toResultSet(Collection<Topic> topics) {
        return new ResultSet<Topic>(topics.size(), new HashSet<Topic>(topics));
    }

    @GET
    @Path("/criteria-types")
    public ResultSet<Topic> getCriteriaTypes() {
        try {
            return toResultSet(criteria.getTypes());
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    private List<String> getSearchTypeUris() {
        List<String> uris = new ArrayList<String>();
        ResultSet<Topic> searchParentTypes = mailService.getSearchParentTypes();
        for (Topic topic : searchParentTypes) {
            uris.add(topic.getUri());
        }
        return uris;
    }

    private List<Topic> getCriterionRecipients(Topic mailing, List<Topic> criterionList,
            List<String> searchTypeUris, ClientState clientState) {
        List<Topic> recipients = new ArrayList<Topic>();
        for (Topic criterion : criterionList) {
            for (RelatedTopic topic : dms.getTopic(criterion.getId(), false, clientState)
                    .getRelatedTopics("dm4.core.aggregation", "dm4.core.part", "dm4.core.whole",
                            null, false, false, 0, clientState)) {
                if (searchTypeUris.contains(topic.getTypeUri())) {
                    recipients.add(topic);
                }
            }
        }
        return recipients;
    }

    /**
     * get all criteria aggregations of a topic
     * 
     * @param topicId
     * @return the topic criterion list, all aggregated criteria sub type
     *         instances
     */
    private Map<String, List<Topic>> getCriterionMap(Topic topic, ClientState clientState) {
        Map<String, List<Topic>> criterionHash = new HashMap<String, List<Topic>>();
        for (String typeUri : criteria.getTypeUris()) {
            ResultSet<RelatedTopic> relatedTopics = topic.getRelatedTopics("dm4.core.aggregation",
                    "dm4.core.whole", "dm4.core.part", typeUri, false, false, 0, null);
            if (relatedTopics.getSize() > 0) {
                List<Topic> topics = new ArrayList<Topic>();
                for (RelatedTopic relatedTopic : relatedTopics) {
                    topics.add(relatedTopic);
                }
                criterionHash.put(typeUri, topics);
            }
        }
        return criterionHash;

    }

    @GET
    @Path("/campaign/recipients/{id}")
    public ResultSet<Topic> queryCampaignRecipients(@PathParam("id") long campaignId,
            @HeaderParam("Cookie") ClientState clientState) {
        log.info("get campaign " + campaignId + " recipients");
        try {
            List<Topic> recipients = new ArrayList<Topic>();
            Topic campaign = dms.getTopic(campaignId, true, clientState);
            List<String> searchTypeUris = getSearchTypeUris();
            Map<String, List<Topic>> criterionMap = getCriterionMap(campaign, clientState);

            // get the first recipient list
            Iterator<String> criteriaIterator = criterionMap.keySet().iterator();
            if (criteriaIterator.hasNext()) {
                recipients.addAll(getCriterionRecipients(campaign,
                        criterionMap.get(criteriaIterator.next()), searchTypeUris, clientState));
                if (recipients.isEmpty() == false) {
                    // merge each other list
                    while (criteriaIterator.hasNext()) {
                        List<Topic> andRecipients = getCriterionRecipients(campaign,
                                criterionMap.get(criteriaIterator.next()), searchTypeUris,
                                clientState);
                        for (Topic topic : new ArrayList<Topic>(recipients)) {
                            if (andRecipients.contains(topic) == false) {
                                recipients.remove(topic);
                            }
                            if (recipients.size() == 0) {
                                break;
                            }
                        }
                    }
                }
            }
            return toResultSet(recipients);
        } catch (Throwable e) {
            throw new WebApplicationException(new RuntimeException(//
                    "recipients query of campaign \"" + campaignId + "\" failed", e));
        }
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

    @Override
    public void pluginServiceArrived(PluginService service) {
        if (service instanceof MailService) {
            log.fine("mail service arrived");
            mailService = (MailService) service;
        }
    }

    @Override
    public void pluginServiceGone(PluginService service) {
        if (service == mailService) {
            mailService = null;
        }
    }
}
