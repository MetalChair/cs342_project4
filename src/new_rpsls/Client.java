package new_rpsls;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javafx.geometry.Insets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.sun.prism.paint.Color.RED;

public class Client extends Application {
    Listener listener;
    TextArea log;
    Stage stage;
    TextArea connectedPlayers;
    Label challengeField;

    private void connectToServer(String ip, String port, String userName){
        System.out.println("Ready to connect with this information");
        System.out.println("IP: " + ip + ", Port: " + port + ", userName: " + userName);
        try{
            Socket socket = new Socket("localhost",Integer.parseInt(port));
            Listener temp = new Listener(
                    socket,
                    new BufferedReader(new InputStreamReader(socket.getInputStream())),
                    new PrintWriter(socket.getOutputStream()),
                    userName
            );
            this.listener = temp;
            new Thread(temp).start();

            //Show the chat box
            stage.setScene(setupChatGUI());

            //We should send the username here when we first open it
            listener.getOut().println("!USER " + userName);
            listener.getOut().flush();


        }catch(Exception e){
            e.printStackTrace();
        }
    }

    //Setup the text area for chatting
    public Scene setupChatGUI(){
        BorderPane chatPane = new BorderPane();
        chatPane.setPadding(new Insets(5,5,5,5));

        /*
        layout:
                        fullContainer
        Container                   Player List
        --------------------------  Connected Players
        | GameLog                |
        |                        |
        |                        |
        |                        |
        |                        |
        |                        |
        |                        |
        --------------------------
        --------------------------
        |Input                   |
        --------------------------
         */
        VBox container = new VBox();
        VBox playerList = new VBox();
        HBox fullContainer = new HBox();

        //Create the box where we will log data from server
        Label serverLabel = new Label("Server Log:");
        TextArea log = new TextArea();
        log.setMinHeight(400);
        log.setEditable(false);
        this.log = log;

        //Create the box where we can enter data
        TextField input = new TextField();

        input.setOnAction(e->{
            listener.getOut().println(input.getText());
            listener.getOut().flush();
            input.clear();
        });

        //Create the bcx for showing all currently connected players
        Label connected = new Label("Players Online:");
        connectedPlayers = new TextArea();
        connectedPlayers.setEditable(false);
        playerList.getChildren().addAll(connected,connectedPlayers);

        //Challenge text
        challengeField = new Label("");
        challengeField.setTextFill(Color.rgb(255,0,0));
        fullContainer.getChildren().addAll(container,playerList);
        container.getChildren().addAll(serverLabel,log,input,challengeField);

        //Set the secene
        chatPane.setCenter(fullContainer);

        return new Scene(chatPane,1000,500);
    }

    //Setup the gui for our first UI where we ask for a port, IP, and username
    public Scene setupClientInfoUI(){
        BorderPane clientPane = new BorderPane();
        clientPane.setPadding(new Insets(5,5,5,5));
        VBox container = new VBox();

        //User name box
        Label username = new Label("Username: ");
        TextField userField=  new TextField("RPSGod");

        //IP box
        Label ip = new Label("IP Address");
        TextField ipField = new TextField("192.168.0.1");

        //Port box
        Label port = new Label("Port:");
        TextField portField = new TextField("5555");

        //Finish button
        Button submit = new Button("Connect");
        submit.setOnAction(e->{
            connectToServer(ipField.getText(),portField.getText(),userField.getText());
        });

        //Add the vbox to the middle and add all buttons
        clientPane.setCenter(container);
        container.getChildren().addAll(username,userField,ip,ipField,port,portField,submit);

        //Create a scene and return it
        Scene newScene = new Scene(clientPane,700,500);
        return newScene;
    }

    public static void main(String[] args){
        launch(args);
    }

    //Override the stop command so we can kill our connection
    @Override
    public void stop(){
        //Notify our server that we're dying
        listener.getOut().println("!QUIT");
        listener.getOut().flush();
        listener.terminate();

        //Close our connection
        try{
            listener.socket.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        //Kill the window
        System.out.println("Quitting");
    }

    //Implement the GUI
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
        primaryStage.setScene(setupClientInfoUI());
        primaryStage.show();
    }

    class Listener implements Runnable{

        //Are we running?
        private boolean running = true;

        //Hold data allowing us to read and write from server
        BufferedReader in;
        PrintWriter out;

        //Hold socket of listener
        Socket socket;

        //Holds the username of the client
        String username;

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

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Listener(Socket socket, BufferedReader in, PrintWriter out, String username) {
            this.in = in;
            this.out = out;
            this.socket = socket;
            this.username = username;
        }

        public void terminate(){
            running = false;
        }

        //Create a thread to sit on the server and listen to it
        @Override
        public void run() {
            while(running){
                try{
                    String dataFromServer = in.readLine();
                    System.out.println(dataFromServer);
                    //If we have a log
                    if(log != null){
                        //We need to handle special cases IE, Getting a new client list
                        //Or getting a challenge

                        //If we got a new client list
                        if(dataFromServer.contains("!CLIENTS ") && dataFromServer.substring(0,9).equals("!CLIENTS ")) {
                            //Split it into a list
                            dataFromServer = dataFromServer.substring(9);
                            List<String> nameList = Arrays.asList(dataFromServer.split(","));
                            //Clear our box and add all the names
                            connectedPlayers.clear();
                            for (int i = 0; i < nameList.size(); i++) {
                                connectedPlayers.appendText(nameList.get(i) + "\n");
                            }
                        }
                        else if(dataFromServer.contains("!CHALLENGE ")){
                            //Create string so we can set it in a Runnable
                            final String data = dataFromServer.substring(11);
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    challengeField.setText(data);
                                }
                            });

                        }else{
                            log.setStyle("");
                            //If we have a basic message, just throw it into the log
                            log.appendText(dataFromServer + "\n");

                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
