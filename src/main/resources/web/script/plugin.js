/*global jQuery, dm4c*/

dm4c.add_plugin('dm4.poemspace.plugin', function () {

  this.getCriteriaTypes = function () {
    return dm4c.restc.request('GET', 'poemspace/criteria-types')
  }

  this.getCampaignRecipients = function (campaignId) {
    var uri = 'poemspace/campaign/' + campaignId + '/recipients'
    return dm4c.restc.request('GET', uri)
  }

  function saveAndWriteMail() {
    dm4c.page_panel.save()
    writeMail()
  }

  function writeMail() {
    var uri = 'poemspace/campaign/' + dm4c.selected_object.id + '/write',
      mail = dm4c.restc.request('POST', uri)
    dm4c.do_reveal_related_topic(mail.id)
    dm4c.show_topic(new Topic(mail), 'edit')
  }

  function createCriteria() {
    dm4c.ui.prompt('New Criteria', 'Name', 'Add', function (name) {
      var uri = 'poemspace/criteria/' + name,
        criteria = dm4c.restc.request('POST', uri)
      // FIXME update client type cache without another server interaction
      dm4c.do_update_topic_type(criteria)
      dm4c.show_topic(new Topic(criteria), 'edit')
    })
  }

  function isCampaignMail(mailId) {
    var campaigns = dm4c.restc.get_topic_related_topics(mailId, {
      assoc_type_uri: 'dm4.core.association',
      others_topic_type_uri: 'dm4.poemspace.campaign'
    }, null, null)
    return campaigns.total_count === 1 ? true : false
  }

  function mailRecipientCheck(mail) {
    if (isCampaignMail(mail.id)) {
      // TODO save count after each query and display it
      return $('<span>').text('Campaign recipient list...')
    }
  }

  // configure menu and type commands
  dm4c.add_listener('topic_commands', function (topic) {
    if (!dm4c.has_create_permission('dm4.poemspace.campaign')) {
      return
    }
    var commands = []
    if (topic.type_uri === 'dm4.poemspace.campaign') {
      commands.push({is_separator: true, context: 'context-menu'})
      commands.push({
        label: 'Write Mail',
        handler: writeMail,
        context: ['context-menu', 'detail-panel-show']
      })
      commands.push({
        label: 'Write Mail',
        handler: saveAndWriteMail,
        context: ['detail-panel-edit']
      })
    }
    return commands
  })

  dm4c.add_listener('post_refresh_create_menu', function (menu) {
    if (!dm4c.has_create_permission('dm4.poemspace.campaign')) {
      return
    }
    menu.add_separator()
    menu.add_item({ label: 'New Criteria', handler: createCriteria })
  })

  // TODO display count or some other additional information
  dm4c.add_listener('render_mail_recipients_info', mailRecipientCheck)
  dm4c.add_listener('render_mail_recipients_form', mailRecipientCheck)

})
