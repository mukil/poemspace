package com.poemspace.dm4.migrations;

import static com.poemspace.dm4.MigrationUtils.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Migration;

public class Migration12 extends Migration {

    private static Logger log = Logger.getLogger(Migration11.class.getName());

    @SuppressWarnings("serial")
    @Override
    public void run() {

        // get criteria topics
        final Map<String, Long> projects = getIdsByValue(dms, PROJECT);
        final Map<String, Long> years = getIdsByValue(dms, YEAR);
        final Map<String, Long> affiliations = getIdsByValue(dms, AFFILIATION);
        final Map<String, Long> publics = getIdsByValue(dms, PUBLIC);

        // define mapping
        Map<String, Map<String, Long>> MAP = new HashMap<String, Map<String, Long>>();
        MAP.put("Mitstreiter10", new HashMap<String, Long>() {
            {
                put(PROJECT, projects.get("Poesiefrühling"));
                put(YEAR, years.get("2010"));
                put(AFFILIATION, affiliations.get("Mitstreiter"));
            }
        });
        MAP.put("Mitstreiter11", new HashMap<String, Long>() {
            {
                put(PROJECT, projects.get("Poesiefrühling"));
                put(YEAR, years.get("2011"));
                put(AFFILIATION, affiliations.get("Mitstreiter"));
            }
        });
        MAP.put("Mitstreiter 12", new HashMap<String, Long>() {
            {
                put(PROJECT, projects.get("Poesiefrühling"));
                put(YEAR, years.get("2012"));
                put(AFFILIATION, affiliations.get("Mitstreiter"));
            }
        });
        MAP.put("Printemps des Poètes Berlin09", new HashMap<String, Long>() {
            {
                put(PROJECT, projects.get("Poesiefrühling"));
                put(YEAR, years.get("2009"));
            }
        });
        MAP.put("Printemps des Poètes, Berlin10", new HashMap<String, Long>() {
            {
                put(PROJECT, projects.get("Poesiefrühling"));
                put(YEAR, years.get("2010"));
            }
        });
        MAP.put("Poesiefrühling-2011", new HashMap<String, Long>() {
            {
                put(PROJECT, projects.get("Poesiefrühling"));
                put(YEAR, years.get("2011"));
            }
        });
        MAP.put("Poesiefrühling 2012", new HashMap<String, Long>() {
            {
                put(PROJECT, projects.get("Poesiefrühling"));
                put(YEAR, years.get("2012"));
            }
        });

        MAP.put("MeisterInnen12", new HashMap<String, Long>() {
            {
                put(PROJECT, projects.get("Meister"));
                put(YEAR, years.get("2012"));
            }
        });
        MAP.put("MeisterInnen13", new HashMap<String, Long>() {
            {
                put(PROJECT, projects.get("Meister"));
                put(YEAR, years.get("2013"));
            }
        });

        MAP.put("wortwedding", new HashMap<String, Long>() {
            {
                put(PROJECT, projects.get("wortwedding"));
                put(AFFILIATION, affiliations.get("Publikum"));
            }
        });

        MAP.put("Poem Space Mobil", new HashMap<String, Long>() {
            {
                put(PROJECT, projects.get("Poem Space Mobil"));
            }
        });

        MAP.put("Stiftungen", new HashMap<String, Long>() {
            {
                put(AFFILIATION, affiliations.get("Förderer"));
            }
        });
        MAP.put("Blogs", new HashMap<String, Long>() {
            {
                put(PUBLIC, publics.get("Webseite"));
            }
        });

        for (Topic list : dms.getTopics(LIST, 0)) {
            String listName = list.getSimpleValue().toString();
            log.info("Reassign members of list " + listName);
            Map<String, Long> criteria = MAP.get(listName);
            if (criteria != null) {
                // map composite value
                ChildTopicsModel valueUpdate = new ChildTopicsModel();
                for (String uri : criteria.keySet()) {
                    valueUpdate.addRef(uri, criteria.get(uri));
                }
                // update all related contacts
                for (String contactTypeUri : CONTACT_URIS) {
                    TopicModel model = new TopicModel(contactTypeUri);
                    model.setChildTopicsModel(valueUpdate);
                    for (RelatedTopic contact : list.getRelatedTopics("dm4.core.association",
                            null, null, contactTypeUri, 0)) {
                        log.info("Update " + listName + " Contact " + contact.getSimpleValue());
                        model.setId(contact.getId());
                        dms.updateTopic(model);
                    }
                }
                // delete distribution list
                dms.deleteTopic(list.getId());
            }
        }
    }
}
