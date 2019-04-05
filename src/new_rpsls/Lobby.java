package new_rpsls;

public class Lobby {
    //Hold the two clients participating in the lobby
    ClientThread client1;

    ClientThread client2;

    public Lobby(ClientThread client1){
        this.client1 = client1;
    }
}
