package rps;


import java.io.*;
import java.net.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class Client extends Application {

    PrintWriter toServer = null;
    BufferedReader fromServer = null;
    String ip;
    int port;

    @Override
    public void start (Stage primaryStage) throws Exception {
        /*---------------------------------------------*/
        /*Everything below this exciting box handles ui*/
        /*stetup. Since JavaFX is beyond stupid, this  */
        /*all has to be done in a single function. Here*/
        /*lies a convoluted UI setup. Beware.          */
        /*---------------------------------------------*/

        //We run the whole app from a borderpane
        BorderPane fieldPane = new BorderPane();
        fieldPane.setPadding(new Insets(5, 5, 5, 5));
        HBox buttonBox = new HBox();

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

        //We create a container holding the buttons that are used to determine "play again"
        //and quit once a user has won 3 rounds
        Button reset = new Button("Play Again");

        Button quit = new Button("Quit");

        //We make them invisible because they're only shown once the round ends
        HBox resetBox = new HBox();
        resetBox.setVisible(false);
        resetBox.getChildren().addAll(reset,quit);


        buttonBox.getChildren().addAll(rockBtn,paperBtn,scissorsBtn, lizardBtn, spockBtn);

        TextField field = new TextField();
        field.setAlignment(Pos.BOTTOM_RIGHT);
        fieldPane.setCenter(field);

        BorderPane mainPane = new BorderPane();
        TextArea area = new TextArea();
        area.setEditable(false);
        mainPane.setCenter(new ScrollPane(area));
        mainPane.setTop(buttonBox);
        mainPane.setBottom(resetBox);

        Scene scene = new Scene(mainPane, 700, 400);
        primaryStage.setTitle("Client");
        primaryStage.setScene(scene);
        primaryStage.show();

        //Connect to our socket
        try {
            System.out.println("Setting up socket");
            Socket socket = new Socket("localhost", 5555);

            fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            toServer = new PrintWriter(socket.getOutputStream());

            //Setup a listener thread
            ClientListener temp  = new ClientListener(area,fromServer, toServer,resetBox);
            new Thread(temp).start();


        } catch (Exception e) {
            area.appendText(e.toString() + '\n');
        }

        //Setup our play buttons so that they interact with the server
        rockBtn.setOnMouseClicked(e ->{
            toServer.println(1);
            toServer.flush();
        });
        paperBtn.setOnMouseClicked(e ->{
            toServer.println(3);
            toServer.flush();
        });
        scissorsBtn.setOnMouseClicked(e->{
            toServer.println(2);
            toServer.flush();
        });
        lizardBtn.setOnMouseClicked(e->{
            toServer.println(4);
            toServer.flush();
        });
        spockBtn.setOnMouseClicked(e->{
            toServer.println(5);
            toServer.flush();
        });
        reset.setOnMouseClicked(e ->{
            toServer.println(7);
            resetBox.setVisible(false);
            toServer.flush();
        });
        quit.setOnMouseClicked(e ->{
            toServer.println(8);
            resetBox.setVisible(false);
            Platform.exit();
        });
    }
    //This is the thread subclass for the client which listens for socket data
    class ClientListener implements Runnable{
        //Class data

        //Holds the output log
        TextArea area;

        //Used to read data from server
        BufferedReader in;

        //Used to send data to server
        PrintWriter out;

        //Box containing reset and play again buttons
        HBox resetBox;


        //Constructor, thread should never be created without these objects
        public ClientListener(TextArea area, BufferedReader in, PrintWriter out, HBox resetBox){
            this.area = area;
            this.in = in;
            this.out = out;
            this.resetBox = resetBox;
        }

        //Our infinite thread loop
        @Override
        public void run() {
            while(true){
                try{
                    //Read in a line, if we aren't resetting the game, print it
                    //If we are resetting the game, show the play again button
                    String data = in.readLine();
                    if(!data.equals("resetGame")){
                        area.appendText("Server: " + in.readLine() + "\n");
                    }else{
                        resetBox.setVisible(true);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main (String[] args) {
        Application.launch(args);

    }
}

