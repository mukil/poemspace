
/**
 * campaign page renderer
 * interactive criteria selection and recipient addressing
 */

// create criterion aggregation
function addCriterion(campaignId, criterionId) {
    dm4c.restc.create_association({
        type_uri: 'dm4.core.aggregation',
        role_1: {topic_id: campaignId, role_type_uri: 'dm4.core.parent'},
        role_2: {topic_id: criterionId, role_type_uri: 'dm4.core.child'}
    })
}

// get and delete criterion aggregation
function deleteCriterion(campaignId, criterionId) {
    var aggregate = dm4c.restc.get_association('dm4.core.aggregation',
        campaignId, criterionId, 'dm4.core.parent', 'dm4.core.child')
    dm4c.restc.delete_association(aggregate.id)
}

// get all criterion instances of a criteria
function getCriterionList(criteria) {
    return dm4c.restc.get_topic_related_topics(criteria.id, {
        assoc_type_uri: 'dm4.core.instantiation',
        others_role_type_uri: 'dm4.core.instance'
    }, null, null).items.sort(function (a, b) {
        return (a.value < b.value) ? -1 : (a.value > b.value) ? 1 : 0
    })
}

function getRecipient(childId, parentUri) {
    return dm4c.restc.get_topic_related_topics(childId, {
        assoc_type_uri: 'dm4.core.composition',
        my_role_type_uri: 'dm4.core.child',
        others_role_type_uri: 'dm4.core.parent',
        others_topic_type_uri: parentUri
    }).items[0]
    // return null
}

function getRecipientsById(campaignId, type) {
    console.log("Fetching Related Recipient Topics")
    return dm4c.restc.get_topic_related_topics(campaignId, {
        assoc_type_uri: type
    }, null, null).items
}

function includeRecipient(campaignId, recipientId) {
    var uri = '/poemspace/campaign/' + campaignId + '/include/' + recipientId
    console.log("POSTing include recipient", recipientId, campaignId)
    return dm4c.restc.request('POST', uri).items
}

function excludeRecipient(campaignId, recipientId) {
    var uri = '/poemspace/campaign/' + campaignId + '/exclude/' + recipientId
    console.log("POSTing exclude recipient", recipientId, campaignId)
    return dm4c.restc.request('POST', uri).items
}

