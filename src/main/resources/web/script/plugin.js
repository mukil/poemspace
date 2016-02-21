/*global jQuery, dm4c*/

dm4c.add_plugin('dm4.poemspace.plugin', function () {

    this.getCriteriaTypes = function () {
        return dm4c.restc.request('GET', '/poemspace/criteria-types')
    }

    this.getCampaignRecipients = function (campaignId) {
        console.log("Get Campaign Recipients..")
        var uri = '/poemspace/campaign/' + campaignId + '/recipients'
        return dm4c.restc.request('GET', uri)
    }

    function getCampaignRecipientCount(campaignId) {
        var campaign = dm4c.restc.get_topic_by_id(campaignId, true),
            count = campaign.childs['dm4.poemspace.campaign.count']
        if (count) {
            return count.value || 0
        } else {
            return 0
        }
    }

    function getCampaignsOfMail(mailId) {
        return dm4c.restc.get_topic_related_topics(mailId, {
            assoc_type_uri: 'dm4.core.association',
            others_topic_type_uri: 'dm4.poemspace.campaign'
        })
    }

    function isCampaignMail(mailId) {
        var campaigns = getCampaignsOfMail(mailId)
        if (campaigns.total_count === 0) {
            return false
        } else {
            return true
        }
    }

    function getMailRecipients(topicId) {
        return dm4c.restc.get_topic_related_topics(topicId, {assoc_type_uri: 'dm4.mail.recipient'})
    }

    function reloadCriteriaCache() {
        return dm4c.restc.request('POST', '/poemspace/criteria-reload')
    }

    function saveAndStartCampaign() {
        dm4c.page_panel.save()
        startCampaign()
    }

    function startCampaign() {
        var sentDate = dm4c.selected_object.childs['dm4.mail.date']
        // copy mail if send before or a recipient is associated
        if ((sentDate && $.isEmptyObject(sentDate.value) === false) // send?
                || getMailRecipients(dm4c.selected_object.id).total_count > 0) { // associated recipients?
            var uri = '/mail/' + dm4c.selected_object.id + '/copy?recipients=false',
                    mail = dm4c.restc.request('POST', uri)
            dm4c.show_topic(new Topic(mail), 'show')
        }
        var campaign = dm4c.restc.request('PUT', '/poemspace/mail/' + dm4c.selected_object.id + '/start')
        dm4c.do_reveal_related_topic(campaign.id, 'show')
        dm4c.show_topic(new Topic(campaign), 'edit')
    }

    function sendCampaignMail() {
        if (isCampaignMail(dm4c.selected_object.id)) {
            return dm4c.restc.request('PUT', '/poemspace/mail/' + dm4c.selected_object.id + '/send')
        }
    }

    function copyCampaignMail() {
        var campaigns = getCampaignsOfMail(dm4c.selected_object.id)
        if (campaigns.total_count > 0) { // campaign mail
            var mail = dm4c.restc.request('POST', '/mail/' + dm4c.selected_object.id + '/copy?recipients=false')
            dm4c.restc.create_association({
                type_uri: 'dm4.core.association',
                role_1: {topic_id: mail.id, role_type_uri: 'dm4.core.default'},
                role_2: {topic_id: campaigns.items[0].id, role_type_uri: 'dm4.core.default'}
            })
            return mail
        }
    }

    function showCampaignMail() {
        var campaigns = getCampaignsOfMail(dm4c.selected_object.id)
        if (campaigns.total_count > 0) { // campaign mail
            dm4c.do_reveal_related_topic(campaigns.items[0].id, 'show')
        }
    }

    function createCriteria() {
        dm4c.ui.prompt('New Criteria', 'Name', 'Add', function (name) {
            var uri = '/poemspace/criteria/' + name,
                criteria = dm4c.restc.request('POST', uri)
            // FIXME update client type cache without another server interaction
            dm4c.do_update_topic_type(criteria)
            dm4c.show_topic(new Topic(criteria), 'edit')
        })
    }


    function renderMailRecipients() {
        var campaigns = getCampaignsOfMail(dm4c.selected_object.id)
        if (campaigns.total_count > 0) { // campaign mail
            var recipients = getMailRecipients(dm4c.selected_object.id)
            if (recipients.total_count > 0) { // campaign mail with recipients => sended
                return $('<span>').text(recipients.total_count + ' recipients')
            } else {
                return $('<span>').text(getCampaignRecipientCount(campaigns.items[0].id) + ' recipients')
            }
        }
    }

    // configure menu and type commands
    dm4c.add_listener('topic_commands', function (topic) {
        if (!dm4c.has_create_permission_for_topic_type('dm4.poemspace.campaign')) {
            return
        }
        var commands = []
        if (topic.type_uri === 'dm4.mail' && isCampaignMail(topic.id) === false) {
            commands.push({is_separator: true, context: 'context-menu'})
            commands.push({
                label: 'Start Campaign',
                handler: startCampaign,
                context: ['context-menu', 'detail-panel-show']
            })
            commands.push({
                label: 'Start Campaign',
                handler: saveAndStartCampaign,
                context: ['detail-panel-edit']
            })
        }
        if (topic.uri === 'com.poemspace.dm4-poemspace') {
            commands.push({
                label: 'Reload Criteria Cache',
                handler: reloadCriteriaCache,
                context: ['context-menu', 'detail-panel-show']
            })
        }
        return commands
    })

    dm4c.add_listener('render_mail_recipients', renderMailRecipients)
    dm4c.add_listener('send_mail', sendCampaignMail)
    dm4c.add_listener('copy_mail', copyCampaignMail)
    dm4c.add_listener('show_mail', showCampaignMail)

    dm4c.add_listener('post_refresh_create_menu', function (menu) {
        if (!dm4c.has_create_permission_for_topic_type('dm4.poemspace.campaign')) {
            return
        }
        menu.add_separator()
        menu.add_item({label: 'New Criteria', handler: createCriteria})
    })

})
