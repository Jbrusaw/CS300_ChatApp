import java.net.*;
import java.io.*;
import java.util.*;

public class Client extends Thread{
    public static void main(String [] args) {
        String serverName = "localhost";
        int port = 666;
        try {
            System.out.println("Connecting to " + serverName + " on port " + port);
            Socket client = new Socket(serverName, port);

            System.out.println("Just connected to " + client.getRemoteSocketAddress());

            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            DataInputStream in = new DataInputStream(client.getInputStream());

            Thread i = new Input(in, client);
            i.start();

            Thread o = new Output(out);
            o.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Input extends Thread{
    private DataInputStream in;
    Socket s;

    public Input(DataInputStream in, Socket s){
        this.in = in;
        this.s = s;
    }

    public void run(){
        String input = "init";
        while (!input.equals("quit")) {
            try {
                input = in.readUTF();
                System.out.println(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            in.close();
            s.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}

class Output extends Thread{
    private DataOutputStream out;

    public Output(DataOutputStream out){
        this.out = out;
    }

    public void run(){
        String output = "init";
        Scanner in = new Scanner(System.in);
        while (!output.equals("quit")){
            try {
                output = in.nextLine();
                out.writeUTF(output);
            } catch(IOException e){
                e.printStackTrace();
            }
        }
        try{
            out.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}