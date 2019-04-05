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
        client.queuedMessage = "";
        client.terminate();
        //Kill all our resources
        try{
            client.in.close();
            client.out.close();
            client.socket.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void sendToAll(String message){
        for(int i = 0; i < clients.size(); i++){
            clients.get(i).getOut().println(message);
            clients.get(i).getOut().flush();
        }
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
            clientList += clients.get(i).userName + ",";
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
                                    new PrintWriter(newClient.getOutputStream())
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
                            //If we have a queued message, send it and nullify it
                            if(clients.get(i).queuedMessage != ""){
                                sendToAll(clients.get(i).queuedMessage);
                                clients.get(i).queuedMessage = "";
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

    //Nested class holding client thread
    class ClientThread implements Runnable{

        //Are we still running
        private boolean running = true;

        volatile private Socket socket;
        volatile private BufferedReader in;
        volatile private PrintWriter out;
        volatile private String userName = "UNSET";
        //Each client has a queued message that we iterate through in the server thread
        //If the message isn't empty, we'll send it
        volatile private String queuedMessage = "";

        //Tells us if we've changed names since the last server update
        volatile boolean hasChangedName;

        //Our clients notify us when they die using this boolean
        volatile boolean isKill = false;

        public Socket getSocket() {
            return socket;
        }

        public void setSocket(Socket socket) {
            this.socket = socket;
        }

        public BufferedReader getIn() {
            return in;
        }

        public void setIn(BufferedReader in) {
            this.in = in;
        }

        public PrintWriter getOut() {
            return out;
        }

        public void setOut(PrintWriter out) {
            this.out = out;
        }

        public void terminate(){
            running = false;
        }

        public ClientThread(Socket socket, BufferedReader in, PrintWriter out) {
            this.socket = socket;
            this.in = in;
            this.out = out;
        }

        //Override the run method
        @Override
        public void run() {
            //For each client thread execute this loop
            while(running && !isKill){
                //Listen for input data
                try{
                    String data = in.readLine();
                    System.out.println("Got data from a client: " + data);
                    System.out.println(queuedMessage);
                    //Handle case for if we've recieved a !USER command
                    if(data.contains("!USER ") && data.substring(0,6).equals("!USER ")) {
                        this.hasChangedName = true;
                        System.out.println("Client has asked to change name");
                        //If we don't have a username, set it and notify the server
                        if (this.userName == "UNSET") {
                            queuedMessage = data.substring(6) + " has connected!";
                        } else {
                            queuedMessage = userName + " has changed their name to " + data.substring(6);
                        }
                        this.userName = data.substring(6);
                    }else if(data.contains("!CHALLENGE ") && data.substring(0,11).equals("!CHALLENGE ")){
                        //We've recieved a challenge request to challenge
                        //TODO:Implment challenge handling
                        System.out.println(this.userName + "has challenged a user");
                    }else if(data.equals("!QUIT")){
                        //We've recieved a quit command so we need to kill this thread
                        isKill = true;

                        //Notify everyone else that we have a kill message
                        queuedMessage = userName + " has quit the server!";
                    }else{
                        queuedMessage = userName + ": " + data;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