function createCriteriaFieldset(criteria, aggregates) {
    var $criterionById = {},
        $icon = dm4c.render.type_icon(criteria.uri).addClass('menu-icon'),
        $closed = $('<span>').addClass('ui-icon ui-icon-circle-triangle-s').css('float', 'right'),
        $open = $('<span>').addClass('ui-icon ui-icon-circle-triangle-n').css('float', 'right').hide(),
        $count = $('<span>').addClass('criterion-count'),
        $legend = $('<p>').addClass('ui-state-default')
            .append($icon).append(criteria.value).append($count).append($closed).append($open),
        $criteria = $('<ul>').addClass('criteria').hide(),
        $fieldset = $('<div>').addClass('box level1').append($legend).append($criteria)

    $.each(getCriterionList(criteria), function (c, criterion) {
        var cId = 'c' + criterion.id,
            $label = $('<label>').append(criterion.value).attr('for', cId),
            $input = $('<input>').attr({
                id: cId, type: 'checkbox', value: criterion.id
            }).data('criterion', criterion).addClass('criterion'),
            $criterion = $('<li>').append($input).append($label)
        $criterionById[criterion.id] = $input
        $criteria.append($criterion)
    })

    // check the aggregates
    var checkedCount = 0;
    $.each(aggregates, function (o, aggregate) {
        checkedCount++
        $criterionById[aggregate.id].attr('checked', true)
    })
    $count.text(checkedCount)

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
    console.log("Create Recipent called..")
    function click() {
        dm4c.do_reveal_related_topic(recipient.id, 'show')
    }

    var $icon = dm4c.render.icon_link(recipient, click),
        $topic = dm4c.render.topic_link(recipient, click),
        $topicDiv = $('<div>').addClass(type).append($icon).append($topic),
        $removeDiv = $('<div>').addClass('remove-button'),
        $recipient = $('<div>').append($topicDiv).append($removeDiv)
    if (type === 'exclude') {
        $removeDiv.append(dm4c.ui.button({on_click: onRemoveExclude, icon: 'circle-plus'}))
    } else if (type === 'include') {
        $removeDiv.append(dm4c.ui.button({on_click: onRemoveInclude, icon: 'circle-minus'}))
    } else { // result
        $removeDiv.append(dm4c.ui.button({on_click: onExclude, icon: 'circle-minus'}))
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
    console.log("Refresh Recipients called..")
    var campaignId = dm4c.selected_object.id,
        recipients = dm4c.get_plugin('dm4.poemspace.plugin').getCampaignRecipients(campaignId),
        $recipients = $('#campaign' + campaignId + 'recipients').empty(),
        excludes = getRecipientsById(campaignId, 'dm4.poemspace.campaign.excl'),
        includesById = mapById(getRecipientsById(campaignId, 'dm4.poemspace.campaign.adds')),
        excludesById = mapById(excludes)

    // render count before merge
    $recipients.prev().text(recipients.length + ' Recipients')

    $.merge(recipients, excludes).sort(function (a, b) {
        return (a.value < b.value) ? -1 : (a.value > b.value) ? 1 : 0
    })

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
    console.log("Registering Criterion Change")
    $parent.on('change', 'input[type="checkbox"]', function () {
        var $input = $(this),
            $count = $('span.criterion-count', $input.parent().parent().parent()),
            criterion = $input.data('criterion')
        if ($input.is(':checked')) {
            console.log("Adding Criterion", criterion)
            addCriterion(campaign.id, criterion.id)
            $count.text(parseInt($count.text()) + 1)
        } else {
            console.log("Deleting Criterion", criterion)
            deleteCriterion(campaign.id, criterion.id)
            $count.text(parseInt($count.text()) - 1)
        }
        refreshRecipients()
    })
}

function getAggregates(topic, type) {
    var aggregates = {}
    $.each(topic.childs[type.uri] || [], function (t, aggregate) {
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
        var subject = campaign.childs['dm4.mail.subject']
        if (subject) {
            dm4c.render.field_label('Name')
            dm4c.render.page(subject.value)
        }
        dm4c.render.topic_associations(campaign.id)
    },
    render_form: function (campaign) {
        var name = campaign.childs['dm4.mail.subject'],
                $name = dm4c.render.input(name),
                $left = $('<div>').css({float: 'left', width: '49%'}),
                $right = $('<div>').css({float: 'right', width: '49%'}),
                $criteria = createCriteriaFieldsetList(campaign),
                $recipients = $('<div>').attr('id', 'campaign' + campaign.id + 'recipients')

        $criteria.on('click', 'p', function () {
            $('ul', $(this).parent()).toggle()
            $('span', $(this)).toggle()
        })

        dm4c.render.field_label('Name', $left)
        $left.append($name)
        dm4c.render.field_label('Criteria', $left)
        $left.append($criteria)

        dm4c.render.page($left)

        dm4c.render.field_label('Recipients', $right)
        dm4c.render.page($right)
        $right.append($recipients).append(dm4c.get_plugin('dm4.mail.plugin')
                .createCompletionField('Add', function include($item, item) {
                    var recipient = getRecipient(item.id, item.type_uri)
                    includeRecipient(campaign.id, recipient.id)
                    refreshRecipients()
                })
                )

        refreshRecipients()
        registerCriterionChange(campaign, $criteria, $recipients)

        return function () {
            dm4c.do_update_topic({
                id: campaign.id,
                childs: {
                    'dm4.mail.subject': $.trim($name.val()),
                    'dm4.time.modified': campaign.childs['dm4.time.modified']
                }
            })
            //dm4c.page_panel.refresh()
        }
    }
})
