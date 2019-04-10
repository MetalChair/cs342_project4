package new_rpsls;

public class Lobby {
    //Holds potential states we can get from input
    public enum playerCommand{
        CANCEL,ACCEPT,NONE,MOVE;
    }

    //Hold the two clients participating in the lobby
    private ClientThread client1;
    private ClientThread client2;

    //Holds the moves for the clients
    private int client1Move = 0;
    private int client2Move = 0;

    //Holds the number of wins each player has
    private int client1Wins = 0;
    private int client2Wins = 0;

    //Has the client rereadied
    private boolean client1Ready = false;
    private boolean client2Ready = false;

    //Hold moves for users

    //Hold a client we are potentially waiting on
    private ClientThread waitingClient;

    private boolean gameIsActive = false;



    public Lobby(ClientThread client1, ClientThread waitingClient){
        this.client1 = client1;
        //Take the user, tell them their in a lobby
        //Also add a reference to this lobby
        this.client1.setInLobby(true);
        this.client1.setCurrentLobby(this);

        //Reference to the client we are waiting on
        this.waitingClient = waitingClient;
    }

    /*
    When our clients are in a lobby, we intercept their messages and process
    them here first. If the message does not contain information about
    starting/participating in a match, we return false and the loop in the
    server thread returns to proccessing the input
    */
    playerCommand proccessInput(String input, ClientThread sender){
        playerCommand command = playerCommand.NONE;
        if(input.contains("!CANCEL")){
            command = playerCommand.CANCEL;
            destroyLobbyBeforeAcceptance();
            //clear the queued messages of the sender
            sender.setQueuedMessage("");
        }else if(input.contains("!ACCEPT")){
            //Handle accepting the lobby request
            //Player 1 should already be in the lobby
            //So we only worry about player 2
            if(sender == waitingClient){
                System.out.println("Client has accepted the challenge");
                command = playerCommand.ACCEPT;
                sender.setCurrentLobby(this);
                sender.setInLobby(true);
                client1.getOut().println("!CHALLENGE " + sender.getUserName() + " has accepted your challenge");
                client1.getOut().flush();
                this.client2 = sender;
                //nullfiy the accepter's message
                client2.setQueuedMessage("");
                //We should start our game here
                startGame();
            }
        }else if(input.contains("!MOVE") && gameIsActive){
            int move = 0;
            move = Integer.parseInt(input.replaceAll("[\\D]", ""));
            //Determine who sent it, set their moves
            if(sender == client1){
                client1Move = move;
            }else if(sender == client2){
                client2Move = move;
            }

            //Send the play value to the user
            sender.getOut().println("!TEXTLOG You played " + Client.intToPlayString(move));

            command = playerCommand.MOVE;

            if(client1Move != 0 && client2Move != 0){
                //If we have both inputs, determine a winner
                determineWinnerAndAddScore();
            }
            sender.setQueuedMessage("");
        }else if(input.contains("!REPLAY")){

            //Find who sent it and ready them up
            if(sender == client1){
                client1Ready = true;
            }

            if(sender == client2){
                client2Ready = true;
            }

            if(client1Ready && client2Ready){
                client1Ready = false;
                client2Ready = false;
                startGame();
            }

        }else if(input.contains("!QUIT")){

        }
        return command;
    }

    //Destroy the lobby
    private void destoryLobby(ClientThread initiator){
        client1.getOut().println("!ENDLOBBY");
        client1.getOut().flush();
        client2.getOut().println("!ENDLOBBY");
        client2.getOut().flush();

        client1.resetLobbyStatus();
        client2.resetLobbyStatus();
    }

