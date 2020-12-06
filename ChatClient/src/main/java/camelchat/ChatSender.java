package camelchat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* The functionality layer of ChatClient application. */

/* The Facade of the layer. */
public class ChatSender {
	private String userName;
	private String room;
	private Map<String, Object> msgHeaders;

	private CommEngine engine;
	private ChatFrame frame;

	public ChatSender(ChatFrame f) {
		userName = null;
		room = null;
		msgHeaders = new HashMap<String, Object>();
		frame = f;
		engine = CommEngine.getInstance(this);
	}
	
	public void stopEngine() {
		engine.stop();
	}
	
	/* Get the chat record of current chat room. */
	void pollChatRecord() {
		assert (room != null);
		// Send join request to jms:queue:JoinRequestQueue, and get response. 
		List<List<String>> records = (List<List<String>>) engine.requestRecords(room); 
		if (records == null) 
			return;
		for (List<String> rec: records) {
			frame.displayMessage(rec.get(0), rec.get(1) ,rec.get(2).replace(';', '\n'));
		}
	}
	
	/* Called when a message is received by message listener of CommEngine. */
	public void receiverMsg(String name, String time, String text) {
		frame.displayMessage(name, time, text);
	}

	/* Join a chat room. Called when "Join" button is clicked. */
	public void joinRoom(String name, String r) {
		userName = name;
		room = r;
		frame.clearWindow();
		pollChatRecord();
		
		// update headers map
		msgHeaders.put("UserName", userName);
		msgHeaders.put("Room", room);
		
		// set the receiver window to listen to jms topic
		engine.setListener(room);
	}
	
	/* Send the text in editor window to chat server. */
	public boolean sendMessage(String msg) {
		if (userName == null || room == null) {
			// Need to specify the user name and join a chat room first. 
			return false;
		}
		
		engine.sendMessage(msg, msgHeaders);
		return true;
	}
}
