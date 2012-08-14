/*global jQuery, dm4c*/

/**
 * campaign page renderer
 * interactive criteria selection and recipient addressing
 */
(function ($, dm4c) {

  function getCriteriaTypes() {
    var criteriaType = dm4c.get_topic_type('dm4.poemspace.criteria.type')
    return dm4c.restc.get_related_topics(criteriaType.id, {
      assoc_type_uri: 'dm4.core.association'
    }, null, null).items
  }

  function getCriterionList(criteria) {
    return dm4c.restc.get_related_topics(criteria.id, {
      assoc_type_uri: 'dm4.core.instantiation',
      //my_role_type_uri: 'dm4.core.type',
      others_role_type_uri: 'dm4.core.instance'
    }, null, null).items
  }

  function createCriteriaFieldset(criteria, criterionList, aggregates) {
    var $criterionById = {},
      $legend = $('<legend>').append(criteria.value),
      $criteria = $('<ul>').addClass('criteria')

    $.each(criterionList, function (c, criterion) {
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

  function refreshRecipients(mailing, $recipents) {
    var recipients = []
    dm4c.restc.get_related_topics(mailing.id, {
      assoc_type_uri: 'dm4.core.aggregation',
      others_role_type_uri: 'dm4.core.part'
    }, null, null).items
  }

  function registerCriterionChange(mailing, $parent, $recipients) {
    $parent.on('change', 'input[type="checkbox"]', function () {
      var $input = $(this),
        criterion = $input.data('criterion')
      if ($input.attr('checked')) {
        dm4c.restc.create_association({
          type_uri: 'dm4.core.aggregation',
          role_1: { topic_id: mailing.id, role_type_uri: 'dm4.core.whole' },
          role_2: { topic_id: criterion.id, role_type_uri: 'dm4.core.part' }
        })
      } else {
        var aggregate = dm4c.restc.get_association('dm4.core.aggregation',
          mailing.id, criterion.id, 'dm4.core.whole', 'dm4.core.part')
        dm4c.restc.delete_association(aggregate.id)
      }
      refreshRecipients(mailing, $recipients)
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
  function createCriteriaFieldsetList(mailing) {
    var $fieldList = $('<div>'),
      types = getCriteriaTypes()
    $.each(types, function (t, type) {
      var aggregates = getAggregates(mailing, type),
        criterionList = getCriterionList(type),
        $criteria = createCriteriaFieldset(type, criterionList, aggregates)
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

    render_form: function (mailing) {
      var $left = $('<div>').attr('style', 'float: left'),
        $right = $('<div>').attr('style', 'float: right'),
        $criteria = createCriteriaFieldsetList(mailing),
        $recipients = $('<div>')

      dm4c.render.field_label('Template', $left)
      dm4c.render.field_label('Criteria', $left)
      dm4c.render.page($left)
      $left.append($criteria)

      dm4c.render.field_label('Recipients', $right)
      dm4c.render.page($right)
      $right.append($recipients)

      refreshRecipients(mailing, $recipients)
      registerCriterionChange(mailing, $criteria, $recipients)

      return function () {
        // nothing to do? dm4c.do_update_topic(mailing)
        dm4c.page_panel.refresh()
      }
    }
  })
})(jQuery, dm4c)
