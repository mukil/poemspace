package com.poemspace.dm4.service;

import java.util.List;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.PluginService;

public interface PoemSpaceService extends PluginService {

    List<Topic> getCriteria(long topicId);

    List<Topic> getCriteriaTypes();

}
