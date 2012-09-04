package com.poemspace.dm4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.DeepaMehtaService;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.event.PluginServiceArrivedListener;
import de.deepamehta.core.service.event.PluginServiceGoneListener;
import de.deepamehta.plugins.mail.RecipientType;
import de.deepamehta.plugins.mail.service.MailService;

@Path("/poemspace")
@Produces(MediaType.APPLICATION_JSON)
public class PoemSpacePlugin extends PluginActivator implements //
        PluginServiceArrivedListener,//
        PluginServiceGoneListener {

    private static final String EXCLUDE = "dm4.poemspace.campaign.excl";

    private static final String INCLUDE = "dm4.poemspace.campaign.adds";

    private static Logger log = Logger.getLogger(PoemSpacePlugin.class.getName());

    private CriteriaCache criteria = null;

    private MailService mailService;

    public static final Comparator<Topic> VALUE_COMPARATOR = new Comparator<Topic>() {
        @Override
        public int compare(Topic a, Topic b) {
            return a.getSimpleValue().toString().compareTo(b.getSimpleValue().toString());
        }
    };

    @GET
    @Path("/criteria-types")
    public List<Topic> getCriteriaTypes() {
        try {
            return criteria.getTypes();
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @POST
    @Path("/campaign/{id}/include/{recipient}")
    public Association include(//
            @PathParam("id") long campaignId,//
            @PathParam("recipient") long recipientId,//
            @HeaderParam("Cookie") ClientState cookie) {
        try {
            log.info("include recipient " + recipientId + " into campaign " + campaignId);
            return createOrUpdateRecipient(INCLUDE, campaignId, recipientId, cookie);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @POST
    @Path("/campaign/{id}/exclude/{recipient}")
    public Association exclude(//
            @PathParam("id") long campaignId,//
            @PathParam("recipient") long recipientId,//
            @HeaderParam("Cookie") ClientState cookie) {
        try {
            log.info("exclude recipient " + recipientId + " from campaign " + campaignId);
            return createOrUpdateRecipient(EXCLUDE, campaignId, recipientId, cookie);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/campaign/{id}/recipients")
    public List<Topic> queryCampaignRecipients(//
            @PathParam("id") long campaignId,//
            @HeaderParam("Cookie") ClientState cookie) {
        log.info("get campaign " + campaignId + " recipients");
        try {
            Topic campaign = dms.getTopic(campaignId, true, cookie);
            List<Topic> recipients = queryCampaignRecipients(campaign);
            Collections.sort(recipients, VALUE_COMPARATOR);
            return recipients;
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException(//
                    "recipients query of campaign " + campaignId + " failed", e));
        }
    }

    @POST
    @Path("/campaign/{id}/write")
    public Topic writeMail(//
            @PathParam("id") long campaignId,//
            @HeaderParam("Cookie") ClientState cookie) {
        log.info("create a campaign " + campaignId + " mail");
        try {
            Topic campaign = dms.getTopic(campaignId, true, cookie);
            String subject = campaign.getCompositeValue().getString("dm4.mail.subject");
            TopicModel template = campaign.getCompositeValue().getTopic("dm4.poemspace.template");
            String body = template.getCompositeValue().getString("dm4.mail.body");

            TopicModel model = new TopicModel("dm4.mail", new CompositeValue()//
                    .put("dm4.mail.subject", subject)//
                    .put("dm4.mail.body", body));

            Topic mail = dms.createTopic(model, cookie);
            createOrUpdateRecipient("dm4.core.association", campaignId, mail.getId(), cookie);

            for (Topic recipient : queryCampaignRecipients(campaign)) {
                mailService.associateRecipient(mail.getId(),
                        dms.getTopic(recipient.getId(), true, cookie), RecipientType.BCC, cookie);
            }
            return mail;
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException(//
                    "write a mail from campaign \"" + campaignId + "\" failed", e));
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

    private List<Topic> queryCampaignRecipients(Topic campaign) {
        List<Topic> recipients = new ArrayList<Topic>();
        Set<String> searchTypeUris = getSearchTypeUris();
        Map<String, Set<RelatedTopic>> criterionMap = getCriterionMap(campaign);

        // get and add the first recipient list
        Iterator<String> criteriaIterator = criterionMap.keySet().iterator();
        if (criteriaIterator.hasNext()) {
            String uri = criteriaIterator.next();
            Set<RelatedTopic> topics = criterionMap.get(uri);
            Set<Topic> and = getCriterionRecipients(campaign, topics, searchTypeUris);
            recipients.addAll(and);
            if (recipients.isEmpty() == false) { // merge each other list
                while (criteriaIterator.hasNext()) {
                    uri = criteriaIterator.next();
                    topics = criterionMap.get(uri);
                    and = getCriterionRecipients(campaign, topics, searchTypeUris);
                    // TODO use iterator instead of cloned list
                    // TODO use map by ID to simplify contain check
                    for (Topic topic : new ArrayList<Topic>(recipients)) {
                        if (and.contains(topic) == false) {
                            recipients.remove(topic);
                        }
                        if (recipients.size() == 0) {
                            break;
                        }
                    }
                }
            }
        }

        // get and add includes
        Iterator<RelatedTopic> includes = campaign.getRelatedTopics(INCLUDE, 0, null).iterator();
        while (includes.hasNext()) {
            RelatedTopic include = includes.next();
            if (recipients.contains(include) == false) {
                recipients.add(include);
            }
        }

        // get and remove excludes
        Iterator<RelatedTopic> excludes = campaign.getRelatedTopics(EXCLUDE, 0, null).iterator();
        while (excludes.hasNext()) {
            RelatedTopic exclude = excludes.next();
            if (recipients.contains(exclude)) {
                recipients.remove(exclude);
            }
        }
        return recipients;
    }

    private Set<String> getSearchTypeUris() {
        Set<String> uris = new HashSet<String>();
        for (Topic topic : mailService.getSearchParentTypes()) {
            uris.add(topic.getUri());
        }
        return uris;
    }

    /**
     * Returns parent aggregates of each criterion.
     * 
     * @param campaign
     * @param criterionList
     *            criterion topics
     * @param searchTypeUris
     *            topic type URIs of possible recipients
     * @return
     */
    private Set<Topic> getCriterionRecipients(Topic campaign, Set<RelatedTopic> criterionList,
            Set<String> searchTypeUris) {
        Set<Topic> recipients = new HashSet<Topic>();
        for (Topic criterion : criterionList) {
            for (RelatedTopic topic : dms.getTopic(criterion.getId(), false, null)
                    .getRelatedTopics("dm4.core.aggregation", "dm4.core.part", "dm4.core.whole",
                            null, false, false, 0, null)) {
                if (searchTypeUris.contains(topic.getTypeUri())) {
                    recipients.add(topic);
                }
            }
        }
        return recipients;
    }

    /**
     * Returns all criteria aggregations of a topic.
     * 
     * @param topic
     * @return criterion map of all aggregated criteria sub type instances
     */
    private Map<String, Set<RelatedTopic>> getCriterionMap(Topic topic) {
        Map<String, Set<RelatedTopic>> criterionMap = new HashMap<String, Set<RelatedTopic>>();
        for (String typeUri : criteria.getTypeUris()) {
            ResultSet<RelatedTopic> relatedTopics = topic.getRelatedTopics("dm4.core.aggregation",
                    "dm4.core.whole", "dm4.core.part", typeUri, false, false, 0, null);
            if (relatedTopics.getSize() > 0) {
                criterionMap.put(typeUri, relatedTopics.getItems());
            }
        }
        return criterionMap;
    }

    private Association createOrUpdateRecipient(String typeUri, long campaignId, long recipientId,
            ClientState clientState) {
        Set<Association> associations = dms.getAssociations(campaignId, recipientId);
        if (associations.size() > 1) {
            throw new IllegalStateException("only one association is supported");
        }
        for (Association association : associations) {
            association.setTypeUri(typeUri);
            return association; // only one association can be used
        }
        return dms.createAssociation(new AssociationModel(typeUri,//
                new TopicRoleModel(campaignId, "dm4.core.default"),//
                new TopicRoleModel(recipientId, "dm4.core.default"), null), clientState);
    }
}
