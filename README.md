# Poemspace - DeepaMehta 4 Plugin

Poemspace is an application for contact management and email distribution
in the field of arts and culture. A mailing is addressed to a
arbitrary combination of single contacts and dynamic filter results.
Contacts are categorized by e.g. arts genre and kind of venue.
The categories themself are configurable by the user.

## Requirements

  * [DeepaMehta 4](http://github.com/jri/deepamehta) 4.7
  * [Images Plugin](http://github.com/dgf/dm4-images) 0.9.6
  * [Mail Plugin](http://github.com/dgf/dm4-mail) 0.3.1

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

### changelog

**0.3**, Nov 17, 2016

- Adapted to be compatible with DeepaMehta 4.8
- A usability issue exists cause by some cache:<br/>
  After updating criterias to be included in a "Campaign" one needs to select any other topic and re-select the "Campaign" to see tis current state. This mis-representation of selected criterias of a campaign also effects subsequent "Edits" of a Campaign (as included criterias may appear un-checked).

**0.2**, Apr 26, 2016

- Upgraded to be compatible with DeepaMehta 4.7
- Fixes: Sending campaign mail works before criterias where edited

