package new_rpsls;

import javafx.application.Application;
import javafx.stage.Stage;

import javax.xml.bind.Unmarshaller;
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

    public static void main(String[] args){
        launch(args);
    }

    //Implement the GUI
    @Override
    public void start(Stage primaryStage) throws Exception {
        try{
            socket = new Socket("localhost",port);
            Listener temp = new Listener(
                    new BufferedReader(new InputStreamReader(socket.getInputStream())),
                    new PrintWriter(socket.getOutputStream())
                    );
            temp.run();
        }catch(Exception e){
            e.printStackTrace();
        }
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
