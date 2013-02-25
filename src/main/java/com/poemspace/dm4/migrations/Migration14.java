package com.poemspace.dm4.migrations;

import static com.poemspace.dm4.MigrationUtils.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Migration;

public class Migration14 extends Migration {

    private static Logger log = Logger.getLogger(Migration13.class.getName());

    @SuppressWarnings("serial")
    @Override
    public void run() {

        // get criteria topics
        final Map<String, Long> art = getIdsByValue(dms, ART);
        final Map<String, Long> press = getIdsByValue(dms, PRESS);

        // define mapping
        Map<String, Map<String, Long>> MAP = new HashMap<String, Map<String, Long>>();
        MAP.put("Jugendclub", new HashMap<String, Long>() {
            {
                put(ART, art.get("Kinder- und Jugendeinrichtung"));
            }
        });
        MAP.put("Kinderclub", new HashMap<String, Long>() {
            {
                put(ART, art.get("Kinder- und Jugendeinrichtung"));
            }
        });
        MAP.put("Café", new HashMap<String, Long>() {
            {
                put(ART, art.get("Café/Restaurant"));
            }
        });
        MAP.put("Restaurant", new HashMap<String, Long>() {
            {
                put(ART, art.get("Café/Restaurant"));
            }
        });

        MAP.put("Radio", new HashMap<String, Long>() {
            {
                put(PRESS, press.get("Radio"));
            }
        });
        MAP.put("Radio regional", new HashMap<String, Long>() {
            {
                put(PRESS, press.get("Radio"));
                put(PRESS, press.get("regional"));
            }
        });
        MAP.put("Radio Überregional", new HashMap<String, Long>() {
            {
                put(PRESS, press.get("Radio"));
                put(PRESS, press.get("überregional"));
            }
        });

        MAP.put("Zeitschrift", new HashMap<String, Long>() {
            {
                put(PRESS, press.get("Print"));
            }
        });
        MAP.put("regionale Printmedien", new HashMap<String, Long>() {
            {
                put(PRESS, press.get("Print"));
                put(PRESS, press.get("regional"));
            }
        });
        MAP.put("Überregionale Print", new HashMap<String, Long>() {
            {
                put(PRESS, press.get("Print"));
                put(PRESS, press.get("überregional"));
            }
        });

        MAP.put("TV", new HashMap<String, Long>() {
            {
                put(PRESS, press.get("Fernsehen"));
            }
        });
        MAP.put("Websites", new HashMap<String, Long>() {
            {
                put(PRESS, press.get("Web"));
            }
        });

        for (Topic criterion : dms.getTopics(ART, false, 0, null)) {
            String criterionName = criterion.getSimpleValue().toString();
            log.info("reassign members of art criterion " + criterionName);
            Map<String, Long> criteria = MAP.get(criterionName);
            if (criteria != null) {
                // map composite value
                CompositeValueModel valueUpdate = new CompositeValueModel();
                for (String uri : criteria.keySet()) {
                    valueUpdate.add(uri, new TopicModel(criteria.get(uri)));
                }

                // update all related contacts
                for (RelatedTopic contact : criterion.getRelatedTopics("dm4.core.aggregation", //
                        "dm4.core.child", "dm4.core.parent", null, false, false, 0, null)) {
                    log.info("update " + criterionName + " contact " + contact.getSimpleValue());
                    contact.setCompositeValue(valueUpdate, null, null);
                }

                // delete distribution list
                dms.deleteTopic(criterion.getId(), null);
            }
        }
    }

}
