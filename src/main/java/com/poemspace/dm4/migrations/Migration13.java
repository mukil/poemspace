package com.poemspace.dm4.migrations;

import static com.poemspace.dm4.MigrationUtils.*;

import java.util.HashMap;
import java.util.Map;

import de.deepamehta.core.RelatedTopic;
import de.deepamehta.core.Topic;
import de.deepamehta.core.model.CompositeValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.service.Migration;

public class Migration13 extends Migration {

    @SuppressWarnings("serial")
    @Override
    public void run() {

        // get criteria topics
        final Map<String, Long> art = getIdsByValue(dms, ART);
        final Map<String, Long> area = getIdsByValue(dms, AREA);
        final Map<String, Long> media = getIdsByValue(dms, MEDIA);

        // define mapping
        Map<String, Map<String, Long>> MAP = new HashMap<String, Map<String, Long>>();
        MAP.put("Café", new HashMap<String, Long>() {
            {
                put(ART, art.get("Café/Restaurant"));
            }
        });
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
        MAP.put("Kulturelle Bildung", new HashMap<String, Long>() {
            {
                put(AREA, area.get("Kulturelle Bildung"));
            }
        });
        MAP.put("Radio", new HashMap<String, Long>() {
            {
                put(AREA, area.get("Presse"));
                put(MEDIA, media.get("Radio"));
            }
        });
        MAP.put("Radio regional", new HashMap<String, Long>() {
            {
                put(AREA, area.get("Presse"));
            }
        });
        MAP.put("Radio Überregional", new HashMap<String, Long>() {
            {
                put(AREA, area.get("Presse"));
            }
        });
        MAP.put("regionale Printmedien", new HashMap<String, Long>() {
            {
                put(AREA, area.get("Presse"));
                put(MEDIA, media.get("Regional"));
            }
        });
        MAP.put("Restaurant", new HashMap<String, Long>() {
            {
                put(ART, art.get("Café/Restaurant"));
            }
        });
        MAP.put("TV", new HashMap<String, Long>() {
            {
                put(AREA, area.get("Presse"));
                put(MEDIA, media.get("Fernsehen"));
            }
        });
        MAP.put("Überregionale Print", new HashMap<String, Long>() {
            {
                put(AREA, area.get("Presse"));
                put(MEDIA, media.get("Überregional"));
            }
        });
        MAP.put("Websites", new HashMap<String, Long>() {
            {
                put(AREA, area.get("Presse"));
                put(MEDIA, media.get("Web"));
            }
        });
        MAP.put("Zeitschrift", new HashMap<String, Long>() {
            {
                put(AREA, area.get("Presse"));
            }
        });

        for (Topic artType : dms.getTopics(ART, false, 0, null)) {
            Map<String, Long> criteria = MAP.get(artType.getSimpleValue().toString());
            if (criteria != null) {
                // map composite value
                CompositeValue valueUpdate = new CompositeValue();
                for (String uri : criteria.keySet()) {
                    valueUpdate.add(uri, new TopicModel(criteria.get(uri)));
                }

                // update all related contacts
                for (RelatedTopic contact : artType.getRelatedTopics("dm4.core.aggregation", //
                        "dm4.core.part", "dm4.core.whole", null, false, false, 0, null)) {
                    contact.setCompositeValue(valueUpdate, null, null);
                }
            }
        }
    }

}
