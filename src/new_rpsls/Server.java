package new_rpsls;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server extends Application{
    //The port that the server runs on
    private int port;
    private Stage stage;
    TextArea messages = new TextArea();
    //Hold the server socket
    volatile private ServerSocket server;

    //Hold all connected clients
    volatile private ArrayList<ClientThread> clients = new ArrayList<>();

    public static void main(String[] args){
        launch(args);
    }

    private void cullDeadConnection(ClientThread client){
        client.setQueuedMessage("");
        client.terminate();
        //Kill all our resources
        try{
            client.getIn().close();
            client.getOut().close();
            client.getSocket().close();
        }catch (Exception e){
            e.printStackTrace();
        }
        //Update all the other player's lists of currently connected users
        sendConnectedPlayersList(getConnectedPlayersList());
    }

    void sendToAll(String message){
        for(int i = 0; i < clients.size(); i++){
            clients.get(i).getOut().println(message);
            clients.get(i).getOut().flush();
        }
    }

    private void sendToUser(ClientThread user, String message){
        user.getOut().println(message);
        user.getOut().flush();
    }

    private ClientThread findByUsername(String name){
        for(int i = 0; i < clients.size(); i++){
            if(clients.get(i).getUserName().equals(name)){
                return clients.get(i);
            }
        }
        return null;
    }
    public void updatePlayerList(){
        sendConnectedPlayersList(getConnectedPlayersList());
    }
    private void sendConnectedPlayersList(String list){
        sendToAll(list);
    }

    //If a player changes names or quits,
    //We need to inform all players on the server
    //By giving them a new list of players connected
    private String getConnectedPlayersList(){
        String clientList = "!CLIENTS ";
        for(int i =0; i < clients.size(); i++){
            clientList += clients.get(i).getUserName() + ";" + clients.get(i).getScore() + ",";
            //We've acknowledged the name change at this point
            //So we tell the thread that the change as been reflected
            clients.get(i).hasChangedName = false;
        }
        return clientList;
    }

    private void startServer(int port) throws Exception{
        this.server = new ServerSocket(port);

    }

    private Parent serverSetUpScreen(){
        Text portChoice = new Text("Choose the port to listen into");
        TextField input = new TextField();
        Button startButton = new Button("Start Server");
        startButton.setOnAction(event ->{
           try {
               this.port = Integer.parseInt(input.getText());
               startServer(this.port);
               this.stage.setScene(new Scene(startGame()));
               this.stage.show();
           } catch (NumberFormatException e){
               e.printStackTrace();
               portChoice.setText("Only use numbers please.");
           } catch (Exception e){
               e.printStackTrace();
               portChoice.setText("Something went wrong...");
           }
        });
        VBox root = new VBox(20, portChoice, input, startButton);
        return root;
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        this.stage = primaryStage;
        this.stage.setScene(new Scene(serverSetUpScreen()));
        this.stage.show();
    }

    public Parent startGame() throws Exception {
        //Create the server
       Button endServerButton = new Button("End Server");
       endServerButton.setOnAction(event -> {
           sendToAll("!QUIT");
       });
       messages.setPrefHeight(550);
       messages.appendText("Listening in on port: " + this.port + "\n");
       messages.setEditable(false);
        HBox topHbox = new HBox(20, endServerButton);
        VBox root = new VBox(20, topHbox, messages);
        root.setPrefSize(600, 600);
        //Thread that listens for new server connections
        //We need a new thread to just listen and accept clients
        //Yes nested threads are confusing
        new Thread(()->{
            try{
                while (true){
                        //Accept new clients
                        Socket newClient = server.accept();
                        synchronized (clients){
                            //Add the new client to the list of connected clients
                            ClientThread temp = new ClientThread(
                                    newClient,
                                    new BufferedReader(new InputStreamReader(newClient.getInputStream())),
                                    new PrintWriter(newClient.getOutputStream(),true)
                            );

                            clients.add(temp);
                            new Thread(temp).start();
                            System.out.println(clients.size());
                            //We got a new client so we should update player lists
                            sendConnectedPlayersList(getConnectedPlayersList());
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }).start();

        //Thread that handles messages recieved
        new Thread(()->{
            try{
                //Sit and listen for connections
                while(true){
                    synchronized (clients){
                        //Look through all the clients to see if they have a queued message
                        //Or if we need to rebuild the connected players list
                        for(int i = 0; i < clients.size(); i++){
                            String currMessage = clients.get(i).getQueuedMessage();
                            //If our client has been challenged or is in a lobby
                            //We want to first ensure they are not sending a message
                            //That is meant only for that game lobby
                            if(clients.get(i).isInLobby() || clients.get(i).getChallenger() != null){
                                //Figure out if the current player is in the lobby or if the challenger is
                                Lobby lobby =
                                (clients.get(i).isInLobby()) ?
                                clients.get(i).getCurrentLobby() :
                                clients.get(i).getChallenger().getCurrentLobby();

                                Lobby.playerCommand command = lobby.proccessInput(currMessage,clients.get(i), messages);

                                if(command != Lobby.playerCommand.NONE){
                                    //This loop only cares if we have a command that should be handled by the lobby
                                    //all other player commands will be handled in the lobby class
                                    break;
                                }
                            }
                            //If we have a queued message, send it and nullify it
                            if(!currMessage.equals("")){
                                //Handle if we got a challenge command
                                if(currMessage.contains("!CHALLENGE")) {
                                    //Splice to get the username
                                    String challengedUser = currMessage.substring(11);
                                    ClientThread challenge = findByUsername(challengedUser);
                                    if (challenge != null) {
                                        //Check we're not trying to challenge ourself
                                        if (challenge == clients.get(i)) {
                                            final int temp = i;
                                            //Notify ourselves if we do
                                            sendToUser(clients.get(i), "!CHALLENGE You can't challenge yourself. Idiot.");
                                            Platform.runLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    messages.appendText(clients.get(temp).getUserName() + " tried to challenge himself to a game. Loser. \n");
                                                }
                                            });
                                        } else if (challenge.getChallenger() != null || challenge.getCurrentLobby() != null) {
                                            final int temp = i;
                                            //If the user has already been challenged, tell our client
                                            sendToUser(clients.get(i), "!CHALLENGE This user has a pending challenge already! Wait for a minute");
                                        } else {
                                            final int temp = i;
                                            //Else, setup the challenge
                                            sendToUser(challenge, "!CHALLENGE " + clients.get(i).getUserName() + " has challenged you to a game!");
                                            Platform.runLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    messages.appendText("!CHALLENGE " + clients.get(temp).getUserName() + " has been challenged to a game!\n");
                                                }
                                            });
                                            //We now put the user into a new lobby
                                            new Lobby(clients.get(i), challenge, this);

                                            //Notify the challenger that we have received their request
                                            clients.get(i).getOut().println("!CHALLENGE Awaiting response from " + challengedUser);
                                            Platform.runLater(new Runnable() {
                                                @Override
                                                public void run() {
                                                    messages.appendText(clients.get(temp).getUserName() + " challenged " + challengedUser + ". Awaiting response. \n");
                                                }
                                            });
                                            clients.get(i).getOut().flush();

                                            challenge.setChallenger(clients.get(i));
                                            //TODO:this bit
                                        }
                                    } else {
                                        final int temp = i;
                                        // else send to the client that this user does not exist on the server
                                        sendToUser(clients.get(i), "!CHALLENGE This user does not exist on this server");
                                        Platform.runLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                messages.appendText(clients.get(temp).getUserName() + " tried to challenge a non-existing user. \n");
                                            }
                                        });
                                    }
                                }else if(currMessage.contains("!GETCLIENTLIST")){
                                    sendConnectedPlayersList(getConnectedPlayersList());
                                }else{
                                    //If the client message isn't a challenge, just send it to everyone
                                    //This is essentially IRC function
                                    //We add a check to ensure users aren't using our keywords
                                    //This could be changed to use objects but... idc
                                    if( !currMessage.contains("!STARTGAME")     &&
                                        !currMessage.contains("!GAMELOG")       &&
                                        !currMessage.contains("!TEXTLOG")       &&
                                        !currMessage.contains("!MOVE")          &&
                                        !currMessage.contains("!ENDLOBBY")      &&
                                        !currMessage.contains("!QUIT")          &&
                                        !currMessage.contains("!SHOWRESETBOX")
                                    ){
                                        sendToAll(currMessage);
                                    }else{
                                        final int temp = i;
                                        clients.get(i).getOut().println("Server: You can't use that command!");
                                        Platform.runLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                messages.appendText(clients.get(temp).getUserName() + " tried to use an unknown command. \n");
                                            }
                                        });
                                        clients.get(i).getOut().flush();
                                    }
                                }
                                //Nullify any queued messages
                                clients.get(i).setQueuedMessage("");

                            //If we changed a name, rebuild our list and notify everyone
                            }else if(clients.get(i).hasChangedName){
                                System.out.println("A client has changed names, we need to reflect this");
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        messages.appendText("A client has either connected or changed names \n");
                                    }
                                });
                                sendConnectedPlayersList(getConnectedPlayersList());
                            }
                            //If we lost a connection, cull the population
                            else if(clients.get(i).isKill){
                                cullDeadConnection(clients.get(i));
                                clients.remove(i);
                                sendConnectedPlayersList(getConnectedPlayersList());
                            }
                        }
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }).start();
        return root;
    }

}
class Lobby {
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

