package benchmarkgame;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import benchmarkgame.gameutils.Command;
import benchmarkgame.gameutils.LocPair;
import benchmarkgame.gameutils.Move;
import benchmarkgame.gameutils.PositionState;
import benchmarkgame.gameutils.Status;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Random;


/**
 * Implements the game server
 */
public class Server {
    /**
     * Maximum number of threads the server may launch.
     */
    private final int threadLimit = 16;
    /**
     * Maximum number of players may be connected to the server.
     */
    private final int maxPlayers  = 30;
    /**
     * Fixed game map size.
     */
    private final int boardSide   = maxPlayers / 2;
    private String[][] MAP = new String[boardSide][boardSide];
    private Map<String, LocPair> currentPosition; 
    private NetworkManager nm;
    private static Server ref = null;
    private int nextFreePos = 0;
    Random randomNumber = new Random();

    /**
     * Returns reference to the server single instance.
     * @return Reference to the server single instance
     */
    public static Server getServerRef() {
        return ref;
    }
    
    /**
     * Constructor: Initializes server by stablishing a connection with the network,
     * initializing the currentPosition and ref variables and initializing the map 
     * of the game with all positions being free, that is, without players.
     *
     * @param ipAddr IP address of the server
     * @throws Exception
     */
    private Server(String ipAddr) throws Exception {
        nm = new NetworkManager(this, ipAddr);
        currentPosition = new HashMap<String, LocPair>();
        ref = this;
        for(int i = 0 ; i < boardSide; i++) {
            for(int j = 0 ; j < boardSide; j++) {
                MAP[i][j] = "free";
            }
        }
    }
    
    /**
     * Returns a reference to the server or creates a new one and returns.
     * @param ipAddr IP address of the server
     * @return Reference to the server single instance
     * @throws Exception 
     */
    public static Server v(String ipAddr) throws Exception {
        if (ref != null)
            return ref;
        
        return new Server(ipAddr);
    }
    
    /**
     * Listen to client new requests through the NetworkManager.
     * @throws Exception 
     */
    private void init() throws Exception {
        System.out.println("\r\nRunning Game Server: " + 
                "Host = " + nm.getSocketAddress().getHostAddress() + 
                " Port = " + nm.getPort());
        
        nm.listen();
    }
    
    /**
     * Chooses a random initial position for a new player.
     *
     * @param clientID New player ID
     * @return Status Enum status describing the success or fail of operation
     */
    public Status randomPosition(String clientID) {
        
        int x, y;
        LocPair pos;

        if (nextFreePos >= (boardSide*boardSide)) {
            return Status.FAILED;
        }
        
        if (! currentPosition.containsKey(clientID)) {
            synchronized(MAP) {
                do {
                	x = randomNumber.nextInt(boardSide); 
                	y = randomNumber.nextInt(boardSide);
                } while(MAP[x][y] != "free"); //finds a initial position for the player that is free
                pos = new LocPair(x,y);
                currentPosition.put(clientID, pos); //stores the player current position
                MAP[x][y] = clientID; //updates the game map
                nextFreePos++;
            }
        }
        
        return Status.OK;
    }
    
    /**
     * Updates the player position in the game map.
     *
     * @param clientID Player identification.
     * @param x Change in the direction x.
     * @param y Change in the direction y.
     * @return LocPair, the coordinate of the current position of the client in the game map.
     */
    public LocPair updatePosition(String clientID, int x, int y) {
        int[] displacement = {-1, 0, 1}; 
        LocPair ret, wantedPos, otherClientNextPos, change;
        LocPair current_pos = currentPosition.get(clientID);
        String otherClientID;

        wantedPos = new LocPair(-2, -2);
        change = new LocPair(-2, -2);
        otherClientNextPos = new LocPair(-2, -2);

        // checking future position
        x += current_pos.x;
        y += current_pos.y;
        
        //making the matrix become "circular"
        if (x >= boardSide){
        	x -= boardSide;
        } else if (x < 0) {
        	x += boardSide; 
        }

        if (y >= boardSide){
        	y -= boardSide;
        } else if (y < 0) {
        	y += boardSide;
        }

        //updating the game map if the position is free
        synchronized (MAP) {
            switch(MAP[x][y]) {
                case "free":
                    // update game map
                    MAP[x][y] = clientID;
                    MAP[current_pos.x][current_pos.y] = "free";

                    // update hashmap pos reference
                    current_pos.x = x;
                    current_pos.y = y;
                    return current_pos;
                
                default:
                    wantedPos.x = x;
                    wantedPos.y = y;
    
                    otherClientID = MAP[x][y]; //gets the ID of the player who is occupying 
                                               //the place for which the first wanted to move
                    otherClientNextPos.x = x;
                    otherClientNextPos.y = y;
                
                    //finds a new position to the other client
                    while(otherClientNextPos.compare(wantedPos) || otherClientNextPos.compare(current_pos)){
                        change.x = displacement[randomNumber.nextInt(3)];
                        change.y = displacement[randomNumber.nextInt(3)];
                        otherClientNextPos.x = wantedPos.x + change.x;
                        otherClientNextPos.y = wantedPos.y + change.y;
                    }

                    //moves the other client
                    ret = updatePosition(otherClientID, change.x, change.y);

                    if (otherClientNextPos.x >= boardSide){
                        otherClientNextPos.x -= boardSide;
                    } else if (otherClientNextPos.x < 0) {
                        otherClientNextPos.x += boardSide; 
                    }

                    if (otherClientNextPos.y >= boardSide){
                        otherClientNextPos.y -= boardSide;
                    } else if (otherClientNextPos.y < 0) {
                        otherClientNextPos.y += boardSide;
                    }
                    
                    //if it works, I know that the wanted position is now available 
                    //and the client can move to the wanted position
                    if(ret.compare(otherClientNextPos)){
                        // update game map
                        MAP[x][y] = clientID;
                        MAP[current_pos.x][current_pos.y] = "free";

                        // update hashmap pos reference
                        current_pos.x = x;
                        current_pos.y = y;
                        return current_pos; 
                    }

                    return current_pos; // if it not works,  
                                        // the client's original position is returned
            }
        }
    }
    
    /**
     * Returns the current position of the requested client.
     *
     * @param clientID Player identification.
     * @return LocPair, a reference to the coordinate of the current position of the customer in the game map
     */
    public LocPair getCurrentPosition(String clientID){
        return currentPosition.get(clientID);
    }

    @Override
    public String toString(){
        String out = "";
        for(int i = 0 ; i < boardSide; i++) {
            for(int j = 0 ; j < boardSide; j++) {
                if(MAP[j][i] == "free")
                    out += "0 ";
                else
                    out += "X ";
                //out += MAP[i][j] + "\t";
            }
            out += "\n";
        }
        return out;
    }
    
    public static void main(String[] args) throws Exception {
        
        final Server app = Server.v(args[0]); // the function initializes the server 
        									  // and returns a reference to the server,
        									  // which is stored in "app".
        									  // if there is no instance of the server class, 
        									  // the function calls the class constructor to create it

        //creating a thread by implementing its run method 
        //thread function: create a file that stores the game state every second
        Thread printer = new Thread() {              
                public void run(){
                    BufferedWriter writer = null;
                    try {
                        writer = new BufferedWriter(new FileWriter("state.txt"));
                        writer.write("Game State:\n");
                        while(true) {                        
                            writer.write(app.toString());
                            writer.write("\n\n");
                            Thread.sleep(500);
                        }
                        
                    } catch (Exception e) {
                        // do something
                    }
                    // writer.close();
                }
            };
        
        printer.start();
        
        app.init();
    }
}
