package benchmarkgame;

import benchmarkgame.gameutils.Command;
import benchmarkgame.gameutils.LocPair;
import benchmarkgame.gameutils.Move;
import benchmarkgame.gameutils.Status;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Implements a network manager that handles new user requests, as well user 
 * operations.
 */
public class NetworkManager {
    private ServerSocket server;
    private int threadLimit = 16;
    private Server s;
    
    
    /**
     * Initializes server, opening socket on input IP address.
     * @param ipAddress: IP address to be used by the server.
     * @throws Exception 
     */
    public NetworkManager(Server _s, String ipAddress) throws Exception {
        if (ipAddress != null && !ipAddress.isEmpty()) 
          this.server = new ServerSocket(0, 1, InetAddress.getByName(ipAddress));
        else 
          this.server = new ServerSocket(0, 1, InetAddress.getLocalHost());
        this.s = _s;
    }
    
    /**
     * Listen for client connections, launching threads for each new connection
     * @throws Exception 
     */
    public void listen() throws Exception {
        System.out.println("Waiting for new players ...");
        while (true) {                        
            Socket client = this.server.accept();
            Thread t = new Thread(new Handler(client));
            t.start();
        }        
    }
    public InetAddress getSocketAddress() {
        return this.server.getInetAddress();
    }
    
    public int getPort() {
        return this.server.getLocalPort();
    }
}

/**
 * Implements a runnable handler to each user.
 */
class Handler implements Runnable {
    Socket client;
    String clientID;
    Server s;
    
    public Handler(Socket c) {
        this.client = c;
    }
    
    private void sendMessage(String message) throws IOException {
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
        out.println(message);
        out.flush();
        return;
    }

    public void cmdMove(String clientID, Move direction) throws IOException {
        
        LocPair ret, currentPos, initialPos;
        String serverMessage;
        
        currentPos = s.getCurrentPosition(clientID);
        initialPos = new LocPair(-2, -2);
        initialPos.x = currentPos.x;
        initialPos.y = currentPos.y;

        switch(direction) {
            case UP:
                ret = s.updatePosition(clientID, 0, 1);
                if (ret == initialPos){                   
                    serverMessage = "You could not make the move, " + clientID; 
                    sendMessage(serverMessage);
                } else {                                
                    serverMessage = "Move successful. " + clientID + ", your new position is: (" + ret.x + ", " + ret.y + ").";
                    sendMessage(serverMessage);         
                }
                break;
            case DOWN:
                ret = s.updatePosition(clientID, 0, -1);
                if (ret == initialPos){                   
                    serverMessage = "You could not make the move, " + clientID; 
                    sendMessage(serverMessage);
                } else {                                
                    serverMessage = "Move successful. " + clientID + ", your new position is: (" + ret.x + ", " + ret.y + ").";
                    sendMessage(serverMessage);         
                }
                break;
            case RIGHT:
                ret = s.updatePosition(clientID, 1, 0);
                if (ret == initialPos){                   
                    serverMessage = "You could not make the move, " + clientID; 
                    sendMessage(serverMessage);
                } else {                                
                    serverMessage = "Move successful. " + clientID + ", your new position is: (" + ret.x + ", " + ret.y + ").";
                    sendMessage(serverMessage);         
                }
                break;
            case LEFT:
                ret = s.updatePosition(clientID, -1, 0);
                if (ret == initialPos){                   
                    serverMessage = "You could not make the move, " + clientID; 
                    sendMessage(serverMessage);
                } else {                                
                    serverMessage = "Move successful. " + clientID + ", your new position is: (" + ret.x + ", " + ret.y + ").";
                    sendMessage(serverMessage);         
                }
                break;
            case UR:
                ret = s.updatePosition(clientID, 1, 1);
                if (ret == initialPos){                   
                    serverMessage = "You could not make the move, " + clientID; 
                    sendMessage(serverMessage);
                } else {                                
                    serverMessage = "Move successful. " + clientID + ", your new position is: (" + ret.x + ", " + ret.y + ").";
                    sendMessage(serverMessage);         
                }
                break;
            case UL:
                ret = s.updatePosition(clientID, -1, 1);
                if (ret != initialPos){                   
                    serverMessage = "Move successful. " + clientID + ", your new position is: (" + ret.x + ", " + ret.y + ").";
                    sendMessage(serverMessage);
                } else {                                
                    serverMessage = "You could not make the move, " + clientID; 
                    sendMessage(serverMessage);         
                }
                break;
            case DR:
                ret = s.updatePosition(clientID, 1, -1);
                if (ret == initialPos){                   
                    serverMessage = "You could not make the move, " + clientID; 
                    sendMessage(serverMessage);
                } else {                                
                    serverMessage = "Move successful. " + clientID + ", your new position is: (" + ret.x + ", " + ret.y + ").";
                    sendMessage(serverMessage);         
                }
                break;
            case DL:
                ret = s.updatePosition(clientID, -1, -1);
                if (ret == initialPos){                   
                    serverMessage = "You could not make the move, " + clientID; 
                    sendMessage(serverMessage);
                } else {                                
                    serverMessage = "Move successful. " + clientID + ", your new position is: (" + ret.x + ", " + ret.y + ").";
                    sendMessage(serverMessage);         
                }
                break;
            default:
                serverMessage = "Invalid movement. " + clientID;
                sendMessage(serverMessage);
                break;
        }
    }
    
    private void cmdMove(String clientID, String a) throws IOException {
        //System.out.println("entrei na função!!! " + clientID);
        cmdMove(clientID, Move.valueOf(a));
    }
        
    /**
     * Handles clients commands like movements and attacks.
     * @param command: Command sent by client.
     */
    public void handleCommand(String clientID, String[] command) throws IOException {
        String op = command[0];
        switch(Command.valueOf(op)) {
            case MOVE:
                cmdMove(clientID, command[1]);
                break;
            case SHOW:
                System.out.println(s.toString());
                System.exit(0);
                break;
            default:
                break;
        }
    }
    
    
    /**
     * Handle client connection while buffer is being used. Receives and 
     * re-passes commands from players.
     */
    public void run() {
        String data = null;
        String clientAddress = client.getInetAddress().getHostAddress();
        String clientPort = ((Integer)client.getPort()).toString();
        clientID = clientAddress + "/" + clientPort;        
        s = Server.getServerRef();
        
        if(s.randomPosition(clientID) == Status.FAILED) {
            // not using this thread.
            return;
        }
        
        System.out.println("Client <" + clientID + "> logged in!");
        
        // receives messages from the player with commands to be executed on the
        // server, like movements or attacks
        try {
            BufferedReader in = new BufferedReader(
                new InputStreamReader(client.getInputStream()));
            while ( (data = in.readLine()) != null ) {
                String[] command = data.split(" ");
                System.out.println("\r\nMessage from " + clientPort + ": " + data + " -- " + command[0]);
                handleCommand(clientID, command);
            }
        } catch (IOException ex) {
            Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}