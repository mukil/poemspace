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
    return dm4c.restc.get_topic_related_topics(criteria.id, {
      assoc_type_uri: 'dm4.core.instantiation',
      others_role_type_uri: 'dm4.core.instance'
    }, null, null).items
  }

  function getRecipientsById(campaignId, type) {
    return dm4c.restc.get_topic_related_topics(campaignId, {
      assoc_type_uri: type
    }, null, null).items
  }

  function includeRecipient(campaignId, recipientId) {
    var uri = 'poemspace/campaign/' + campaignId + '/include/' + recipientId
    return dm4c.restc.request('POST', uri).items
  }

  function excludeRecipient(campaignId, recipientId) {
    var uri = 'poemspace/campaign/' + campaignId + '/exclude/' + recipientId
    return dm4c.restc.request('POST', uri).items
  }

  function createCriteriaFieldset(criteria, aggregates) {
    var $criterionById = {},
      $icon = dm4c.render.type_icon(criteria.uri).addClass('menu-icon'),
      $closed = $('<span>').addClass('ui-icon ui-icon-circle-triangle-s').css('float', 'right'),
      $open = $('<span>').addClass('ui-icon ui-icon-circle-triangle-n').css('float', 'right').hide(),
      $legend = $('<p>').addClass('ui-state-default').append($icon).append(criteria.value).append($closed).append($open),
      $criteria = $('<ul>').addClass('criteria').hide(),
      $fieldset = $('<div>').addClass('box level1').append($legend).append($criteria)

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

    return $fieldset
  }

  // remove recipient include, create an exclude and refresh the list
  function onRemoveInclude() {
    var $recipient = $(this).parent().parent(),
      recipientId = $recipient.data('recipient').id,
      campaignId = dm4c.selected_object.id,
      includes = dm4c.restc.get_associations(campaignId, recipientId, 'dm4.poemspace.campaign.adds')
    $.each(includes, function (i, include) {
      dm4c.do_delete_association(include)
    })
    excludeRecipient(campaignId, recipientId)
    refreshRecipients()
  }

  function onExclude() {
    var $recipient = $(this).parent().parent(),
      recipientId = $recipient.data('recipient').id,
      campaignId = dm4c.selected_object.id
    excludeRecipient(campaignId, recipientId)
    refreshRecipients()
  }

  function onRemoveExclude() {
    var $recipient = $(this).parent().parent(),
      recipientId = $recipient.data('recipient').id,
      campaignId = dm4c.selected_object.id,
      excludes = dm4c.restc.get_associations(campaignId, recipientId, 'dm4.poemspace.campaign.excl')
    $.each(excludes, function (i, exclude) {
      dm4c.do_delete_association(exclude)
    })
    refreshRecipients()
  }

  function createRecipient(recipient, type) {
    function click() {
      dm4c.do_reveal_related_topic(recipient.id)
    }

    var $icon = dm4c.render.icon_link(recipient, click),
      $topic = dm4c.render.topic_link(recipient, click),
      $topicDiv = $('<div>').addClass(type).append($icon).append($topic),
      $removeDiv = $('<div>').addClass('remove-button'),
      $recipient = $('<div>').append($topicDiv).append($removeDiv)
    if (type === 'exclude') {
      $removeDiv.append(dm4c.ui.button(onRemoveExclude, undefined, 'circle-plus'))
    } else if (type === 'include') {
      $removeDiv.append(dm4c.ui.button(onRemoveInclude, undefined, 'circle-minus'))
    } else { // result
      $removeDiv.append(dm4c.ui.button(onExclude, undefined, 'circle-minus'))
    }
    return $recipient.addClass('box').data('recipient', recipient)
  }

  // TODO move to js_utils
  function mapById(topics) {
    var topicsById = {}
    $.each(topics, function (r, topic) {
      topicsById[topic.id] = topic
    })
    return topicsById
  }

  function refreshRecipients() {
    var campaignId = dm4c.selected_object.id,
      recipients = dm4c.get_plugin('dm4.poemspace.plugin').getCampaignRecipients(campaignId),
      $recipients = $('#campaign' + campaignId + 'recipients').empty(),
      excludes = getRecipientsById(campaignId, 'dm4.poemspace.campaign.excl'),
      includesById = mapById(getRecipientsById(campaignId, 'dm4.poemspace.campaign.adds')),
      excludesById = mapById(excludes)

    $.merge(recipients, excludes).sort(function (a, b) {
      return (a.value < b.value) ? -1 : (a.value > b.value) ? 1 : 0
    })
    $recipients.prev().text(recipients.length + ' Recipients')
    $.each(recipients, function (r, recipient) {
      if (excludesById[recipient.id]) {
        $recipients.append(createRecipient(recipient, 'exclude'))
      } else if (includesById[recipient.id]) {
        $recipients.append(createRecipient(recipient, 'include'))
      } else {
        $recipients.append(createRecipient(recipient, 'result'))
      }
    })
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
      refreshRecipients()
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

    render_page: function (campaign) {
      var subject = campaign.composite['dm4.mail.subject'],
        template = campaign.composite['dm4.poemspace.template']
      if ($.isPlainObject(subject)) {
        dm4c.render.page($('<h1>').append(subject.value))
      }
      if ($.isPlainObject(template)) {
        dm4c.render.field_label('Template')
        function clickTemplate() {
          dm4c.do_reveal_related_topic(template.id)
        }

        dm4c.render.page(dm4c.render.icon_link(template, clickTemplate))
        dm4c.render.page(dm4c.render.topic_link(template, clickTemplate))
      }
      dm4c.render.topic_associations(campaign.id)
    },

    render_form: function (campaign) {
      var $left = $('<div>').css({ float: 'left', width: '49%' }),
        $right = $('<div>').css({ float: 'right', width: '49%' }),
        subject = campaign.composite['dm4.mail.subject'],
        template = campaign.composite['dm4.poemspace.template'] || { id: -1 },
        $subject = dm4c.render.input(subject),
        templateMenu = dm4c.render.topic_menu('dm4.poemspace.template', template.id),
        $criteria = createCriteriaFieldsetList(campaign),
        $recipients = $('<div>').attr('id', 'campaign' + campaign.id + 'recipients')

      dm4c.render.field_label('Subject', $left)
      $left.append($subject)

      dm4c.render.field_label('Template', $left)
      $left.append(templateMenu.dom)

      dm4c.render.field_label('Criteria', $left)
      $left.append($criteria)
      dm4c.render.page($left)
      $criteria.on('click', 'p', function () {
        $('ul', $(this).parent()).toggle()
        $('span', $(this)).toggle()
      })

      dm4c.render.field_label('Recipients', $right)
      dm4c.render.page($right)
      $right.append($recipients).append(dm4c.get_plugin('dm4.mail.plugin')
        .createCompletionField('Add', function include($item, item) {
          includeRecipient(campaign.id, item.id)
          refreshRecipients()
        }))

      refreshRecipients()
      registerCriterionChange(campaign, $criteria, $recipients)

      return function () {
        var selection = templateMenu.get_selection()
        dm4c.do_update_topic({
          id: campaign.id,
          composite: {
            'dm4.mail.subject': $.trim($subject.val()),
            'dm4.poemspace.template': dm4c.REF_PREFIX + selection.value
          }
        })
        dm4c.page_panel.refresh()
      }
    }
  })
}(jQuery, dm4c))
