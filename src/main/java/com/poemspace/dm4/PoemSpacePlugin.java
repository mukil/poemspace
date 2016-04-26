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
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import de.deepamehta.core.Association;
import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.TopicType;
import de.deepamehta.core.ViewConfiguration;
import de.deepamehta.core.model.AssociationDefinitionModel;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.IndexMode;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.model.TopicTypeModel;
import de.deepamehta.core.osgi.PluginActivator;
import de.deepamehta.core.service.Inject;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.service.Transactional;
import de.deepamehta.core.storage.spi.DeepaMehtaTransaction;
import de.deepamehta.plugins.accesscontrol.AccessControlService;
import de.deepamehta.plugins.mail.Mail;
import de.deepamehta.plugins.mail.StatusReport;
import de.deepamehta.plugins.mail.service.MailService;

@Path("/poemspace")
@Produces(MediaType.APPLICATION_JSON)
public class PoemSpacePlugin extends PluginActivator {

    private static final String CAMPAIGN    = "dm4.poemspace.campaign";
    private static final String COUNT       = "dm4.poemspace.campaign.count";
    private static final String EXCLUDE     = "dm4.poemspace.campaign.excl";
    private static final String INCLUDE     = "dm4.poemspace.campaign.adds";

    private static Logger log = Logger.getLogger(PoemSpacePlugin.class.getName());

    @Inject private AccessControlService acService; // currently unused
    @Inject private MailService mailService;

    private CriteriaCache criteria = null;

    public static final Comparator<Topic> VALUE_COMPARATOR = new Comparator<Topic>() {
        @Override
        public int compare(Topic a, Topic b) {
            return a.getSimpleValue().toString().compareTo(b.getSimpleValue().toString());
        }
    };

    @Override
    public void init() {
        log.info("Initially populating criteria cache necessary for sending poemspace mail campaigns");
        reloadCriteriaCache();
    }

    @GET
    @Path("/criteria-types")
    public List<Topic> getCriteriaTypes() {
        if (criteria == null) return reloadCriteriaCache();
        return criteria.getTypes();
    }

    @POST
    @Path("/criteria-reload")
    public List<Topic> reloadCriteriaCache() {
        criteria = new CriteriaCache(dms);
        return getCriteriaTypes();
    }

    @POST
    @Path("/criteria/{name}")
    @Transactional
    public Topic createCriteria(@PathParam("name") String name) {
        log.info("Create new Poemspace Criteria \"" + name + "\"");
        // TODO sanitize name parameter
        String uri = "dm4.poemspace.criteria." + name.trim().toLowerCase();
        TopicType type = dms.createTopicType(new TopicTypeModel(uri, name, "dm4.core.text"));
        type.addIndexMode(IndexMode.FULLTEXT);
        ViewConfiguration viewConfig = type.getViewConfig();
        viewConfig.addSetting("dm4.webclient.view_config",//
                "dm4.webclient.multi_renderer_uri", "dm4.webclient.checkbox_renderer");
        viewConfig.addSetting("dm4.webclient.view_config",//
                "dm4.webclient.show_in_create_menu", true);
        viewConfig.addSetting("dm4.webclient.view_config",//
                "dm4.webclient.searchable_as_unit", true);
        // associate criteria type
        dms.createAssociation(new AssociationModel("dm4.core.association",
                new TopicRoleModel("dm4.poemspace.criteria.type", "dm4.core.default"),
                new TopicRoleModel(type.getId(), "dm4.core.default")));
        // create search type aggregates
        for (Topic topic : mailService.getSearchParentTypes()) {
            TopicType searchType = dms.getTopicType(topic.getUri());
            searchType.addAssocDef(new AssociationDefinitionModel("dm4.core.aggregation_def",
                    searchType.getUri(), type.getUri(), "dm4.core.one", "dm4.core.many"));
        }
        // renew cache
        criteria = new CriteriaCache(dms);
        return type;
    }

    @POST
    @Path("/campaign/{id}/include/{recipient}")
    public Association include(@PathParam("id") long campaignId, @PathParam("recipient") long recipientId) {
        log.info("Include recipient " + recipientId + " in Campaign " + campaignId);
        return createOrUpdateRecipient(INCLUDE, campaignId, recipientId);
    }

    @POST
    @Path("/campaign/{id}/exclude/{recipient}")
    public Association exclude(@PathParam("id") long campaignId, @PathParam("recipient") long recipientId) {
        log.info("Exclude recipient " + recipientId + " from Campaign " + campaignId);
        return createOrUpdateRecipient(EXCLUDE, campaignId, recipientId);
    }

    @GET
    @Path("/campaign/{id}/recipients")
    @Transactional
    public List<Topic> queryCampaignRecipients(@PathParam("id") long campaignId) {
        log.info("Fetching Recipients of Campaign (Topic ID:" + campaignId + ")");
        try {
            Topic campaign = dms.getTopic(campaignId);
            // get and sort recipients
            List<Topic> recipients = queryCampaignRecipients(campaign);
            Collections.sort(recipients, VALUE_COMPARATOR);
            // update campaign count and return result
            campaign.getChildTopics().set(COUNT, recipients.size());
            return recipients;
        } catch (Exception e) {
            throw new RuntimeException("recipients query of campaign " + campaignId + " FAILED", e);
        }
    }

