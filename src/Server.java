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
    private static HashMap<String, String> accounts = new HashMap<>();
    private static ServerWindow sw;
    private static int chatID = 1;
    private static ArrayList<User> oul = new ArrayList<>();
    private static HashMap<Integer, User[]> chats = new HashMap<>();

    public static void main(String[] args) throws IOException {
        int port = 300;
        ServerSocket ss = new ServerSocket(port);
        sw = new ServerWindow(port);
        try {
            BufferedReader reader = new BufferedReader(new FileReader("accounts.txt"));
            String input;
            while ((input = reader.readLine()) != null) {
                accounts.put(input, reader.readLine());
            }
        } catch (FileNotFoundException e) {
            System.out.println("No accounts found");
        }
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

    static class ServerWindow implements ActionListener {
        JLabel header;
        JFrame f;
        static DefaultListModel<String> oulList = new DefaultListModel<>();

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
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }

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

        private void update() {
            header.setText("Online users: " + Server.oul.size());
            f.pack();
        }
    }

    static class ClientThread extends Thread {
        Socket s;
        String user;
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
            String pass, option;
            Boolean login = false;
            try {
                while (!login) {
                    try {
                        option = in.readUTF();
                        user = in.readUTF();
                        pass = in.readUTF();
                        if (option.equals("login")) {
                            login = attemptLogin(user, pass);
                        } else if (option.equals("create")) {
                            login = attemptCreate(user, pass);
                        }
                        out.writeBoolean(login);
                    } catch (IOException e) {
                        break;
                    }
                }
                out.writeInt(Server.oul.size());
                for (User u : Server.oul)
                    out.writeUTF(u.username);
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (User u : Server.oul) {
                u.sendMsg("login");
                u.sendMsg(user);
            }
            client = new User(user, out);
            Server.oul.add(client);
            ServerWindow.oulList.addElement(user);
            Server.sw.update();
            listen();
        }

        Boolean attemptLogin(String user, String pass) {
            for (User u : Server.oul) {
                if (u.match(user)) {
                    return false;
                }
            }
            if (Server.accounts.containsKey(user.toLowerCase())) {
                return pass.equals(Server.accounts.get(user.toLowerCase()));
            }
            return false;
        }

        Boolean attemptCreate(String user, String pass) {
            if (Server.accounts.containsKey(user.toLowerCase()))
                return false;
            Server.accounts.put(user.toLowerCase(), pass);
            return true;
        }

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
                ServerWindow.oulList.removeElement(client.username);
                Server.oul.remove(client);
                Server.sw.update();
                for (User u : Server.oul) {
                    u.sendMsg("logoff");
                    u.sendMsg(user);
                }
            }
        }

        synchronized void newGroup() throws IOException {
            client.sendMsg("group");
            client.sendMsg(String.valueOf(Server.chatID));
            int num = in.readInt();
            User[] chatUsers = new User[num];
            for (int i = 0; i < num; i++) {
                String username = in.readUTF();
                for (User u : Server.oul) {
                    if (u.match(username))
                        chatUsers[i] = u;
                }
            }
            Server.chats.put(Server.chatID, chatUsers);
            Server.chatID++;
        }

        void newMsg() throws IOException {
            int id = in.readInt();
            String msg = in.readUTF();
            for (User u : Server.chats.get(id)) {
                u.sendMsg("msg");
                u.sendMsg(String.valueOf(id));
                u.sendMsg(String.valueOf(Server.chats.get(id).length));
                for (User u2 : Server.chats.get(id)) {
                    u.sendMsg(u2.username);
                }
                u.sendMsg(msg);
            }
        }

        void quit() {
            ServerWindow.oulList.removeElement(client.username);
            Server.oul.remove(client);
            Server.sw.update();
            for (User u : Server.oul) {
                u.sendMsg("logoff");
                u.sendMsg(user);
            }
        }
    }

}