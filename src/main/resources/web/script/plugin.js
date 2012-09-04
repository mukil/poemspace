/*global jQuery, dm4c*/

dm4c.add_plugin('dm4.poemspace.plugin', function () {

  this.getCriteriaTypes = function () {
    return dm4c.restc.request('GET', '/poemspace/criteria-types')
  }

  this.getCampaignRecipients = function (campaignId) {
    var uri = '/poemspace/campaign/' + campaignId + '/recipients'
    return dm4c.restc.request('GET', uri)
  }

  function writeMail() {
    var uri = '/poemspace/campaign/' + dm4c.selected_object.id + '/write'
    dm4c.do_reveal_related_topic(dm4c.restc.request('POST', uri).id)
  }

  // configure menu and type commands
  dm4c.add_listener('topic_commands', function (topic) {
    if (!dm4c.has_create_permission('dm4.poemspace.campaign')) {
      return
    }
    var commands = [];
    if (topic.type_uri === 'dm4.poemspace.campaign') {
      commands.push({
        label: 'Write Mail',
        handler: writeMail,
        context: ['context-menu', 'detail-panel-show']
      })
    }
    return commands
  })

  dm4c.add_listener('post_refresh_create_menu', function (menu) {
    if (!dm4c.has_create_permission('dm4.poemspace.campaign')) {
      return
    }
    menu.add_separator()
    menu.add_item({
      label: 'New Criteria',
      handler: function () {
        alert('create a new criteria')
        // TODO call server method to:
        //  * create topic type
        //  * associate with criteria singleton
        //  * add association definition to person and institution
      }
    })
  })

})
