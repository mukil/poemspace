# Poemspace - DeepaMehta 4 Plugin

Poemspace is an application for contact management and email distribution
in the field of arts and culture. A mailing is addressed to a
arbitrary combination of single contacts and dynamic filter results.
Contacts are categorized by e.g. arts genre and kind of venue.
The categories themself are configurable by the user.

## Requirements

  * [DeepaMehta 4](http://github.com/jri/deepamehta) 4.8
  * [Images Plugin](http://github.com/dgf/dm4-images) 0.9.10
  * [Mail Plugin](http://github.com/dgf/dm4-mail) 0.3.3

## Usage

![mail campaign map](https://github.com/dgf/poemspace/raw/master/screenshot.png)

### supported distribution workflow

  * create and design a HTML *Mail*
  * *Send* this *Mail* to some test recipients
  * use the *Mail* to *Start Campaign*
  * *Edit* the *Mail Campaign* and filter the *Recipient* list to your needs
  * really *Send* this *Mail* to all campaign recipients

### send again

when the *Send Again* action is invoked on a campaign mail,
then the new *Mail* is also linked with the *Mail Campaign* and can
directly be manipulated and resend to the actual campaign recipients.

### create a criteria

use the *New Criteria* action from the create menu and enter a unique name
to create a new *Criteria Topic Type*

After each criteria modification, the cache and webclient must be reloaded.
So you have to reveal the *Poem Space* plugin to
call the *Reload Criteria Cache* command and in addition your have to
reload the webclient page in the web browser.

## plugin development

TBD. You can start adapting the source code of this plugin by following the paragraph on plugin development in this [README](https://github.com/mukil/dm4-kiezatlas-angebote#usage--development) while replacing the names of the bundles this plugin depends on (dm4-images, dm4-mail).

### changelog

**0.3**, Dec 20, 2016

- Adapted to be compatible with DeepaMehta 4.8
- A usability issue exists cause by the dm4-cache module:<br/>
  After updating criterias to be included in a "Campaign" one _needs_ to select any other topic and re-select the "Campaign" to see and edit its up-to-date state. Disabling the dm4-cache module should work around this issue.

**0.2**, Apr 26, 2016

- Upgraded to be compatible with DeepaMehta 4.7
- Fixes: Sending campaign mail works before criterias where edited

