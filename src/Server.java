import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server extends Thread {
    private static TreeNode users;
    private static ServerWindow sw;
    private static int chatID = 1;
    private static HashMap<String, DataOutputStream> oul = new HashMap<>();
    private static HashMap<Integer, DataOutputStream[]> chats = new HashMap<>();

    public static void main(String[] args) throws IOException {
        int port = 300;
        ServerSocket ss = new ServerSocket(port);
        users = new TreeNode();
        sw = new ServerWindow(port);
        while(true){
            try{
                Socket s = ss.accept();
                Thread t = new ClientThread(s);
                t.start();
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    static class ServerWindow implements ActionListener {
        JLabel header;
        JButton exit;
        JFrame f;
        JList jl;

        ServerWindow(int port) {
            f = new JFrame("ChatApp Server");
            header = new JLabel("Online users: " + oul.size(), JLabel.CENTER);
            exit = new JButton("Exit");
            jl = new JList(oul.keySet().toArray());
            exit.addActionListener(this);
            exit.setAlignmentX(Component.CENTER_ALIGNMENT);
            header.setAlignmentX(Component.CENTER_ALIGNMENT);
            jl.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel temp = new JLabel("Listening for connections on port " + port, JLabel.CENTER);
            temp.setAlignmentX(Component.CENTER_ALIGNMENT);
            f.add(temp);
            f.add(Box.createRigidArea(new Dimension(250, 5)));
            f.add(header);
            f.add(Box.createRigidArea(new Dimension(250, 5)));
            f.add(jl);
            f.add(Box.createRigidArea(new Dimension(250, 5)));
            f.add(exit);
            f.add(Box.createRigidArea(new Dimension(250, 5)));
            f.setLayout(new BoxLayout(f.getContentPane(), BoxLayout.Y_AXIS));
            f.pack();
            f.setVisible(true);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }

        public void actionPerformed(ActionEvent ae) {
            System.exit(1);
        }

        private void update() {
            header.setText("Online users: " + oul.size());
            jl.setListData(oul.keySet().toArray());
            f.pack();
        }
    }

    static class TreeNode {
        private String user;
        private String pass;
        private TreeNode left;
        private TreeNode right;

        TreeNode() {
            user = "Admin";
            pass = "Password";
            left = null;
            right = null;
        }

        TreeNode(String user, String pass) {
            this.user = user;
            this.pass = pass;
            this.left = null;
            this.right = null;
        }

        private boolean create(TreeNode curr, String user, String pass) {
            if (curr.user.equals(user))
                return false;
            if (curr.user.compareTo(user) > 0) {
                if (curr.right == null) {
                    curr.right = new TreeNode(user, pass);
                    return true;
                }
                return create(curr.right, user, pass);
            }
            if (curr.left == null) {
                curr.left = new TreeNode(user, pass);
                return true;
            }
            return create(curr.left, user, pass);
        }

        private boolean login(TreeNode curr, String user, String pass) {
            if (curr == null)
                return false;
            if (curr.user.equals(user)) {
                return curr.pass.equals(pass);
            }
            if (curr.user.compareTo(user) > 0)
                return login(curr.right, user, pass);
            return login(curr.left, user, pass);
        }
    }

    static class ClientThread extends Thread {
        Socket s;
        static private String user;
        static DataOutputStream out;
        DataInputStream in;

        private ClientThread(Socket s) {
            this.s = s;
            user = null;
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
                            if (!oul.containsKey(user))
                                login = users.login(users, user, pass);
                        } else if (option.equals("create")) {
                            login = users.create(users, user, pass);
                        }
                        out.writeBoolean(login);
                    } catch (IOException e) {
                        break;
                    }
                }
                out.writeInt(oul.size());
                for (String s : oul.keySet())
                    out.writeUTF(s);
            } catch (IOException e) {
                oul.remove(user);
                sw.update();
            }
            for (String s : oul.keySet()) {
                synchronized (oul.get(s)) {
                    try {
                        oul.get(s).writeInt(1);
                        oul.get(s).writeUTF(user);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            oul.put(user, out);
            sw.update();

            int run = 1;
            Thread t = new HeartBeat();
            t.start();
            try {
                while (run > 0) {
                    run = in.readInt();
                    synchronized (out) {
                        switch (run) {
                            case 1:
                                newChat();
                                break;
                            case 2:
                                newMsg();
                                break;

                        }
                    }

                }
            } catch (IOException e) {
                oul.remove(user);
                sw.update();
                for (String s : oul.keySet()) {
                    synchronized (oul.get(s)) {
                        try {
                            oul.get(s).writeInt(2);
                            oul.get(s).writeUTF(user);
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            }
        }

        synchronized void newChat() throws IOException {
            out.writeInt(4);
            System.out.println("Assigned ChatID: " + chatID);
            out.writeInt(chatID);
            int num = in.readInt();
            DataOutputStream[] chatusers = new DataOutputStream[num];
            for (int i = 0; i < num; i++) {
                chatusers[i] = oul.get(in.readUTF());
            }
            chats.put(chatID, chatusers);
            System.out.println("Current Chat HashMap: " + chats);
            chatID++;
            System.out.println("New ChatID: " + chatID);
        }

        synchronized void newMsg() throws IOException {
            int id = in.readInt();
            String msg = in.readUTF();
            System.out.println(id + " " + chats.get(id));
            for (DataOutputStream o : chats.get(id)) {
                o.write(3);
                o.writeInt(id);
                if (in.readBoolean() == false) {
                    o.writeInt(chats.get(id).length);
                    for (DataOutputStream o2 : chats.get(id))
                        o.writeUTF(getUser(o2));
                }
                o.writeUTF(msg);
            }
        }

        String getUser(DataOutputStream o) {
            for (String u : oul.keySet()) {
                if (o == oul.get(u))
                    return u;
            }
            return null;
        }

        static class HeartBeat extends Thread {
            public void run() {
                try {
                    while (true) {
                        sleep(60 * 1000);
                        synchronized (out) {
                            out.writeInt(0);
                        }
                    }
                } catch (IOException e) {
                    oul.remove(user);
                    sw.update();
                    for (String s : oul.keySet()) {
                        synchronized (oul.get(s)) {
                            try {
                                oul.get(s).writeInt(2);
                                oul.get(s).writeUTF(user);
                            } catch (IOException e2) {
                                e2.printStackTrace();
                            }
                        }
                    }
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }
}

//TODO: Add a 'heartbeat' to monitor socket health
//TODO: Adjust OUL to add output datastreams
//TODO: Case De-sensitize username