    //Hold the server
    Server server;



    public Lobby(ClientThread client1, ClientThread waitingClient, Server server){
        this.client1 = client1;
        //Take the user, tell them their in a lobby
        //Also add a reference to this lobby
        this.client1.setInLobby(true);
        this.client1.setCurrentLobby(this);

        //Reference to the client we are waiting on
        this.waitingClient = waitingClient;

        //Add reference to server
        this.server = server;
    }

    /*
    When our clients are in a lobby, we intercept their messages and process
    them here first. If the message does not contain information about
    starting/participating in a match, we return false and the loop in the
    server thread returns to proccessing the input
    */
    playerCommand proccessInput(String input, ClientThread sender, TextArea messages){
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
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        messages.appendText("!CHALLENGE " + sender.getUserName() + " has accepted " + client1.getUserName() + "'s challenge.\n");
                    }
                });
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
            final int m = move;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    messages.appendText("!TEXTLOG " + sender.getUserName() + " played " + Client.intToPlayString(m) + "\n");
                }
            });
            command = playerCommand.MOVE;

            if(client1Move != 0 && client2Move != 0){
                //If we have both inputs, determine a winner
                determineWinnerAndAddScore(messages);
            }
            sender.setQueuedMessage("");
        }else if(input.contains("!REPLAY")){

            //Set this so the server doesn't process our input
            command = playerCommand.MOVE;

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

        }else if(input.contains("!QUITLOBBY")){
            destoryLobby(sender);
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

        client1.setQueuedMessage("");
        client2.setQueuedMessage("");
    }

    //Determine the winner of the game
    void determineWinnerAndAddScore(TextArea messages) {
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
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    messages.appendText("!GAMELOG " + client1.getUserName() + " wins: " + client1Wins + "\n");
                    messages.appendText("!GAMELOG " + client2.getUserName() + " wins: " + client2Wins + "\n");
                }
            });
            client1Wins++;
        }else if(client2 == winner){
            client2.getOut().println("!GAMELOG " + client2Move + " " + client1Move + " 1");
            client1.getOut().println("!GAMELOG " + client1Move + " " + client2Move + " 0");
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    messages.appendText("!GAMELOG " + client1.getUserName() + " wins: " + client1Wins + "\n");
                    messages.appendText("!GAMELOG " + client2.getUserName() + " wins: " + client2Wins + "\n");
                }
            });
            client2Wins++;
        }


        client1Move = 0;
        client2Move = 0;

        ClientThread threeWinner = checkForThreeWins();
        if(threeWinner != null){
            //Notify the winner they one the game
            threeWinner.getOut().println("!TEXTLOG You WON! Would you like to play again?");
            threeWinner.getOut().flush();
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    messages.appendText(threeWinner.getUserName() + " WON! would they like to play again? \n");
                }
            });

            //Show them the reset controls
            winner.getOut().println("!SHOWRESETBOX");
            winner.getOut().flush();

            //Add to their score
            threeWinner.increaseScoreByOne();

            //Tell the loser they lost
            loser.getOut().println("!TEXTLOG You LOST! Would you like to play again?");
            loser.getOut().flush();
            final ClientThread l = loser;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    messages.appendText(l.getUserName() + " LOST! Would you like to play again? \n");
                }
            });
            //Show them the reset controls
            loser.getOut().println("!SHOWRESETBOX");
            loser.getOut().flush();

            client1Wins = 0;
            client2Wins = 0;

            server.sendToAll(winner.getUserName() + " has beaten " + loser.getUserName() + " in a game!");
            final ClientThread w = winner;
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    messages.appendText(w.getUserName() + " has beaten " + l.getUserName() + " in a game! \n");
                }
            });
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
