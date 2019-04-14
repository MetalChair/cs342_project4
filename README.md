# cs342_project4
A cool hangout spot for net-savvy folks to play rad games of rock, paper, scissors, lizard, Spock! Groovy!

## Here are the bugs we should take a look at:
### TRIPLE PRIORITY IMPORTANT
2. The server has no GUI so far. I'd like to leave a means of running the server in CMD mode by using launch params but we still need to implement the gui
3. I've been chasing this weird bug wherein everything fucntions as normal but the GUI doesn't seem to update properly. No errors occur and for all intents and purposes it appears to be a JAVAFX issue so we'll see
##### less important stuff
1. When the player list is sent to the client, I'd like to pad all the strings it recieves to be of the same length. Currently the alignemnt is off and it looks a little cheap
2. When a user challenges a user that isn't on the server, there is no notification that their challenge failed. Nothing bad happens because of this, it's just confusing to the end user
3. We should probably limit player names to 64 characters...
4. I'm not 100% the game logic is perfect, it can be tested more
