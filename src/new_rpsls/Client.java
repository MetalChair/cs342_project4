package new_rpsls;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
    private Listener listener;
    private TextArea log;
    private Stage stage;
    private TextArea connectedPlayers;
    private Label challengeField;
    private HBox fullContainer;
    private HBox buttonBox;
    private VBox gameLog;
    private VBox gameTextLog;
    private HBox resetBox;

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
            setupChatGUI();

            this.listener = temp;
            new Thread(temp).start();


            //We should send the username here when we first open it
            listener.getOut().println("!USER " + userName);
            listener.getOut().flush();


        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public HBox setupGameButtons(){
        //Setup buttons in hbox
        //rock
        Image rockImg = new Image(getClass().getResourceAsStream("./Resources/rock.png"));
        Button rockBtn = new Button();
        rockBtn.setMaxHeight(75);
        rockBtn.setMaxWidth(75);
        ImageView rockBG = new ImageView(rockImg);
        rockBG.setFitHeight(75);
        rockBG.setFitWidth(75);
        rockBtn.setGraphic(rockBG);

        //paper
        Image paperImg = new Image(getClass().getResourceAsStream("./Resources/paper.png"));
        Button paperBtn = new Button();
        paperBtn.setMaxHeight(75);
        paperBtn.setMaxWidth(75);
        ImageView paperBG = new ImageView(paperImg);
        paperBG.setFitHeight(75);
        paperBG.setFitWidth(75);
        paperBtn.setGraphic(paperBG);

        //scissors
        Image scissorsImg = new Image(getClass().getResourceAsStream("./Resources/scissors.jpg"));
        Button scissorsBtn = new Button();
        scissorsBtn.setMaxHeight(75);
        scissorsBtn.setMaxWidth(75);
        ImageView scissorsBG = new ImageView(scissorsImg);
        scissorsBG.setFitHeight(75);
        scissorsBG.setFitWidth(75);
        scissorsBtn.setGraphic(scissorsBG);

        //lizard
        Image lizardImg = new Image(getClass().getResourceAsStream("./Resources/lizard.jpg"));
        Button lizardBtn = new Button();
        lizardBtn.setMaxHeight(75);
        lizardBtn.setMaxWidth(75);
        ImageView lizardBG = new ImageView(lizardImg);
        lizardBG.setFitHeight(75);
        lizardBG.setFitWidth(75);
        lizardBtn.setGraphic(lizardBG);

        //spock
        Image spockImg = new Image(getClass().getResourceAsStream("./Resources/spock.jpg"));
        Button spockBtn = new Button();
        spockBtn.setMaxHeight(75);
        spockBtn.setMaxWidth(75);
        ImageView spockBG = new ImageView(spockImg);
        spockBG.setFitHeight(75);
        spockBG.setFitWidth(75);
        spockBtn.setGraphic(spockBG);

        //Setup our play buttons so that they interact with the server
        rockBtn.setOnMouseClicked(e ->{
            listener.getOut().println("!MOVE 1");
            listener.getOut().flush();
            setPlayButtonsAsInactive(true);
        });
        paperBtn.setOnMouseClicked(e ->{
            listener.getOut().println("!MOVE 3");
            listener.getOut().flush();
            setPlayButtonsAsInactive(true);
        });
        scissorsBtn.setOnMouseClicked(e->{
            listener.getOut().println("!MOVE 2");
            listener.getOut().flush();
            setPlayButtonsAsInactive(true);
        });
        lizardBtn.setOnMouseClicked(e->{
            listener.getOut().println("!MOVE 4");
            listener.getOut().flush();
            setPlayButtonsAsInactive(true);
        });
        spockBtn.setOnMouseClicked(e->{
            listener.getOut().println("!MOVE 5");
            listener.getOut().flush();
            setPlayButtonsAsInactive(true);
        });

        this.buttonBox = new HBox();
        buttonBox.getChildren().addAll(rockBtn,paperBtn,scissorsBtn,lizardBtn,spockBtn);
        return buttonBox;
    }

    //activates/deactivates the play buttons
    public void setPlayButtonsAsInactive(boolean state){
        for(int i = 0; i < buttonBox.getChildren().size(); i++){
            buttonBox.getChildren().get(i).setDisable(state);
        }
    }

    //Setup the lobby UI
    public void setupLobbyGUI(){
        ScrollPane scrollableGameLog = new ScrollPane();
        ScrollPane scrollableGameTextLog = new ScrollPane();

        //Make these scrollable in the case theres a very long game
        scrollableGameLog.setStyle("-fx-background-color: tranparent;");
        scrollableGameTextLog.setStyle("-fx-background-color: tranparent;");

        this.gameLog = new VBox();
        this.gameTextLog = new VBox();

        scrollableGameTextLog.setContent(gameTextLog);
        scrollableGameLog.setContent(gameLog);
        //We run this in a runlater to prevent thread issues with JAVAFX
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                fullContainer.getChildren().clear();

                //Setup the vboxes to hold game state
                VBox gameContainer = new VBox();
                VBox gameControls = new VBox();

                gameContainer.setMinWidth(225);

                //Create a button for resetting and quitting lobby
                Button reset = new Button("Reset");
                reset.setOnAction(e->{
                    listener.getOut().println("!REPLAY");
                    resetBox.setVisible(false);
                    listener.getOut().flush();
                });

                Button quit = new Button("Quit");
                quit.setOnAction(e->{
                    listener.getOut().println("!QUITLOBBY");
                    resetBox.setVisible(false);
                    listener.getOut().flush();
                });

                //Add them to their container
                resetBox = new HBox();

                resetBox.getChildren().addAll(reset,quit);

                resetBox.setVisible(false);

                //Add the controls
                gameControls.getChildren().add(resetBox);
                gameControls.getChildren().add(setupGameButtons());
                gameControls.getChildren().add(scrollableGameTextLog);

                //Create the box where we will log data for the game
                Label gameLogLabel = new Label("Game Log:");

                gameContainer.getChildren().addAll(gameLogLabel,scrollableGameLog);
                fullContainer.getChildren().addAll(gameContainer,gameControls);
            }
        });
    }

    //Converts a integer representation of a play
    //to it's string representation
    public static String intToPlayString(int val){
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

    //Setup the text area for chatting
    public void setupChatGUI(){
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
        this.fullContainer = fullContainer;

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
        String onlineString = "Players Online:";
        onlineString = String.format("%-59s",onlineString);
        onlineString += "Score:";
        Label connected = new Label(onlineString);
        connectedPlayers = new TextArea();
        connectedPlayers.setEditable(false);
        playerList.getChildren().addAll(connected,connectedPlayers);

        //create the box for showing all the rules and commands the users can do
        TextArea RuleBox = new TextArea("The commands are as follows:\n" +
                "  -> !CHALLENGE <username> : challenges the user with username currently on the server\n" +
                "  -> !USER <newname> : sets your username to the newly listed name\n" +
                "  -> !ACCEPT : Accepts a currently active challenge\n");
        RuleBox.setPrefHeight(195);
        RuleBox.setPrefWidth(200);
        RuleBox.setEditable(false);
        RuleBox.setWrapText(true);

        VBox VBoxRuleNPlayerLog = new VBox();
        VBoxRuleNPlayerLog.getChildren().addAll(playerList, RuleBox);

        //Challenge text
        challengeField = new Label("");
        challengeField.setTextFill(Color.rgb(255,0,0));
        fullContainer.getChildren().addAll(container,VBoxRuleNPlayerLog);
        container.getChildren().addAll(serverLabel,log,input,challengeField);

        //Set the scene
        chatPane.setCenter(fullContainer);

        this.stage.setScene(new Scene(chatPane,700,500));
    }

    private ImageView moveIntToPlayIcon(int move){
        ImageView image = null;
        if(move == 1){
            Image rockImg = new Image(getClass().getResourceAsStream("./Resources/rock.png"),75,75,false,false);
            image = new ImageView(rockImg);
        }else if(move == 2){
            Image scissorsImg = new Image(getClass().getResourceAsStream("./Resources/scissors.jpg"),75,75,false,false);
            image = new ImageView(scissorsImg);
        }else if(move == 3){
            Image paperImg = new Image(getClass().getResourceAsStream("./Resources/paper.png"),75,75,false,false);
            image = new ImageView(paperImg);
        }else if(move == 4){
            Image lizardImg = new Image(getClass().getResourceAsStream("./Resources/lizard.jpg"),75,75,false,false);
            image = new ImageView(lizardImg);
        }else{
            Image spockImg = new Image(getClass().getResourceAsStream("./Resources/spock.jpg"),75,75,false,false);
            image = new ImageView(spockImg);
        }
        return image;
    }

    //Setup the gui for our first UI where we ask for a port, IP, and username
    public void setupClientInfoUI(){
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
        this.stage.setScene(newScene);
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
        setupClientInfoUI();
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
                    if(log != null) {
                        //We need to handle special cases IE, Getting a new client list
                        //Or getting a challenge

                        //If we got a new client list
                        if (dataFromServer.contains("!CLIENTS ") && dataFromServer.substring(0, 9).equals("!CLIENTS ")) {
                            //Split it into a list
                            dataFromServer = dataFromServer.substring(9);
                            List<String> nameList = Arrays.asList(dataFromServer.split(","));
                            //Clear our box and add all the names
                            connectedPlayers.clear();
                            for (int i = 0; i < nameList.size(); i++) {
                                String name = nameList.get(i).substring(0,nameList.get(i).indexOf(";"));
                                name = String.format("%-64s", name);
                                name += nameList.get(i).substring(nameList.get(i).indexOf(";") + 1);
                                connectedPlayers.appendText(name + "\n");
                            }
                        } else if (dataFromServer.contains("!CHALLENGE ")) {
                            //Create string so we can set it in a Runnable
                            final String data = dataFromServer.substring(11);
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    challengeField.setText(data);
                                }
                            });

                        } else if (dataFromServer.contains("!STARTGAME")) {
                            setupLobbyGUI();
                        } else if (dataFromServer.contains("!GAMELOG")) {
                            HBox box = new HBox();
                            box.setSpacing(2.5);
                            String choppedString = dataFromServer.replaceAll("[\\D]", "");
                            int move1 = Character.getNumericValue(choppedString.charAt(0));
                            int move2 = Character.getNumericValue(choppedString.charAt(1));
                            int won = Character.getNumericValue(choppedString.charAt(2));

                            if (won == 1) {
                                box.setStyle("" +
                                        "-fx-border-color: green;\n" +
                                        "-fx-border-insets: 5;\n" +
                                        "-fx-border-width: 3;\n" +
                                        "-fx-border-style: dashed;\n");
                            } else if (won == 0) {
                                box.setStyle("" +
                                        "-fx-border-color: red;\n" +
                                        "-fx-border-insets: 5;\n" +
                                        "-fx-border-width: 3;\n" +
                                        "-fx-border-style: dashed;\n");
                            }

                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    //Tell the user what the opponent played
                                    gameTextLog.getChildren().add(new Label("Your opponent played " + intToPlayString((move2))));

                                    //Add an element to show the last round of play
                                    box.getChildren().add(moveIntToPlayIcon(move1));
                                    Label vs = new Label("VS.");
                                    vs.setMinHeight(75);
                                    box.getChildren().add(vs);
                                    box.getChildren().add(moveIntToPlayIcon(move2));
                                    gameLog.getChildren().add(box);

                                    String logStr = "";
                                    //label for if we won or lost
                                    Label winLoss = new Label();
                                    if (won == 1) {
                                        winLoss.setText("WON");
                                        logStr = "You won!";
                                    } else if (won == 0) {
                                        winLoss.setText("LOST");
                                        logStr = "You lost!";
                                    } else {
                                        winLoss.setText("TIE");
                                        logStr = "Game ended in a tie!";
                                    }

                                    winLoss.setMinHeight(75);

                                    //set gamelog message to inform winner and loser
                                    gameTextLog.getChildren().add(new Label(logStr));

                                    box.getChildren().add(winLoss);
                                }
                            });

                            setPlayButtonsAsInactive(false);

                        } else if (dataFromServer.contains("!TEXTLOG")) {
                            String append = dataFromServer.substring(9);
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    gameTextLog.getChildren().add(new Label(append));
                                }
                            });
                        } else if (dataFromServer.contains("!ENDLOBBY")) {
                            System.out.println("Resetting chat ui");
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    setupChatGUI();
                                }
                            });
                            this.getOut().println("!GETCLIENTLIST");
                            this.getOut().flush();
                        } else if (dataFromServer.contains("!SHOWRESETBOX")){
                            System.out.println("Showing reset box");
                            resetBox.setVisible(true);
                        }else{
                            log.setStyle("");
                            //If we have a basic message, just throw it into the log
                            log.appendText(dataFromServer + "\n");
                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                    System.exit(99);
                }
            }
        }
    }
}
