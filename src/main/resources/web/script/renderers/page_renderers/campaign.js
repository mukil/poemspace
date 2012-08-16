/*global jQuery, dm4c*/

/**
 * campaign page renderer
 * interactive criteria selection and recipient addressing
 */
(function ($, dm4c) {

  // create criterion aggregation
  function addCriterion(campaignId, criterionId) {
    dm4c.restc.create_association({
      type_uri: 'dm4.core.aggregation',
      role_1: { topic_id: campaignId, role_type_uri: 'dm4.core.whole' },
      role_2: { topic_id: criterionId, role_type_uri: 'dm4.core.part' }
    })
  }

  // get and delete criterion aggregation
  function deleteCriterion(campaignId, criterionId) {
    var aggregate = dm4c.restc.get_association('dm4.core.aggregation',
      campaignId, criterionId, 'dm4.core.whole', 'dm4.core.part')
    dm4c.restc.delete_association(aggregate.id)
  }

  // get all criterion instances of a criteria
  function getCriterionList(criteria) {
    return dm4c.restc.get_related_topics(criteria.id, {
      assoc_type_uri: 'dm4.core.instantiation',
      others_role_type_uri: 'dm4.core.instance'
    }, null, null).items
  }

  function createCriteriaFieldset(criteria, aggregates) {
    var $criterionById = {},
      $legend = $('<legend>').append(criteria.value),
      $criteria = $('<ul>').addClass('criteria')

    $.each(getCriterionList(criteria), function (c, criterion) {
      var cId = 'c' + criterion.id,
        $label = $('<label>').append(criterion.value).attr('for', cId),
        $input = $('<input>').attr({
          id: cId,
          type: 'checkbox',
          value: criterion.id
        }).data('criterion', criterion).addClass('criterion'),
        $criterion = $('<li>').append($input).append($label)
      $criterionById[criterion.id] = $input
      $criteria.append($criterion)
    })

    // check the aggregates
    $.each(aggregates, function (o, aggregate) {
      $criterionById[aggregate.id].attr('checked', true)
    })

    return $('<fieldset>').append($legend).append($criteria)
  }

  function refreshRecipients(campaign, $recipents) {
    var recipients = dm4c.get_plugin('dm4.poemspace.plugin').getCampaignRecipients(campaign.id)
    $recipents.empty().append(dm4c.render.topic_list(recipients))
  }

  function registerCriterionChange(campaign, $parent, $recipients) {
    $parent.on('change', 'input[type="checkbox"]', function () {
      var $input = $(this),
        criterion = $input.data('criterion')
      if ($input.attr('checked')) {
        addCriterion(campaign.id, criterion.id)
      } else {
        deleteCriterion(campaign.id, criterion.id)
      }
      refreshRecipients(campaign, $recipients)
    })
  }

  function getAggregates(topic, type) {
    var aggregates = {}
    $.each(topic.composite[type.uri] || [], function (t, aggregate) {
      if (aggregate.id !== -1)
        aggregates[aggregate.id] = aggregate
    })
    return aggregates
  }

  /**
   * render a fieldset for each criteria and activate any aggregated criterion
   */
  function createCriteriaFieldsetList(campaign) {
    var $fieldList = $('<div>'),
      types = dm4c.get_plugin('dm4.poemspace.plugin').getCriteriaTypes()
    $.each(types, function (t, type) {
      var aggregates = getAggregates(campaign, type),
        $criteria = createCriteriaFieldset(type, aggregates)
      $fieldList.append($criteria)
    })
    return $fieldList
  }

  dm4c.add_page_renderer('dm4.poemspace.campaign.renderer', {

    render_page: function (topic) {
      dm4c.render.field_label('Template')
      dm4c.render.page('Monthly Meeting')
      //
      dm4c.render.associations(topic.id)
    },

    render_form: function (campaign) {
      var $left = $('<div>').css({ float: 'left', width: '49%' }),
        $right = $('<div>').css({ float: 'right', width: '49%' }),
        $criteria = createCriteriaFieldsetList(campaign),
        $recipients = $('<div>')

      dm4c.render.field_label('Template', $left)
      dm4c.render.field_label('Criteria', $left)
      dm4c.render.page($left)
      $left.append($criteria)

      dm4c.render.field_label('Recipients', $right)
      dm4c.render.page($right)
      $right.append($recipients)

      refreshRecipients(campaign, $recipients)
      registerCriterionChange(campaign, $criteria, $recipients)

      return function () {
        // nothing to do? dm4c.do_update_topic(campaign)
        dm4c.page_panel.refresh()
      }
    }
  })
})(jQuery, dm4c)
