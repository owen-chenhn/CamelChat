package camelchat;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.Font;
import javax.swing.JButton;
import javax.swing.border.BevelBorder;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import javax.swing.JTextPane;
import java.awt.TextArea;

/* The UI layer of ChatClient application. */

/* Event handler of joinButton. */
class JoinButtonListener implements ActionListener {
	private ChatFrame frame;
	public JoinButtonListener(ChatFrame f) {
		frame = f;
	}

	public void actionPerformed(ActionEvent e) {
		frame.joinRoom();
	}
}

/* Event handler of sendButton. */
class SendButtonListener implements ActionListener {
	private ChatFrame frame;
	public SendButtonListener(ChatFrame f) {
		frame = f;
	}

	public void actionPerformed(ActionEvent e) {
		frame.sendMessage();
	}
}


/* Main window frame of the chat GUI. */
public class ChatFrame extends JFrame {
	private ChatSender sender;
	
	private JPanel contentPane;
	private JTextField userNameField;
	private JTextField roomField;
	private TextArea receiverWindow;
	private JTextArea inputEditor;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ChatFrame frame = new ChatFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public void joinRoom() { 
		String name = userNameField.getText();
		String room = roomField.getText();
		if (name.isEmpty()) {
			JOptionPane.showMessageDialog(null, "UserName field cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (room.isEmpty()) {
			JOptionPane.showMessageDialog(null, "Room field cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		sender.joinRoom(name, room); 
	}

	public void sendMessage() {
		String message = inputEditor.getText();
		if (message.isEmpty()) 
			return;
		
		if (sender.sendMessage(message)) {
			inputEditor.setText(null);
		}
		else {
			// prompt user to input user name and room name
			JOptionPane.showMessageDialog(null, "Please enter user name and room name first!", 
				"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void clearWindow() { receiverWindow.setText(null); }
	public void displayMessage(String name, String time, String text) {
		receiverWindow.append(name + "  (" + time + "):\n" + text + "\n\n");
	}

	/**
	 * Create the frame.
	 */
	public ChatFrame() {
		setTitle("CamelChat");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent evt) {
				sender.stopEngine();
			}
		});
		
		setBounds(100, 100, 970, 598);
		contentPane = new JPanel();
		contentPane.setBackground(new Color(0, 153, 204));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JPanel topPanel = new JPanel();
		topPanel.setBounds(168, 10, 776, 427);
		contentPane.add(topPanel);
		topPanel.setLayout(null);
		
		receiverWindow = new TextArea();
		receiverWindow.setBounds(0, 32, 776, 385);
		receiverWindow.setFont(new Font("Arial", Font.PLAIN, 14));
		receiverWindow.setEditable(false);
		topPanel.add(receiverWindow);
		
		JLabel windowLabel = new JLabel("Messages");
		windowLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
		windowLabel.setBounds(3, 0, 773, 34);
		topPanel.add(windowLabel);
		
		JPanel leftPanel = new JPanel();
		leftPanel.setBackground(Color.LIGHT_GRAY);
		leftPanel.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
		leftPanel.setBounds(10, 10, 148, 539);
		contentPane.add(leftPanel);
		leftPanel.setLayout(null);
		
		userNameField = new JTextField();
		userNameField.setBounds(10, 257, 128, 27);
		leftPanel.add(userNameField);
		userNameField.setColumns(10);
		
		JLabel lblNewLabel = new JLabel("User Name:");
		lblNewLabel.setFont(new Font("Arial", Font.BOLD, 14));
		lblNewLabel.setBounds(34, 233, 80, 27);
		leftPanel.add(lblNewLabel);
		
		roomField = new JTextField();
		roomField.setColumns(10);
		roomField.setBounds(10, 318, 128, 27);
		leftPanel.add(roomField);
		
		JLabel lblRoom = new JLabel("Room:");
		lblRoom.setFont(new Font("Arial", Font.BOLD, 14));
		lblRoom.setBounds(52, 294, 44, 27);
		leftPanel.add(lblRoom);
		
		JButton joinButton = new JButton("Join");
		joinButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
		joinButton.setBounds(34, 368, 80, 27);
		leftPanel.add(joinButton);
		
		JTextPane txtpnPleaseEnterYour = new JTextPane();
		txtpnPleaseEnterYour.setEditable(false);
		txtpnPleaseEnterYour.setBackground(Color.LIGHT_GRAY);
		txtpnPleaseEnterYour.setFont(new Font("Arial", Font.PLAIN, 14));
		txtpnPleaseEnterYour.setText("Please enter your user name and the room number to join.");
		txtpnPleaseEnterYour.setBounds(10, 144, 128, 74);
		leftPanel.add(txtpnPleaseEnterYour);
		
		JTextPane txtpnWelcomeToChat = new JTextPane();
		txtpnWelcomeToChat.setText("Welcome to chat room! ");
		txtpnWelcomeToChat.setFont(new Font("Arial", Font.PLAIN, 14));
		txtpnWelcomeToChat.setEditable(false);
		txtpnWelcomeToChat.setBackground(Color.LIGHT_GRAY);
		txtpnWelcomeToChat.setBounds(10, 94, 128, 40);
		leftPanel.add(txtpnWelcomeToChat);
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setBounds(168, 437, 776, 112);
		contentPane.add(bottomPanel);
		bottomPanel.setLayout(null);
		
		JButton sendButton = new JButton("Send");
		sendButton.setBounds(699, 10, 67, 92);
		sendButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
		bottomPanel.add(sendButton);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(0, 10, 689, 102);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		bottomPanel.add(scrollPane);
		
		inputEditor = new JTextArea();
		scrollPane.setViewportView(inputEditor);
		inputEditor.setFont(new Font("Arial", Font.PLAIN, 14));
		inputEditor.setTabSize(4);
		
		// Add button click handler.
		joinButton.addActionListener(new JoinButtonListener(this));
		sendButton.addActionListener(new SendButtonListener(this));
		
		sender = new ChatSender(this);
	}
}
