import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class server {

    private static int uniqueId;

    private ArrayList<ClientThread> al;

    private SimpleDateFormat sdf;

    private int port;

    // to check if server is running
    private boolean keepGoing;

    private String notif="***";

    public server() {
    }

    public server(int port) {
        this.port = port;
        // display date
        sdf = new SimpleDateFormat("HH:mm::ss");
        al = new ArrayList<ClientThread>();
    }

    public void start() {

        keepGoing = true;
        ServerSocket serverSocket;

        try {
            serverSocket = new ServerSocket(port);

            // infinite loop to wait for connections, while server is alive
            while(keepGoing){
                display("Server waiting for clients on port "+ port +".");

                // accept connection if requested from client
                Socket socket = serverSocket.accept();

                // break if server is dead
                if(!keepGoing) break;

                // if client is connected, create its thread
                ClientThread ct = new ClientThread(socket);
                al.add(ct);

                // this will call run() method
                ct.start();
            }
            // try to stop the server
            try {
                serverSocket.close();
                for(int i = 0; i < al.size(); ++i) {
                    ClientThread tc = al.get(i);
                    try {
                        // close all data streams and socket
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    }
                    catch(IOException ioE) {
                    }
                }
            }
            catch(Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        }
        catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }

    // stop the server
    protected void stop() {

        keepGoing = false;

        try {
            new Socket("localhost", port);
        }
        catch(Exception e){System.out.println(e);}
    }

    // Display an event to the console
    private void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);
    }

    // if client sent LOGOUT message to exit
    synchronized void remove(int id) {

        String disconnectedClientName = "";

        // scan the array list until we found the Id
        for(ClientThread ct : al){
            if(ct.id == id){
                al.remove(ct);
                disconnectedClientName =ct.getUsername();
                break;
            }
        }
        broadcast(notif + disconnectedClientName + " left the chat room." + notif);
    }

    private synchronized boolean broadcast(String message) {
        // add timestamp to the message
        String time = sdf.format(new Date());

        // to check if message is private i.e. client to client message
        String[] w = message.split(" ",3);

        boolean isPrivate = false;

        if(w[1].charAt(0)=='@')
            isPrivate=true;

        // if private message, send message to mentioned username only
        if(isPrivate)
        {
            String tocheck=w[1].substring(1);

            message=w[0]+w[2];
            String messageLf = time + " " + message + "\n";
            boolean found = false;

            // we loop to find the mentioned username
            for(ClientThread ct1 : al)
            {
                String check = ct1.getUsername();
                if(check.equals(tocheck))
                {
                    // try to write to the Client
                    // if it fails
                    // remove it from the list
                    if(!ct1.writeMsg(messageLf)) {
                        al.remove(ct1);
                        display("Disconnected Client " + ct1.username + " removed from list.");
                    }
                    // username found and delivered the message
                    found=true;
                    break;
                }
            }

            // mentioned user not found, return false
            if(!found)
            {
                return false;
            }
        }

        // if message is a broadcast message
        else
        {
            String messageLf = time + " " + message + "\n";
            // display message
            System.out.print(messageLf);

            // we loop in reverse order in case we would have to remove a Client
            // because it has disconnected
            for(int i = al.size(); --i >= 0;) {
                ClientThread ct = al.get(i);
                // try to write to the Client if it fails remove it from the list
                if(!ct.writeMsg(messageLf)) {
                    al.remove(i);
                    display("Disconnected Client " + ct.username + " removed from list.");
                }
            }
        }
        return true;
    }

    /*
     *  To run as a console application
     * > java Server
     * > java Server portNumber
     * If the port number is not specified 1500 is used
     */
    public static void main(String[] args) {

        int portNumber = 1500;

        switch(args.length){

            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                }
                catch(Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }

            case 0:
                break;

            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;
        }

        server server = new server(portNumber);
        server.start();

    }



    // One instance of this thread will run for each client
    class ClientThread extends Thread{
        int id;

        // the socket to get messages from client
        private Socket socket;

        private ObjectInputStream sInput;
        private ObjectOutputStream sOutput;
        private String username;
        private ChatMessage cm;
        private String date;

        // Constructor
        ClientThread(Socket socket){

            this.socket = socket;

            id = ++uniqueId;

            System.out.println("Thread trying to create object input/output streams ");

            try {
                // *******NOTICE!!! THE ORDER IS CRUCIAL!!!**********
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());

                // read the username
                username = (String) sInput.readObject();
                broadcast(notif + username + " has joined the chat room. " + notif);

            } catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                throw new RuntimeException(e);

            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            date = new Date().toString() + "\n";

        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }


        @Override
        // infinite loop to read and forward message
        public void run() {

            boolean keepGoing = true;
            while (keepGoing) {

                try {
                    cm = (ChatMessage) sInput.readObject();
                }
                catch (IOException e) {
                    display(username + " Exception reading Streams: "+e);
                    break;
                }
                catch (ClassNotFoundException e) {
                    break;
                }

                String msg = cm.getMessage();

                switch(cm.getType()){

                    case ChatMessage.WHOISIN:

                        writeMsg("List of users connected at time of "+ sdf.format(new Date())+ "\n");

                        for ( int i = al.size(); --i >= 0;){
                            ClientThread ct = al.get(i);
                            writeMsg((i+1) + ")" + ct.username + "since" + new Date());
                        }

                        break;

                    case ChatMessage.MESSAGE:

                        boolean isConfirmed = broadcast(username + ":" + msg);

                        if(!isConfirmed){
                            writeMsg(notif+ "Sorry, no such user exists" + notif);
                        }
                        break;

                    case ChatMessage.LOGOUT:

                        display(username + " disconnected with logout msg.");
                        // stop client
                        keepGoing = false;
                        break;
                }
            }
            // if out of the loop
            // then disconnected and remove from client list
            remove(id);
            close();

        }


        // write a String to the Client output stream
        boolean writeMsg(String msg){

            if(!socket.isConnected()){
                close();
                return false;
            }

            try {
                sOutput.writeObject(msg);
            } catch (IOException e) {
                display(notif + "Error sending msg to "+ username + notif);
                display(e.toString());
            }
            return true;
        }

        // close input, output, socket
        private void close() {
            try {
                if(sOutput != null){
                    sOutput.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                if(sInput != null){
                    sInput.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                if(socket != null){
                    socket.close();
                }
            }catch(IOException e){
                throw new RuntimeException(e);
            }

        }
    }

}
