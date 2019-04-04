package new_rpsls;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javafx.geometry.Insets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;

public class Client extends Application {
    Listener listener;
    TextArea log;
    Stage stage;

    private void connectToServer(String ip, String port, String userName){
        System.out.println("Ready to connect with this information");
        System.out.println("IP: " + ip + ", Port: " + port + ", userName: " + userName);
        try{
            Socket socket = new Socket("localhost",Integer.parseInt(port));
            Listener temp = new Listener(
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

        VBox container = new VBox();

        //Create the box where we will log data from server
        TextArea log = new TextArea();
        log.setMinHeight(400);
        this.log = log;

        //Create the box where we can enter data
        TextField input = new TextField();

        input.setOnAction(e->{
            listener.getOut().println(input.getText());
            listener.getOut().flush();
            input.clear();
        });

        container.getChildren().addAll(log,input);
        chatPane.setCenter(container);

        return new Scene(chatPane,700,500);
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

    //Implement the GUI
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
        primaryStage.setScene(setupClientInfoUI());
        primaryStage.show();
    }

    class Listener implements Runnable{

        //Hold data allowing us to read and write from server
        BufferedReader in;
        PrintWriter out;

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

        public Listener(BufferedReader in, PrintWriter out, String username) {
            this.in = in;
            this.out = out;
        }

        //Create a thread to sit on the server and listen to it
        @Override
        public void run() {
            while(true){
                try{
                    String dataFromServer = in.readLine();
                    System.out.println(dataFromServer);
                    if(log != null)
                        log.appendText(dataFromServer + "\n");

                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
