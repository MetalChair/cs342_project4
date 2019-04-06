package new_rpsls;

public class Lobby {
    //Holds potential states we can get from input
    public enum playerCommand{
        CANCEL,ACCEPT,NONE;
    }

    //Hold the two clients participating in the lobby
    ClientThread client1;
    ClientThread client2;

    //Hold a client we are potentially waiting on
    ClientThread waitingClient;



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
        }
        return command;
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
}
