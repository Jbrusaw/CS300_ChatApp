import java.io.*;
import java.net.*;
import java.util.LinkedList;

public class Server extends Thread {
    public static void main(String args[])throws IOException {
        ServerSocket ss = new ServerSocket(666);
        LinkedList<String> oul = new LinkedList<>();
        TreeNode users = new TreeNode();
        System.out.println("Listening for connections on Port 666...\n");
        while(true){
            try{
                Socket s = ss.accept();
                System.out.println("New client connected: " + s);
                Thread t = new ClientThread(s,oul, users);
                t.start();
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}

class TreeNode{
    private String user;
    private String pass;
    private TreeNode left;
    private TreeNode right;

    TreeNode(){
        user = "Admin";
        pass = "Password";
        left = null;
        right = null;
    }

    TreeNode(String user, String pass){
        this.user = user;
        this.pass = pass;
        this.left = null;
        this.right = null;
    }

    public boolean addToTree(TreeNode curr, String user, String pass) {
        if (curr.user.equalsIgnoreCase(user))
            return false;
        if (curr.user.compareTo(user) > 0) {
            if (curr.right == null) {
                curr.right = new TreeNode(user, pass);
                return true;
            }
            return addToTree(curr.right, user, pass);
        }
        if (curr.left == null) {
            curr.left = new TreeNode(user, pass);
            return true;
        }
        return addToTree(curr.left, user, pass);
    }

    public boolean login(TreeNode curr, String user, String pass){
        if (curr == null)
            return false;
        if (curr.user.equalsIgnoreCase(user)){
            if (curr.pass.equals(pass))
                return true;
            return false;
        }
        if (curr.user.compareTo(user) > 0)
            return login(curr.right, user, pass);
        return login(curr.left,user,pass);
    }
}

class ClientThread extends Thread{
    private Socket s;
    private DataInputStream in;
    private DataOutputStream out;
    private LinkedList<String> oul;
    private TreeNode users;

    public ClientThread(Socket s, LinkedList oul, TreeNode users)throws IOException{
        this.s = s;
        this.in = new DataInputStream(s.getInputStream());
        this.out = new DataOutputStream(s.getOutputStream());
        this.oul = oul;
        this.users = users;
    }

    public void run() {
        String user;
        String pass;
        String options = "0";
        while (options.equals("0")){
            try {
                out.writeUTF("Press 1 to login, or 2 to create a new account");
                options = in.readUTF();
                if (options.equals("1")){
                    out.writeUTF("Enter Username: ");
                    user = in.readUTF();
                    out.writeUTF("Enter Password: ");
                    pass = in.readUTF();
                    if (users.login(users,user,pass)){
                        out.writeUTF("Welcome to ChatApp!");
                        oul.add(user);
                    }
                    else{
                        out.writeUTF("Sorry, login unsuccessful, try again");
                        options = "0";
                    }
                }
                else if (options.equals("2")){
                    out.writeUTF("Enter Username: ");
                    user = in.readUTF();
                    out.writeUTF("Enter Password: ");
                    pass = in.readUTF();
                    if (users.addToTree(users,user,pass)){
                        out.writeUTF("Welcome to ChatApp!");
                        oul.add(user);
                    }
                    else{
                        out.writeUTF("Sorry, username already in use, try again");
                        options = "0";
                    }
                }
                else
                    out.writeUTF("Invalid selection, try again");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            out.writeUTF("To see online user list, type \"o\"");
            options = in.readUTF();
            if (options.equalsIgnoreCase("o")){
                out.writeUTF("Online Users:");
                for (String o:oul)
                    out.writeUTF(o);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}