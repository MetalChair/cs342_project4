package DEPRECATED;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server extends Application {

    private ServerSocket server;
    private ClientThread client1, client2;
    int client1Move = 0;
    int client2Move = 0;
    private TextArea messages = new TextArea();
    ArrayList<ClientThread> clientThreads = new ArrayList<>();
    int gameState = 0;

    //Setups the UI for the server
    private Parent setupLayout() {
        messages.setPrefHeight(550);
        messages.setEditable(false);
        TextField input = new TextField();
        VBox root = new VBox(20, messages, input);
        root.setPrefSize(600, 600);
        return root;
    }

    //Send data to all clients connected (stored in ClientThreads)
    private void sendToAllClients(String data){
        for(int i = 0; i < clientThreads.size(); i++){
            try{
                clientThreads.get(i).getOut().println(data);
                clientThreads.get(i).getOut().flush();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    //Sends game start information to all connected clients
    private void startGame(){
        messages.appendText("Getting moves from users \n");
        sendToAllClients("What is your move?");
        getInput();
    }

    //Tells all connected clients to enter the getInput state
    private void getInput(){
        for(int i = 0; i < clientThreads.size(); i++){
            clientThreads.get(i).setGettingInput(true);
        }
    }

    //Checks we've recieved any input from both clients
    //TODO: Sanity checking?
    private boolean validateInputs(){
        if(client2Move != 0 && client1Move != 0){
            messages.appendText("Inputs recieved \n");
            return true;
        }
        return false;
    }

    //After each turn we reset the moves we recieved to 0 (no move)
    private void reset(){
        client1Move = 0;
        client2Move = 0;
    }

    //Converts a integer representation of a play
    //to it's string representation
    private String intToPlayString(int val){
        if(val == 1){
            return "Rock";
        }else if(val == 2){
            return "Scissors";
        }else if(val == 3){
            return "Paper";
        }else if(val == 4){
            return "Lizard";
        }else if(val == 5){
            return "Spock";
        }
        return "ERROR";
    }

    //If either client has won the game, we need to end it
    //This function checks for that
    private void checkForWonThree(){
        if(client1.getScore() == 3){
            client1.getOut().println("You won!");
            client1.getOut().flush();
            client2.getOut().println("You lose!");
            client2.getOut().flush();
            gameState = 3;

        }else if(client2.getScore() == 3){
            client2.getOut().println("You won!");
            client2.getOut().flush();
            client1.getOut().println("You lose!");
            client1.getOut().flush();
        }else{
            gameState = 0;
        }
    }

    private void determineWinnerAndAddScore(){
        //The games moves are as follows
        /*
            1: rock
            2: scissors
            3. paper
            4. lizard
            5. spock
         */
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
        if(client1Move == 1 && client2Move == 2){
            winner = client1;
        }else if(client2Move == 1 && client2Move == 2){
            winner = client2;
        }else if(client1Move == 2 && client2Move == 3){
            winner = client1;
        }else if(client2Move == 2 && client1Move == 3){
            winner = client2;
        }else if(client1Move == 3 && client2Move == 1){
            winner = client1;
        }else if(client2Move == 3 && client1Move == 1){
            winner = client2;
        }else if(client1Move == 2 && client2Move == 4){
            winner = client1;
        }else if(client2Move == 2 && client1Move == 4){
            winner = client2;
        }else if(client1Move == 4 && client2Move == 5){
            winner = client1;
        }else if(client2Move == 4 && client1Move == 5){
            winner = client2;
        }else if(client1Move == 5 && client2Move == 2){
            winner = client1;
        }else if(client2Move == 5 && client1Move == 2){
            winner = client2;
        }else if(client1Move == 3 && client2Move == 5){
            winner = client1;
        }else if(client2Move == 3 && client1Move == 5){
            winner = client2;
        }else if(client1Move == 5 && client2Move == 1){
            winner = client1;
        }else if(client2Move == 5 && client1Move == 1){
            winner = client2;
        }else if(client1Move == 1 && client2Move == 4){
            winner = client1;
        }else if(client2Move == 1 && client1Move == 1){
            winner = client2;
        }else if(client1Move == 4 && client2Move == 3){
            winner = client1;
        }else if(client2Move == 4 && client1Move == 3){
            winner = client2;
        }
        client1.getOut().println("Your opponent played " + intToPlayString(client2Move));
        client1.getOut().flush();
        client1.getOut().println("You played " + intToPlayString(client1Move));
        client1.getOut().flush();


        client2.getOut().println("Your opponent played " + intToPlayString(client1Move));
        client2.getOut().flush();
        client2.getOut().println("You played " + intToPlayString(client2Move));
        client2.getOut().flush();

        if(winner == null){
            for(int i = 0; i < clientThreads.size(); i++){
                clientThreads.get(i).getOut().println("The game ended in a tie!");
                clientThreads.get(i).getOut().flush();
            }
        }else{
            //Do the score update
            winner.setScore(winner.getScore() + 1);
            winner.getOut().println("You won the round!");
            winner.getOut().flush();
            reset();
        }

        client1.getOut().println("Your score is " + client1.getScore() + " (Playing till 3)");
        client1.getOut().flush();

        client2.getOut().println("Your score is " + client2.getScore() + " (Playing till 3)");
        client2.getOut().flush();

        reset();
        checkForWonThree();


    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(setupLayout()));
        primaryStage.setTitle("Server");
        primaryStage.show();

        //Setup thread to run server
        //Our server has it's own thread but then dispatches NEW threads for each client
        //As is required
        new Thread(()->{
           try{
               //Server runs on port 5555
               //TODO: setup a gui to select port
               this.server = new ServerSocket(5555);

               //This loop runs for the entirety of the lifecyle of our server
               while(true){
                   //We're waiting for two clients to connect
                   //If both our clients are defined, start the game
                   if(client1 == null || client2 == null){
                       Socket newClient = server.accept();
                       ClientThread temp = new ClientThread(
                               newClient,
                               new PrintWriter(newClient.getOutputStream(),true),
                               new BufferedReader(new InputStreamReader(newClient.getInputStream()))
                       );

                       //Assign our socket to our new client
                       if(client1 == null)
                           client1 = temp;
                       else if(client2 == null)
                           client2 = temp;

                       clientThreads.add(temp);
                       new Thread(temp).start();
                       System.out.println("New Client Connected!");
                   }
                   //When both of our clients are connected
                   //We start a gameplay loop that has 4 states
                   //state 0: Start game
                   //State 1: Get input
                   //State 2: Determine Winner and add score
                   //State 3: If one player has won 3 games, we reset, this requires waiting for a press of the replay button
                   if(client1 != null && client2 != null){
                       try {
                           switch (gameState) {
                               case 0:
                                   gameState++;
                                   startGame();
                                   break;
                               case 1:
                                   if(validateInputs()){
                                       System.out.println("We got inputs");
                                       gameState++;
                                   }
                                   break;
                               case 2:
                                   determineWinnerAndAddScore();
                                   break;
                               case 3:
                                   sendToAllClients("resetGame");
                                   getInput();
                                   if(client1Move == 7 && client2Move == 7){
                                       reset();
                                       gameState = 0;
                                       client1.setScore(0);
                                       client2.setScore(0);
                                       break;
                                   }else if(client1Move == 8 || client2Move == 8){
                                       sendToAllClients("Your opponnent left");
                                   }
                           }
                       }catch (Exception e){
                           //TODO: Critical failure on exception is probably not ideal
                           e.printStackTrace();
                           System.exit(1);
                       }
                   }
               }

           }catch(Exception e){
               e.printStackTrace();
           }
        }).start();
    }

    public static void main(String[] args) {
        launch();
    }

    //Sub-class for thread created for each new client
    class ClientThread implements Runnable {

        //Allows us to determine if we're getting input from the two clients
        private boolean isGettingInput = false;

        //Class data, containing a printwriter and buffered reader for every connected client
        private Socket socket;
        BufferedReader in;
        PrintWriter out;

        //The score for the currently connected client
        private int score = 0;


        ///////////////////////////////////////////
        /* Getters and setters for class data    */
        ///////////////////////////////////////////
        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public void setGettingInput(boolean gettingInput) {
            isGettingInput = gettingInput;
        }


        public PrintWriter getOut() {
            return out;
        }

        ////////////////////////////////////////////
        /* End getters and setters                */
        ////////////////////////////////////////////


        //Every instance of this class should be instantiated with a socket, reader and writer
        public ClientThread(Socket socket, PrintWriter out, BufferedReader in) {
            this.socket = socket;
            this.out = out;
            this.in = in;
        }

        @Override
        public void run() {
            try {
                //Assign the values for our buffered reader and writer on thread setup
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream());

                //Handle message appending
                while (true) {

                    String data = in.readLine();
                    //If we are in the state where we are getting input
                    //We parse the data as an integer
                    if(isGettingInput){
                        if(this == client1){
                            client1Move = Integer.parseInt(data);
                        }else if(this == client2){
                            client2Move = Integer.parseInt(data);
                        }
                        this.isGettingInput = false;
                    }

                    //Print data to client console
                    Platform.runLater(() -> {
                        messages.appendText("Server received: " + data + "\n");
                    });
                }
            } catch (Exception e) {
                //TODO: lol what even is error handling you stupid fuck????
                System.out.println("An error has occured");
            }
        }
    }
}
