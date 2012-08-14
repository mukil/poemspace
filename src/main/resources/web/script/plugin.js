/*global jQuery, dm4c*/

dm4c.add_plugin('dm4.poemspace.plugin', function () {

  dm4c.restc.get_criteria_types = function () {
    return this.request('GET', '/poemspace/criteria-types')
  }

  dm4c.add_listener('post_refresh_create_menu', function (menu) {
    menu.add_separator()
    menu.add_item({
      label: 'New Criteria',
      handler: function () {
        alert('create a new criteria')
        // @todo call server method to:
        //  * create topic type
        //  * associate with criteria singleton
        //  * add association definition to person and institution
      }
    })
  })

})
