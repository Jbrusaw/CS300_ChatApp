import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class Server extends Thread {
    private static LinkedList<String> oul = new LinkedList<>();
    private static LinkedList<DataOutputStream> outs = new LinkedList<>();
    private static TreeNode users;
    private static ServerWindow sw;

    public static void main(String[] args) throws IOException {
        int port = 300;
        ServerSocket ss = new ServerSocket(port);
        oul = new LinkedList<>();
        outs = new LinkedList<>();
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
            jl = new JList(oul.toArray());
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
            jl.setListData(oul.toArray());
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
        private DataInputStream in;
        private DataOutputStream out;
        private String user;

        private ClientThread(Socket s) throws IOException {
            this.in = new DataInputStream(s.getInputStream());
            this.out = new DataOutputStream(s.getOutputStream());
            user = null;
        }

        public void run() {
            String pass, option;
            Boolean login = false;
            try {
                while (!login) {
                    try {
                        option = in.readUTF();
                        user = in.readUTF();
                        pass = in.readUTF();
                        if (option.equals("login")) {
                            if (!oul.contains(user))
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
                for (String s : oul)
                    out.writeUTF(s);
                oul.add(user);
                sw.update();
            } catch (IOException e) {
                oul.remove(user);
                sw.update();
            }
            for (DataOutputStream o : outs) {
                try {
                    o.writeInt(1);
                    o.writeUTF(user);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            outs.add(out);
            //oul.remove(user);
            //sw.update();
        }
    }
}

//TODO: Add a 'heartbeat' to monitor socket health
//TODO: Adjust OUL to add output datastreams
//TODO: Case De-sensitize username