    /**
     * Starts and returns a new campaign from a mail.
     * 
     * @param mailId
     * @return Campaign associated with the starting mail.
     */
    @PUT
    @Path("/mail/{id}/start")
    @Transactional
    public Topic startCampaign(@PathParam("id") long mailId) {
        log.info("Start a campaign from mail " + mailId);
        try {
            Topic campaign = dms.createTopic(new TopicModel(CAMPAIGN));
            dms.createAssociation(new AssociationModel("dm4.core.association",
                    new TopicRoleModel(mailId, "dm4.core.default"),
                    new TopicRoleModel(campaign.getId(), "dm4.core.default")));
            return campaign;
        } catch (Exception e) {
            throw new RuntimeException("Starting a campaign from mail " + mailId + " FAILED", e);
        }
    }

    /**
     * Sends a campaign mail.
     * 
     * @param mailId
     * @return Sent mail topic.
     */
    @PUT
    @Path("/mail/{id}/send")
    @Transactional
    public StatusReport sendCampaignMail(@PathParam("id") long mailId) {
        log.info("Sending Campaign Mail " + mailId);
        try {
            Topic mail = dms.getTopic(mailId);
            RelatedTopic campaign = mail.getRelatedTopic("dm4.core.association",
                    "dm4.core.default", "dm4.core.default", CAMPAIGN);
            // associate recipients of query result
            mailService.associateValidatedRecipients(mailId, queryCampaignRecipients(campaign));
            // send and report status
            StatusReport report = mailService.send(new Mail(mailId, dms));
            return report;
        } catch (Exception e) {
            throw new RuntimeException("Sending Campaign Mail " + mailId + " FAILED", e);
        }
    }

    private List<Topic> queryCampaignRecipients(Topic campaign) {
        List<Topic> recipients = new ArrayList<Topic>();
        Set<String> searchTypeUris = getSearchTypeUris();
        Map<String, List<RelatedTopic>> criterionMap = getCriterionMap(campaign);
        // get and add the first recipient list
        Iterator<String> criteriaIterator = criterionMap.keySet().iterator();
        if (criteriaIterator.hasNext()) {
            String uri = criteriaIterator.next();
            List<RelatedTopic> topics = criterionMap.get(uri);
            List<Topic> and = getCriterionRecipients(topics, searchTypeUris);
            recipients.addAll(and);
            if (recipients.isEmpty() == false) { // merge each other list
                while (criteriaIterator.hasNext()) {
                    uri = criteriaIterator.next();
                    topics = criterionMap.get(uri);
                    and = getCriterionRecipients(topics, searchTypeUris);
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
        Iterator<RelatedTopic> includes = campaign.getRelatedTopics(INCLUDE, 0).iterator();
        while (includes.hasNext()) {
            RelatedTopic include = includes.next();
            if (recipients.contains(include) == false) {
                recipients.add(include);
            }
        }
        // get and remove excludes
        Iterator<RelatedTopic> excludes = campaign.getRelatedTopics(EXCLUDE, 0).iterator();
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
     * @param criterionList
     *            criterion topics
     * @param searchTypeUris
     *            topic type URIs of possible recipients
     * @return
     */
    private List<Topic> getCriterionRecipients(List<RelatedTopic> criterionList, Set<String> searchTypeUris) {
        List<Topic> recipients = new ArrayList<Topic>();
        for (Topic criterion : criterionList) {
            for (RelatedTopic topic : dms.getTopic(criterion.getId())
                    .getRelatedTopics("dm4.core.aggregation", "dm4.core.child", "dm4.core.parent",
                            null, 0)) {
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
    private Map<String, List<RelatedTopic>> getCriterionMap(Topic topic) {
        Map<String, List<RelatedTopic>> criterionMap = new HashMap<String, List<RelatedTopic>>();
        for (String typeUri : criteria.getTypeUris()) {
            ResultList<RelatedTopic> relatedTopics = topic.getRelatedTopics("dm4.core.aggregation",
                    "dm4.core.parent", "dm4.core.child", typeUri, 0);
            if (relatedTopics.getSize() > 0) {
                criterionMap.put(typeUri, relatedTopics.getItems());
            }
        }
        return criterionMap;
    }

    private Association createOrUpdateRecipient(String typeUri, long campaignId, long recipientId) {
        log.fine("Create recipient " + typeUri + " association");
        List<Association> associations = dms.getAssociations(campaignId, recipientId);
        if (associations.size() > 1) {
            throw new IllegalStateException("only one association is supported");
        }
        DeepaMehtaTransaction tx = dms.beginTx();
        try {
            for (Association association : associations) {
                log.fine("Update recipient " + typeUri + " association");
                association.setTypeUri(typeUri);
                return association; // only one association can be used
            }
            Association association = dms.createAssociation(new AssociationModel(typeUri,
                    new TopicRoleModel(campaignId, "dm4.core.default"),
                    new TopicRoleModel(recipientId, "dm4.core.default")));
            tx.success();
            return association;
        } finally {
            tx.finish();
        }
    }

}