    //Determine the winner of the game
    private void determineWinnerAndAddScore() {
        //The games moves are as follows
        /*
            1: rock
            2: scissors
            3. paper
            4. lizard
            5. spock
         */
        ClientThread loser = null;
        ClientThread winner = null;
        //Handle rock > scissors
        //handle scissors > paper
        //Handle paper > rock
        //handle scissors > lizard
        //handle lizard > spock
        //handle spock > scissors
        //handle paper > spock
        //handle spock > rock
        //handle rock > lizard
        //handle lizard > paper
        if (client1Move == 1 && client2Move == 2) {
            winner = client1;
            loser = client2;
        } else if (client2Move == 1 && client1Move == 2) {
            winner = client2;
            loser = client1;
        } else if (client1Move == 2 && client2Move == 3) {
            winner = client1;
            loser = client2;
        } else if (client2Move == 2 && client1Move == 3) {
            winner = client2;
            loser = client1;
        } else if (client1Move == 3 && client2Move == 1) {
            winner = client1;
            loser = client2;
        } else if (client2Move == 3 && client1Move == 1) {
            winner = client2;
            loser = client1;
        } else if (client1Move == 2 && client2Move == 4) {
            winner = client1;
            loser = client2;
        } else if (client2Move == 2 && client1Move == 4) {
            winner = client2;
            loser = client1;
        } else if (client1Move == 4 && client2Move == 5) {
            winner = client1;
            loser = client2;
        } else if (client2Move == 4 && client1Move == 5) {
            winner = client2;
            loser = client1;
        } else if (client1Move == 5 && client2Move == 2) {
            winner = client1;
            loser = client2;
        } else if (client2Move == 5 && client1Move == 2) {
            winner = client2;
            loser = client1;
        } else if (client1Move == 3 && client2Move == 5) {
            winner = client1;
            loser = client2;
        } else if (client2Move == 3 && client1Move == 5) {
            winner = client2;
            loser = client1;
        } else if (client1Move == 5 && client2Move == 1) {
            winner = client1;
            loser = client2;
        } else if (client2Move == 5 && client1Move == 1) {
            winner = client2;
            loser = client1;
        } else if (client1Move == 1 && client2Move == 4) {
            winner = client1;
            loser = client2;
        } else if (client2Move == 1 && client1Move == 4) {
            winner = client2;
            loser = client1;
        } else if (client1Move == 4 && client2Move == 3) {
            winner = client1;
            loser = client2;
        } else if (client2Move == 4 && client1Move == 3) {
            winner = client2;
            loser = client1;
        }else{
            client1.getOut().println("!GAMELOG " + client1Move + " " + client2Move + " 2");
            client2.getOut().println("!GAMELOG " + client2Move + " " + client1Move + " 2");
        }

        //Send a string that will create a new element for the gamelog in each client
        if(client1 == winner){
            client1.getOut().println("!GAMELOG " + client1Move + " " + client2Move + " 1");
            client2.getOut().println("!GAMELOG " + client2Move + " " + client1Move + " 0");
            client1Wins++;
        }else if(client2 == winner){
            client2.getOut().println("!GAMELOG " + client2Move + " " + client1Move + " 1");
            client1.getOut().println("!GAMELOG " + client1Move + " " + client2Move + " 0");
            client2Wins++;
        }


        client1Move = 0;
        client2Move = 0;

        ClientThread threeWinner = checkForThreeWins();
        if(threeWinner != null){
            //Notify the winner they one the game
            threeWinner.getOut().println("!TEXTLOG You WON! Would you like to play again?");
            threeWinner.getOut().flush();

            //Show them the reset controls
            winner.getOut().println("!SHOWRESETBOX");
            winner.getOut().flush();

            //Add to their score
            threeWinner.increaseScoreByOne();

            //Tell the loser they lost
            loser.getOut().println("!TEXTLOG You LOST! Would you like to play again?");
            loser.getOut().flush();

            //Show them the reset controls
            loser.getOut().println("!SHOWRESETBOX");
            loser.getOut().flush();

            client1Wins = 0;
            client2Wins = 0;


            gameIsActive = false;
        }


    }

    ClientThread checkForThreeWins(){
        if(client1Wins == 3){
            return client1;
        }else if(client2Wins == 3){
            return client2;
        }
        return null;
    }

    //Destroys the lobby when one player has challenged another
    //but the other player hasn't accepted
    void destroyLobbyBeforeAcceptance(){
        client1.getOut().println("!CHALLENGE You cancelled your challenge you wuss!");
        client1.getOut().flush();

        //reset all of the lobby status variables
        client1.resetLobbyStatus();
        waitingClient.resetLobbyStatus();

        waitingClient.getOut().println("!CHALLENGE " + client1.getUserName() + " has cancelled their challenge against you!");
        waitingClient.getOut().flush();
    }
    void startGame(){
        client1.getOut().println("!STARTGAME");
        client1.getOut().flush();

        client2.getOut().println("!STARTGAME");
        client2.getOut().flush();

        gameIsActive = true;

    }
}
