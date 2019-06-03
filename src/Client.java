import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class Client extends Thread {

    private static DataOutputStream out;
    private static DataInputStream in;
    private static String serverName = "localhost";
    private static int port = 300;
    private static DefaultListModel<String> oul;
    private static DefaultListModel<String> mainlog;

    public static void main(String[] args) {
        try {
            Socket client = new Socket(serverName, port);
            out = new DataOutputStream(client.getOutputStream());
            in = new DataInputStream(client.getInputStream());
            new LoginWindow();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class LoginWindow implements ActionListener {
        JTextField user;
        JPasswordField pass;
        JLabel header;
        JButton log, create, group;
        JFrame f;
        JList<String> online;
        JList<String> userlog;

        LoginWindow() {

            f = new JFrame("Welcome to ChatApp");
            user = new JTextField(20);
            pass = new JPasswordField(20);
            log = new JButton("Login");
            create = new JButton("Create Account");
            log.addActionListener(this);
            create.addActionListener(this);
            header = new JLabel("Please enter credentials");
            header.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel j1 = new JPanel();
            j1.setLayout(new FlowLayout(FlowLayout.RIGHT));
            j1.add(new JLabel("Username:"));
            j1.add(user);

            JPanel j2 = new JPanel();
            j2.setLayout(new FlowLayout(FlowLayout.RIGHT));
            j2.add(new JLabel("Password:"));
            j2.add(pass);

            JPanel j3 = new JPanel();
            j3.setLayout(new FlowLayout());
            j3.add(log);
            j3.add(create);

            f.setLayout(new BoxLayout(f.getContentPane(), BoxLayout.Y_AXIS));
            f.add(header);
            f.add(j1);
            f.add(j2);
            f.add(j3);
            f.pack();
            f.setVisible(true);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }

        public void actionPerformed(ActionEvent ae) {
            if (ae.getSource() == log) {
                String us = user.getText();
                String ps = new String(pass.getPassword());
                if (us.length() < 3)
                    header.setText("Username must be 3+ characters");
                else if (ps.length() < 3)
                    header.setText("Password must be 3+ characters");
                else if (login(us, ps))
                    LaunchOUL();
                else
                    header.setText("Login failed. Try again");
            } else if (ae.getSource() == create) {
                String us = user.getText();
                String ps = new String(pass.getPassword());
                if (us.length() < 3)
                    header.setText("Username must be 3+ characters");
                else if (ps.length() < 3)
                    header.setText("Password must be 3+ characters");
                else if (createAccount(us, ps))
                    LaunchOUL();
                else
                    header.setText("Username taken. Try again");
            } else if (ae.getSource() == group) {
                new ChatWindow(online.getSelectedValuesList());
            }
        }

        private Boolean login(String us, String ps) {
            try {
                out.writeUTF("login");
                out.writeUTF(us);
                out.writeUTF(ps);
                return in.readBoolean();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        private Boolean createAccount(String us, String ps) {
            try {
                out.writeUTF("create");
                out.writeUTF(us);
                out.writeUTF(ps);
                return in.readBoolean();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        private void LaunchOUL() {
            f.dispose();
            JOptionPane.showMessageDialog(null, "Login Successful!");
            oul = new DefaultListModel<>();
            mainlog = new DefaultListModel<>();
            try {
                int oulnum = in.readInt();
                for (int i = 0; i < oulnum; i++) {
                    oul.addElement(in.readUTF());
                }
                online = new JList(oul);
                oul.removeElement(user);
                userlog = new JList(mainlog);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Thread t = new Input();
            t.start();
            f.removeAll();
            f = new JFrame("ChatApp");
            header.setText("Select user(s) you'd like to chat with: ");
            group = new JButton("Create Chat Group");
            group.setAlignmentX(Component.CENTER_ALIGNMENT);
            group.addActionListener(this);
            JScrollPane list = new JScrollPane(online);
            list.setAlignmentX(Component.RIGHT_ALIGNMENT);
            JPanel listPane = new JPanel();
            listPane.setLayout(new BoxLayout(listPane, BoxLayout.LINE_AXIS));
            listPane.add(list);
            JScrollPane loginlist = new JScrollPane(userlog);
            listPane.add(loginlist);
            listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            f.add(header);
            f.add(listPane);
            f.add(group);
            f.add(Box.createRigidArea(new Dimension(0, 5)));
            f.setLayout(new BoxLayout(f.getContentPane(), BoxLayout.Y_AXIS));
            f.pack();
            f.setVisible(true);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
    }

    static class Input extends Thread {

        public void run() {
            try {
                while (true) {
                    int parse = in.readInt();
                    System.out.println(parse);
                    switch (parse) {
                        case 1:
                            newLogon();
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        private void newLogon() throws IOException {
            String input = in.readUTF();
            mainlog.addElement(input + " has logged on!");
            oul.addElement(input);
        }
    }

    static class ChatWindow implements ActionListener {
        JTextArea msg, chats;
        List<String> users;

        ChatWindow(List<String> users) {
            this.users = users;
            if (users.size() < 1) {
                JOptionPane.showMessageDialog(null, "You must select at least 1 user!");
                return;
            }
            String head = "Chatting with ";
            for (String s : users) {
                head += s + ", ";
            }
            head = head.substring(0, head.length() - 2);

            chats = new JTextArea();
            chats.setEditable(false);
            chats.setColumns(30);
            chats.setRows(10);
            msg = new JTextArea();
            msg.setColumns(30);
            msg.setRows(10);
            JButton send = new JButton("Send");
            send.addActionListener(this);

            JPanel jp1 = new JPanel();
            jp1.setLayout(new FlowLayout());
            jp1.add(new JLabel(head));
            jp1.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel jp2 = new JPanel();
            jp2.setLayout(new FlowLayout());
            jp2.add(chats);
            jp2.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel jp3 = new JPanel();
            jp3.setLayout(new FlowLayout());
            jp3.add(msg);
            jp3.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel jp4 = new JPanel();
            jp4.setLayout(new FlowLayout());
            jp4.add(send);
            jp4.setAlignmentX(Component.CENTER_ALIGNMENT);

            JFrame f = new JFrame(head);
            f.add(jp1);
            f.add(jp2);
            f.add(jp3);
            f.add(jp4);
            f.setLayout(new BoxLayout(f.getContentPane(), BoxLayout.Y_AXIS));
            f.pack();
            f.setVisible(true);
        }

        public void actionPerformed(ActionEvent ae) {
            //TODO: make the 'send' button do something useful
            chats.append(msg.getText() + '\n');
            msg.setText(null);
        }
    }
}
