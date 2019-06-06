/*  Server.java John Brusaw
    6/5/2019 CS300 ChatApp project

    This is the server software for the ChatApp. It works by
    continuously accepting sockets from new clients and
    creating new threads for each client. Both the client
    and server communicate by sending a string indicating
    what they're trying to do. The string is parsed on either
    end and the appropriate functions are called.
 */


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Server extends Thread {
    //accounts is a hashmap that stores all account usernames
    //and associated passwords
    private static HashMap<String, String> accounts = new HashMap<>();

    //sw is a server GUI
    private static ServerWindow sw;

    //chatID is used to track open chats between users
    private static int chatID = 1;

    //oul is a list of online users
    private static ArrayList<User> oul = new ArrayList<>();

    //chats is a hashmap used to track active chat windows
    private static HashMap<Integer, User[]> chats = new HashMap<>();

    public static void main(String[] args) throws IOException {
        //opens a serversocket and server GUI
        int port = 300;
        ServerSocket ss = new ServerSocket(port);
        sw = new ServerWindow(port);

        //imports account usernames and passwords from accounts.txt
        try {
            BufferedReader reader = new BufferedReader(new FileReader("accounts.txt"));
            String input;
            while ((input = reader.readLine()) != null) {
                accounts.put(input, reader.readLine());
            }
        } catch (FileNotFoundException e) {
            System.out.println("No accounts found");
        }

        //runs an infinite loop waiting for server connections.
        //new connections get their own threads
        while (true) {
            try {
                Socket s = ss.accept();
                Thread t = new ClientThread(s);
                t.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //User is a class that tracks usernames and associated output streams
    static class User {
        String username;
        DataOutputStream out;

        User(String user, DataOutputStream out) {
            this.username = user;
            this.out = out;
        }

        Boolean match(String user) {
            return (this.username.equalsIgnoreCase(user));
        }

        synchronized void sendMsg(String msg) {
            try {
                out.writeUTF(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //ServerWindow is a server GUI
    static class ServerWindow implements ActionListener {
        JLabel header;
        JFrame f;

        //oulList is a list of online users that gets wrapped in a JList
        //updates to oulList are automatically updated this way
        static DefaultListModel<String> oulList = new DefaultListModel<>();

        //creates a GUI to display online users and give the option to quit
        ServerWindow(int port) {
            f = new JFrame("ChatApp Server");
            header = new JLabel("Online users: " + Server.oul.size(), JLabel.CENTER);
            JButton exit = new JButton("Exit");
            exit.addActionListener(this);
            exit.setAlignmentX(Component.CENTER_ALIGNMENT);
            header.setAlignmentX(Component.CENTER_ALIGNMENT);
            for (User u : oul) {
                oulList.addElement(u.username);
            }
            JList<String> jl = new JList<>(oulList);
            jl.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel temp = new JLabel("Listening for connections on port " + port, JLabel.CENTER);
            temp.setAlignmentX(Component.CENTER_ALIGNMENT);
            JPanel button = new JPanel();
            button.add(exit);
            button.setBorder(BorderFactory.createEmptyBorder(0, 5, 15, 5));
            f.add(temp);
            f.add(Box.createRigidArea(new Dimension(250, 5)));
            f.add(header);
            f.add(Box.createRigidArea(new Dimension(250, 5)));
            f.add(jl);
            f.add(Box.createRigidArea(new Dimension(250, 5)));
            f.add(button);
            f.add(Box.createRigidArea(new Dimension(250, 5)));
            f.setLayout(new BoxLayout(f.getContentPane(), BoxLayout.Y_AXIS));
            f.pack();
            f.setVisible(true);

            //X is disabled because it would be difficult to store user
            //account data otherwise
            f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        }

        //when the quit button is clicked, store all account data
        //to accounts.txt and exit the program
        public void actionPerformed(ActionEvent ae) {
            try {
                PrintWriter writer = new PrintWriter("accounts.txt");
                for (String s : accounts.keySet()) {
                    writer.println(s);
                    writer.println(accounts.get(s));
                }
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            System.exit(1);
        }

        //functions call this when someone logs on or off.
        //it updates the number of users and re-sizes the window
        private void update() {
            header.setText("Online users: " + Server.oul.size());
            f.pack();
        }
    }

    //each connected client gets their own thread
    static class ClientThread extends Thread {
        Socket s;
        User client;
        DataOutputStream out;
        DataInputStream in;

        ClientThread(Socket s) {
            this.s = s;
        }

        public void run() {
            try {
                in = new DataInputStream(s.getInputStream());
                out = new DataOutputStream(s.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            //user is set to "init" to avoid compiler warnings.
            //the while() loop below won't terminate until user is set
            String pass, option, user = "init";

            //start a loop to get user info from the client. The loop
            //won't terminate until the client provides valid login
            //info or terminates the connection
            Boolean login = false;
            try {
                while (!login) {
                    try {
                        //read info
                        option = in.readUTF();
                        user = in.readUTF();
                        pass = in.readUTF();

                        //process selection
                        if (option.equals("login")) {
                            login = attemptLogin(user, pass);
                        } else if (option.equals("create account")) {
                            login = attemptCreate(user, pass);
                        }

                        //let the client know the result
                        out.writeBoolean(login);
                    } catch (IOException e) {
                        break;
                    }
                }

                //when the user has logged in, we send them
                //online user information
                out.writeInt(Server.oul.size());
                for (User u : Server.oul)
                    out.writeUTF(u.username);

                //create a new User from the info
                client = new User(user, out);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //send everyone else a message that the user has logged on
            for (User u : Server.oul) {
                u.sendMsg("login");
                u.sendMsg(client.username);
            }

            //add user to online user list, update sw, and start listening
            Server.oul.add(client);
            ServerWindow.oulList.addElement(client.username);
            Server.sw.update();
            listen();
        }

        //if user tries to login
        Boolean attemptLogin(String user, String pass) {
            //check all online users and make sure they're not already online
            for (User u : Server.oul) {
                if (u.match(user)) {
                    return false;
                }
            }

            //then check if they're account username exists
            if (Server.accounts.containsKey(user.toLowerCase())) {
                //and if the password matches
                return pass.equals(Server.accounts.get(user.toLowerCase()));
            }

            //if no account found, return false
            return false;
        }

        //if the user wants to create a new account
        Boolean attemptCreate(String user, String pass) {
            //if the username already exists, return false
            if (Server.accounts.containsKey(user.toLowerCase()))
                return false;

            //otherwise create the account and return true
            Server.accounts.put(user.toLowerCase(), pass);
            return true;
        }

        //this function just listens to the user and reacts according to input
        void listen() {
            try {
                String input = "init";
                while (!input.equals("quit")) {
                    input = in.readUTF();
                    switch (input) {
                        case "group":
                            newGroup();
                            break;
                        case "msg":
                            newMsg();
                            break;
                        case "quit":
                            quit();
                            break;
                        default:
                            break;
                    }
                }
            } catch (IOException e) {
                //if the user disconnects unexpectedly, just quit
                quit();
            }
        }

        //if the user creates a new chat group
        synchronized void newGroup() throws IOException {
            //sending the "group" message back tells the client to
            //be prepared for new info
            client.sendMsg("group");

            //send the chatID of this new group
            client.sendMsg(String.valueOf(Server.chatID));

            //number of users in this group
            int num = in.readInt();

            //gathers User info for every user and stores it
            User[] chatUsers = new User[num];
            for (int i = 0; i < num; i++) {
                String username = in.readUTF();
                for (User u : Server.oul) {
                    if (u.match(username))
                        chatUsers[i] = u;
                }
            }

            //stores info in chats
            Server.chats.put(Server.chatID, chatUsers);

            //increments chatID so next group gets new id number
            Server.chatID++;
        }

        //if user wants to send a message
        void newMsg() throws IOException {
            //get chatID of group and the new message
            int id = in.readInt();
            String msg = in.readUTF();

            //looks up the group info from the chatID and sends the message to each user
            for (User u : Server.chats.get(id)) {
                //let the user know a message is incoming
                u.sendMsg("msg");

                //send the chatID
                u.sendMsg(String.valueOf(id));

                //send the names of the other users in the chat
                //this is used for display purposes on the client side
                u.sendMsg(String.valueOf(Server.chats.get(id).length));
                for (User u2 : Server.chats.get(id)) {
                    u.sendMsg(u2.username);
                }

                //send the message
                u.sendMsg(msg);
            }
        }

        //if the user quits
        void quit() {
            //remove the user from the online user list
            ServerWindow.oulList.removeElement(client.username);
            Server.oul.remove(client);
            Server.sw.update();

            //and let everyone else know the user has logged off
            for (User u : Server.oul) {
                u.sendMsg("logoff");
                u.sendMsg(client.username);
            }
        }
    }
}