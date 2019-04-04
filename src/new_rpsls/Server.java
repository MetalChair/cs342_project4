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
    private ServerSocket server;

    //Hold all connected clients
    private ArrayList<ClientThread> clients = new ArrayList<>();

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
        //Thread that listens for new server connections
        new Thread(()->{
            try{
                //Create the server
                this.server = new ServerSocket(5555);

                //Sit and listen for connections
                while(true){
                    //Accept new clients
                    Socket newClient = server.accept();

                    //Add the new client to the list of connected clients
                    clients.add(new ClientThread(
                            newClient,
                            new BufferedReader(new InputStreamReader(newClient.getInputStream())),
                            new PrintWriter(newClient.getOutputStream())
                    ));

                    //DEBUG
                    System.out.println("A new client connected");
                    sendToAll("A new client has connected!");

                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    //Nested class holding client thread
    class ClientThread implements Runnable{

        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

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
            }
        }
    }
}
