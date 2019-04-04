package new_rpsls;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javafx.geometry.Insets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client extends Application {
    Socket socket;
    PrintWriter toServer;
    BufferedReader fromServer;
    String ip;
    //TODO:Implement this
    int port = 5555;

    private void connectToServer(String ip, String port, String userName){
        System.out.println("Ready to connect with this information");
        System.out.println("IP: " + ip + ", Port: " + port + ", userName: " + userName);
        try{
            socket = new Socket("localhost",Integer.parseInt(port));
            Listener temp = new Listener(
                    new BufferedReader(new InputStreamReader(socket.getInputStream())),
                    new PrintWriter(socket.getOutputStream())
            );
            temp.run();
        }catch(Exception e){
            e.printStackTrace();
        }
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
        TextField ipField = new TextField("192.0.0.1");

        //Port box
        Label port = new Label("Port:");
        TextField portField = new TextField("5555");

        //Finish button
        Button submit = new Button("Connect");
        submit.setOnAction(e->{
            connectToServer(ipField.getText(),portField.getText(),username.getText());
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
        primaryStage.setScene(setupClientInfoUI());
        primaryStage.show();
    }

    class Listener implements Runnable{

        //Hold data allowing us to read and write from server
        BufferedReader in;
        PrintWriter out;

        public Listener(BufferedReader in, PrintWriter out) {
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

                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
