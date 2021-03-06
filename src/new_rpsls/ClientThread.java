package new_rpsls;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientThread implements Runnable{

    //Hold the number of games the client has won
    volatile int score = 0;

    //Stores the lobby the user is currently in
    volatile private Lobby currentLobby = null;

    //We use this to see if the user is currently in a game lobby
    volatile private boolean isInLobby = false;

    //Use this to reference the person who challenged this user to a game
    volatile private ClientThread challenger = null;

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

    public void increaseScoreByOne(){
        score++;
    }

    public boolean isInLobby() {
        return isInLobby;
    }

    public void setInLobby(boolean inLobby) {
        isInLobby = inLobby;
    }

    public ClientThread getChallenger() {
        return challenger;
    }

    public void setChallenger(ClientThread challenger) {
        this.challenger = challenger;
    }

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

    public String getQueuedMessage() {
        return queuedMessage;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Lobby getCurrentLobby() {
        return currentLobby;
    }

    public void setCurrentLobby(Lobby currentLobby) {
        this.currentLobby = currentLobby;
    }

    public void setQueuedMessage(String queuedMessage) {
        this.queuedMessage = queuedMessage;
    }

    public int getScore() {
        return score;
    }

    public ClientThread(Socket socket, BufferedReader in, PrintWriter out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    public void resetLobbyStatus(){
        challenger = null;
        isInLobby = false;
        currentLobby = null;
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
                    if(!data.contains(",") && !data.contains(";")){
                        this.hasChangedName = true;
                        System.out.println("Client has asked to change name");
                        //If we don't have a username, set it and notify the server
                        if (this.userName == "UNSET") {
                            queuedMessage = data.substring(6) + " has connected!";
                        } else {
                            queuedMessage = userName + " has changed their name to " + data.substring(6);
                        }
                        this.userName = data.substring(6);
                    }else{
                     this.getOut().println("Names cannot contain , or ;");
                     this.getOut().flush();
                    }
                }else if(data.contains("!CHALLENGE ") && data.substring(0,11).equals("!CHALLENGE ")){
                    //We've recieved a challenge request to challenge
                    System.out.println(this.userName + "has challenged a user");
                    //Pass the request to the server
                    queuedMessage = data;
                }else if(data.equals("!QUIT")) {
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