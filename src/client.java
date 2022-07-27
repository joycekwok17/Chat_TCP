import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class client {
    private String notif = " *** ";

    private ObjectInputStream sInput;

    private ObjectOutputStream sOutput;

    private Socket socket;

    private int port;

    private String username, server;

    public client() {
    }

    public client(int port, String username, String server) {
        this.port = port;
        this.username = username;
        this.server = server;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /*
     * display a msg to the console
     */
    private void display(String msg) {
        System.out.println(msg);
    }

    void sendMessage(ChatMessage msg){
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            display("Exception writing to server: "+e);
        }
    }

    private void disconnect(){
        try {
            if(sInput != null){
                sInput.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try{
            if(sOutput != null){
                sOutput.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try{
            if(socket != null){
                socket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean start(){

        try{
            socket = new Socket(server, port);
        } catch (Exception e) {
            display("Error when connecting to server: "+ e);
            return false;
        }

        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);

        try{
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
            display("Created both data streams");
        } catch (IOException e) {
            display("Exception creating new Input/output Streams: " + e);
            return false;
        }

        new ListenFromServer().start();

        try {
            sOutput.writeObject(username);
        } catch (IOException e) {
            display("Exception doing login : " + e);
            disconnect();
            return false;
        }

        return true;
    }

    public static void main(String[] args) {
        // default values if not entered
        int portNumber = 1500;
        String serverAddress ="localhost";
        String userName = "Anonymous";
        Scanner scan = new Scanner(System.in);

        System.out.println("Enter the username: ");
        userName = scan.nextLine();

        switch(args.length) {
            case 3:
                // for > javac Client username portNumber serverAddr
                serverAddress = args[2];
            case 2:
                // for > javac Client username portNumber
                try {
                    portNumber = Integer.parseInt(args[1]);
                }
                catch(Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                    return;
                }
            case 1:
                // for > javac Client username
                userName = args[0];
            case 0:
                // for > java Client
                break;
            // if number of arguments are invalid
            default:
                System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                return;
        }

        client client = new client(portNumber, userName, serverAddress);
        // try to connect to server, return if not connected
        if(!client.start()) return;

        System.out.println("\nHello.! Welcome to the chatroom.");
        System.out.println("Instructions:");
        System.out.println("1. Simply type the message to send broadcast to all active clients");
        System.out.println("2. Type '@username<space>yourmessage' without quotes to send message to desired client");
        System.out.println("3. Type 'WHOISIN' without quotes to see list of active clients");
        System.out.println("4. Type 'LOGOUT' without quotes to logoff from server");

        // infinite loop to get the input from the user
        while(true) {
            System.out.print("> ");

            // read message from user
            String msg = scan.nextLine();

            // logout if message is LOGOUT
            if(msg.equalsIgnoreCase("LOGOUT")) {
                client.sendMessage(new ChatMessage("",ChatMessage.LOGOUT));
                break;
            }
            // message to check who are present in chatroom
            else if(msg.equalsIgnoreCase("WHOISIN")) {
                client.sendMessage(new ChatMessage("",ChatMessage.WHOISIN));
            }
            // regular text message
            else {
                client.sendMessage(new ChatMessage(msg,ChatMessage.MESSAGE));
            }
        }
    }
    // a class that waits for msg from server
    class ListenFromServer extends Thread{

        @Override
        public void run() {

            while (true) {

                try {
                    String msg = (String) sInput.readObject();
                    display(msg);
                    display(">");
                } catch (IOException e) {
                    display(notif + "Server has closed the connection: "+e+notif);
                    break;
              } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
