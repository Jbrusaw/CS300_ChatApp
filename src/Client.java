import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.Integer.parseInt;

public class Client extends Thread {

    public static void main(String[] args) {
        String serverName = "localhost";
        int port = 300;
        try {
            new LoginWindow(new Socket(serverName, port));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class LoginWindow implements ActionListener {
        JTextField user;
        JPasswordField pass;
        JLabel header;
        JButton log, create;
        JFrame f;
        DataInputStream in;
        DataOutputStream out;

        LoginWindow(Socket s) {
            try {
                in = new DataInputStream(s.getInputStream());
                out = new DataOutputStream(s.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

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
                else if (login(us, ps)) {
                    f.dispose();
                    new OULWindow(us, in, out);
                } else
                    header.setText("Login failed. Try again");
            } else if (ae.getSource() == create) {
                String us = user.getText();
                String ps = new String(pass.getPassword());
                if (us.length() < 3)
                    header.setText("Username must be 3+ characters");
                else if (ps.length() < 3)
                    header.setText("Password must be 3+ characters");
                else if (createAccount(us, ps)) {
                    f.dispose();
                    new OULWindow(us, in, out);
                } else
                    header.setText("Username taken. Try again");
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
    }

    static class OULWindow implements ActionListener {
        static String username;
        static DataInputStream in;
        static DataOutputStream out;
        static DefaultListModel<String> oul;
        static JList<String> online;
        static JPanel userLog;
        JButton group, quit;
        static JFrame f;
        static HashMap<Integer, ChatWindow> windows = new HashMap<>();

        OULWindow(String user, DataInputStream dIn, DataOutputStream dOut) {
            username = user;
            in = dIn;
            out = dOut;
            JOptionPane.showMessageDialog(null, "Login Successful!");
            oul = new DefaultListModel<>();
            userLog = new JPanel();
            userLog.setLayout(new BoxLayout(userLog, BoxLayout.Y_AXIS));
            try {
                int oulNum = in.readInt();
                for (int i = 0; i < oulNum; i++) {
                    oul.addElement(in.readUTF());
                }
                online = new JList<>(oul);
                oul.removeElement(user);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Thread t = new Input();
            t.start();
            f = new JFrame(username);
            JLabel header = new JLabel("Select user(s) you'd like to chat with: ");
            header.setAlignmentX(Component.CENTER_ALIGNMENT);
            group = new JButton("Create Chat Group");
            group.setAlignmentX(Component.LEFT_ALIGNMENT);
            group.addActionListener(this);
            quit = new JButton("Quit");
            quit.setAlignmentX(Component.RIGHT_ALIGNMENT);
            quit.addActionListener(this);
            JPanel buttons = new JPanel();
            buttons.add(group);
            buttons.add(quit);
            buttons.setLayout(new FlowLayout());
            JScrollPane list = new JScrollPane(online);
            list.setAlignmentX(Component.CENTER_ALIGNMENT);
            JPanel listPane = new JPanel();
            listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
            listPane.add(list);
            listPane.add(new JScrollPane(userLog));
            listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            f.add(header);
            f.add(listPane);
            f.add(buttons);
            f.add(Box.createRigidArea(new Dimension(0, 5)));
            f.setLayout(new BoxLayout(f.getContentPane(), BoxLayout.Y_AXIS));
            f.pack();
            f.setVisible(true);
            f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        }

        public void actionPerformed(ActionEvent ae) {
            try {
                if (ae.getSource() == group) {
                    out.writeUTF("group");
                } else if (ae.getSource() == quit) {
                    out.writeUTF("quit");
                    System.exit(1);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        static class Input extends Thread {
            public void run() {
                try {
                    while (true) {
                        String input = in.readUTF();
                        switch (input) {
                            case "login":
                                newLogin();
                                break;
                            case "logoff":
                                newLogoff();
                                break;
                            case "msg":
                                newMessage();
                                break;
                            case "group":
                                newGroup();
                                break;
                            default:
                                break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            private void newLogin() throws IOException {
                String input = in.readUTF();
                JLabel j = new JLabel(input + " has logged on!\n");
                j.setForeground(Color.green.darker());
                userLog.add(j);
                oul.addElement(input);
                f.pack();
            }

            private void newLogoff() throws IOException {
                String input = in.readUTF();
                JLabel j = new JLabel(input + " has logged off!\n");
                j.setForeground(Color.red);
                userLog.add(j);
                oul.removeElement(input);
                f.pack();
            }

            private void newMessage() throws IOException {
                int id = parseInt(in.readUTF());
                int num = parseInt(in.readUTF());
                List<String> users = new ArrayList<>();
                for (int i = 0; i < num; i++) {
                    users.add(in.readUTF());
                }
                if (!windows.containsKey(id))
                    windows.put(id, new ChatWindow(id, users));
                windows.get(id).newMsg(in.readUTF());
            }

            private void newGroup() throws IOException {
                int id = parseInt(in.readUTF());
                out.writeInt(online.getSelectedValuesList().size() + 1);
                out.writeUTF(username);
                List<String> users = new ArrayList<>();
                for (String s : online.getSelectedValuesList()) {
                    out.writeUTF(s);
                    users.add(s);
                }
                users.add(username);
                windows.put(id, new ChatWindow(id, users));
            }
        }
    }

    static class ChatWindow implements ActionListener {
        JTextArea msg, chats;
        List<String> users;
        JFrame f;
        int chatID;

        ChatWindow(int id, List<String> users) {
            if (users.size() < 1) {
                JOptionPane.showMessageDialog(null, "You must select at least 1 user!");
                return;
            }
            chatID = id;
            this.users = users;
            StringBuilder head = new StringBuilder("Conversation with ");
            for (String s : users) {
                if (!s.equalsIgnoreCase(OULWindow.username)) {
                    head.append(s);
                    head.append(", ");
                }
            }
            String header = head.substring(0, head.length() - 2);

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
            jp1.add(new JLabel(header));
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

            f = new JFrame(OULWindow.username);
            f.add(jp1);
            f.add(jp2);
            f.add(jp3);
            f.add(jp4);
            f.setLayout(new BoxLayout(f.getContentPane(), BoxLayout.Y_AXIS));
            f.pack();
            f.setVisible(true);
            f.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        }

        public void actionPerformed(ActionEvent ae) {
            try {
                OULWindow.out.writeUTF("msg");
                OULWindow.out.writeInt(chatID);
                OULWindow.out.writeUTF(OULWindow.username + ": " + msg.getText());
                msg.setText(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void newMsg(String msg) {
            chats.append(msg + '\n');
            f.pack();
            f.setVisible(true);
        }
    }
}