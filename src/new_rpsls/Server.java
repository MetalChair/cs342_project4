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

    private void sendToAll(String message){
        for(int i = 0; i < clients.size(); i++){
            clients.get(i).getOut().println(message);
            clients.get(i).getOut().flush();
        }
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
                        for(int i = 0; i < clients.size(); i++){
                            //If we have a queued message, send it and nullify it
                            if(clients.get(i).queuedMessage != ""){
                                sendToAll(clients.get(i).queuedMessage);
                            }
                            clients.get(i).queuedMessage = "";
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

        volatile private Socket socket;
        volatile private BufferedReader in;
        volatile private PrintWriter out;
        volatile private String userName = "UNSET";
        //Each client has a queued message that we iterate through in the server thread
        //If the message isn't empty, we'll send it
        volatile private String queuedMessage = "";

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

        public ClientThread(Socket socket, BufferedReader in, PrintWriter out) {
            this.socket = socket;
            this.in = in;
            this.out = out;
        }

        //Override the run method
        @Override
        public void run() {
            //For each client thread execute this loop
            while(true){
                //Listen for input data
                try{
                    String data = in.readLine();
                    System.out.println("Got data from a client: " + data);
                    System.out.println(queuedMessage);
                    //Handle case for if we've recieved a !USER command
                    if(data.contains("!USER ") && data.substring(0,6).equals("!USER ")) {
                            System.out.println("Client has asked to change name");
                            //If we don't have a username, set it and notify the server
                            if (this.userName == "UNSET") {
                                queuedMessage = data.substring(6) + " has connected!";
                            } else {
                                queuedMessage = userName + " has changed their name to " + data.substring(6);
                            }
                            this.userName = data.substring(6);
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
