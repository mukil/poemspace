package com.poemspace.dm4;

import java.util.ArrayList;
import java.util.Arrays;
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
import de.deepamehta.core.DeepaMehtaTransaction;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.ResultSet;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.PluginService;
import de.deepamehta.core.service.event.InitializePluginListener;
import de.deepamehta.core.service.event.PluginServiceArrivedListener;
import de.deepamehta.core.service.event.PluginServiceGoneListener;
import de.deepamehta.plugins.mail.RecipientType;
import de.deepamehta.plugins.mail.service.MailService;

@Path("/poemspace")
@Produces(MediaType.APPLICATION_JSON)
public class PoemSpacePlugin extends PluginActivator implements //
        InitializePluginListener,//
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
    @Path("/criteria/{name}")
    public Topic createCriteria(@PathParam("name") String name,
            @HeaderParam("Cookie") ClientState cookie) {
        log.info("create criteria " + name);
        // TODO sanitize name parameter
        String uri = "dm4.poemspace.criteria." + name.trim().toLowerCase();

        TopicType type = null;
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            type = dms.createTopicType(//
                    new TopicTypeModel(uri, name, "dm4.core.text"), cookie);
            type.setIndexModes(new HashSet<IndexMode>(Arrays.asList(IndexMode.FULLTEXT)));

            ViewConfiguration viewConfig = type.getViewConfig();
            viewConfig.addSetting("dm4.webclient.view_config",//
                    "dm4.webclient.multi_renderer_uri", "dm4.webclient.checkbox_renderer");
            viewConfig.addSetting("dm4.webclient.view_config",//
                    "dm4.webclient.add_to_create_menu", true);
            viewConfig.addSetting("dm4.webclient.view_config",//
                    "dm4.webclient.is_searchable_unit", true);

            // associate criteria type
            dms.createAssociation(new AssociationModel("dm4.core.association",//
                    new TopicRoleModel("dm4.poemspace.criteria.type", "dm4.core.default"),//
                    new TopicRoleModel(type.getId(), "dm4.core.default"), null), cookie);

            // create search type aggregates
            for (Topic topic : mailService.getSearchParentTypes()) {
                TopicType searchType = dms.getTopicType(topic.getUri(), cookie);
                searchType.addAssocDef(new AssociationDefinitionModel("dm4.core.aggregation_def",
                        searchType.getUri(), type.getUri(), "dm4.core.one", "dm4.core.many"));
            }

            // renew cache
            criteria = new CriteriaCache(dms);
            tx.success();
        } catch (Exception e) {
            throw new WebApplicationException(e);
        } finally {
            tx.finish();
        }

        return type;
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
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            Topic campaign = dms.getTopic(campaignId, true, cookie);
            RelatedTopic template = campaign.getRelatedTopic("dm4.core.aggregation", //
                    "dm4.core.whole", "dm4.core.part",//
                    "dm4.poemspace.template", true, false, cookie);

            String subject = campaign.getCompositeValue().getString("dm4.mail.subject");
            String body = template.getCompositeValue().getString("dm4.mail.body");
            RelatedTopic sender = template.getRelatedTopic("dm4.mail.sender",//
                    "dm4.core.whole", "dm4.core.part", null, true, false, cookie);

            Topic mail = dms.createTopic(new TopicModel("dm4.mail", new CompositeValue()//
                    .put("dm4.mail.from", true)// do not use the default sender
                    .put("dm4.mail.subject", subject)//
                    .put("dm4.mail.body", body)), cookie);

            mailService.associateSender(mail.getId(), sender, cookie);
            for (Topic recipient : queryCampaignRecipients(campaign)) {
                mailService.associateRecipient(mail.getId(),
                        dms.getTopic(recipient.getId(), true, cookie), RecipientType.BCC, cookie);
            }
            dms.createAssociation(new AssociationModel("dm4.core.association",//
                    new TopicRoleModel(campaignId, "dm4.core.default"),//
                    new TopicRoleModel(mail.getId(), "dm4.core.default"), null), cookie);
            tx.success();
            return mail;
        } catch (Exception e) {
            throw new WebApplicationException(new RuntimeException(//
                    "write a mail from campaign \"" + campaignId + "\" failed", e));
        } finally {
            tx.finish();
        }
    }

    /**
     * Initialize criteria cache.
     */
    @Override
    public void initializePlugin() {
        // TODO add update listener to reload cache (create, update, delete)
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
            log.fine("update recipient " + typeUri + " association");
            association.setTypeUri(typeUri);
            return association; // only one association can be used
        }
        log.fine("create recipient " + typeUri + " association");
        return dms.createAssociation(new AssociationModel(typeUri,//
                new TopicRoleModel(campaignId, "dm4.core.default"),//
                new TopicRoleModel(recipientId, "dm4.core.default"), null), clientState);
    }

}
