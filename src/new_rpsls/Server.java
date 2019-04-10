package new_rpsls;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server extends Application{
    //The port that the server runs on
    private int port;

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
    private void sendConnectedPlayersList(String list){
        sendToAll(list);
    }

    //If a player changes names or quits,
    //We need to inform all players on the server
    //By giving them a new list of players connected
    private String getConnectedPlayersList(){
        String clientList = "!CLIENTS ";
        for(int i =0; i < clients.size(); i++){
            clientList += clients.get(i).getUserName() + ",";
            //We've acknowledged the name change at this point
            //So we tell the thread that the change as been reflected
            clients.get(i).hasChangedName = false;
        }
        return clientList;
    }

    //Implement the GUI
    @Override
    public void start(Stage primaryStage) throws Exception {
        //Create the server
        this.server = new ServerSocket(5555);

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

                                Lobby.playerCommand command = lobby.proccessInput(currMessage,clients.get(i));

                                if(command != Lobby.playerCommand.NONE){
                                    //This loop only cares if we have a command that should be handled by the lobby
                                    //all other player commands will be handled in the lobby class
                                    break;
                                }
                            }
                            //If we have a queued message, send it and nullify it
                            if(currMessage != ""){
                                //Handle if we got a challenge command
                                if(currMessage.contains("!CHALLENGE")){
                                    //Splice to get the username
                                    String challengedUser = currMessage.substring(11);
                                    ClientThread challenge = findByUsername(challengedUser);
                                    if(challenge != null){
                                        //Check we're not trying to challenge ourself
                                        if(challenge == clients.get(i)){
                                            //Notify ourselves if we do
                                            sendToUser(clients.get(i), "!CHALLENGE You can't challenge yourself. Idiot.");
                                        }else if(challenge.getChallenger() != null || challenge.getCurrentLobby() != null){
                                            //If the user has already been challenged, tell our client
                                            sendToUser(clients.get(i), "!CHALLENGE This user has a pending challenge already! Wait for a minute");
                                        }else{
                                            //Else, setup the challenge
                                            sendToUser(challenge,"!CHALLENGE " + clients.get(i).getUserName() + " has challenged you to a game!");
                                            //We now put the user into a new lobby
                                            new Lobby(clients.get(i),challenge);

                                            //Notify the challenger that we have recieved their request
                                            clients.get(i).getOut().println("!CHALLENGE Awaiting response from " + challengedUser);
                                            clients.get(i).getOut().flush();

                                            challenge.setChallenger(clients.get(i));
                                            //TODO:this bit
                                        }
                                    }
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
                                        clients.get(i).getOut().println("Server: You can't use that command!");
                                        clients.get(i).getOut().flush();
                                    }
                                }
                                //Nullify any queued messages
                                clients.get(i).setQueuedMessage("");

                            //If we changed a name, rebuild our list and notify everyone
                            }else if(clients.get(i).hasChangedName){
                                System.out.println("A client has changed names, we need to reflect this");
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
    }
}
