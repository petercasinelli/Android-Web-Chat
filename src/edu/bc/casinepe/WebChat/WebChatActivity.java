package edu.bc.casinepe.WebChat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.example.android_web_chat.R;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class WebChatActivity extends Activity {
	
	private static final String SOCKET_HOST = "cslab.bc.edu";
	private static final int SOCKET_PORT = 10000;
	private Socket s = null;
	private BufferedReader in = null;
	private BufferedWriter out = null;
	private TextView messageBox = null;
	private EditText groupNameField = null;
	private EditText messageField = null;
	private EditText passwordField = null;
	
	//Encryption settings
	private static final String ENCRYPTION_ALGORITHM = "AES";
	private static final int    KEY_LENGTH = 128;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_web_chat);
		
		messageBox = (TextView)findViewById(R.id.chatTextField);
		groupNameField = (EditText)findViewById(R.id.groupTextField);
		messageField = (EditText)findViewById(R.id.messageTextField);
		passwordField = (EditText)findViewById(R.id.passwordTextField);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.web_chat, menu);
		return true;
	}
	
	// make key be KEY_LENGTH bits
	private String fixKey(String key) {
		if (key.length() > KEY_LENGTH/8)
			key = key.substring(0, KEY_LENGTH/8);
		while (key.length() < KEY_LENGTH/8)
			key += " ";
		return key;
	}
	
	/*
	 * encrypt helper method to encrypt strings with given configuration
	 */
	public String encrypt(String str) {
		byte[] result = encryptDecryptHelper(str.getBytes(), Cipher.ENCRYPT_MODE);
		String finalStr = Base64.encodeToString(result, Base64.DEFAULT);
		return finalStr;
	}
	
	/*
	 * decrypt helper method to encrypt strings with given configuration
	 */
	public String decrypt(String str) {
		byte[] cipherText = Base64.decode(str, Base64.DEFAULT);
		byte[] result = encryptDecryptHelper(cipherText, Cipher.DECRYPT_MODE);
		return new String(result);
	}
	
	/*
	 * Helper method for encryption/decryption. Can be slow, should be in non-UI thread
	 */
	public byte[] encryptDecryptHelper(byte[] oldBytes, int mode) {
		String key = passwordField.getText().toString();
		key = fixKey(key);
		Cipher cipher;
		try {
			cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
			SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), ENCRYPTION_ALGORITHM);
			cipher.init(mode, keySpec);
			byte[] newBytes = cipher.doFinal(oldBytes);
			return newBytes; // normal case, no problems
		} catch (InvalidKeyException e) {
			return new byte[] {0};
		} catch (NoSuchAlgorithmException e) {
			// Handled below
		} catch (NoSuchPaddingException e) {
			// Handled below
		} catch (IllegalBlockSizeException e) {
			// Handled below
		} catch (BadPaddingException e) {
			// Handled below
		}

		// If we didn't return in the try block, something went wrong
		Log.d(this.getClass().toString(), "Encrypt/Decrypt helper did not return correct message.");
		return new byte[] {0};
	}
	
	/*
	 * Connect button is pressed; in a new thread, establish socket connection, send group name, 
	 * and start listening to messages.
	 */
	public void connectToServer(View view) {
		Log.d(this.getClass().toString(), "Connecting to server...");
		
		messageField.setText("");
		
	    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        
        if (networkInfo != null && networkInfo.isConnected()) { // this line and the two above are to find out if we're connected to the net
        	final String groupName = groupNameField.getText().toString();
    		Log.d(this.getClass().toString(), "About to start thread to get socket/streams, send message to server with " + groupName);

        	// Start new thread to get encrypted messages from the server
        	new Thread(new Runnable() { // network activity MUST occur on a separate thread
		        public void run() {
		        	//Get socket and streams
		        	getSocket();
		    		//First, send the group name to the server (only one time, no new thread)
    	    		sendMessageToServer(groupName + "\n", false);
		        	//start displaying messages
		        	getEncryptedMessages();
		        }
		    }).start();
        	
        } else {
        	messageBox.setText(getString(R.string.notConnected));
        }
	}
	
	/*
	 * Get a socket and it's reader/writer
	 */
	private void getSocket() {
		try {
    		Log.d(this.getClass().toString(), "Connecting to socket and getting streams");
			s = new Socket(SOCKET_HOST, SOCKET_PORT);
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));
    		out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/*
	 * Listen for encrypted messages from the server.
	 */
	private void getEncryptedMessages() {
		
		Log.d(this.getClass().toString(), "Setting up to receive messages...");
		try {
			String line;
			while (true) {
				while ((line = this.in.readLine()) != null) {
					Log.d(this.getClass().toString(), "Received a message: " + line);					
					appendMessage(decrypt(line));				
				}	
			}
		} catch (IOException e) {
			Log.d(this.getClass().toString(), "IOException: " + e);
			appendMessage("Unknown message received.");
		} catch (Exception e) {
			appendMessage("Unknown message received.");
		} finally {
			Log.d(this.getClass().toString(), "Finally statement for message listener");
			if (s != null)
				try {
					s.close();
				} catch (IOException e) {
					// we tried
				}
		}
	}
	
	/*
	 * Send button is pressed in UI
	 */
	public void sendMessage(View view) {
		final String message = messageField.getText().toString();
		Log.d(this.getClass().toString(), "Send message button pressed. Message is: " + message);
		new Thread(new Runnable() {
	        public void run() {
	        	sendMessageToServer(message, true);
	        }
	    }).start();	
	}
	
	/*
	 * Send a message to a server with established socket s and output stream os
	 */
	private void sendMessageToServer(String message, boolean sendEncrypted) {
		Log.d(this.getClass().toString(), "Preparing to send a message: " + message);
		try {
			Log.d(this.getClass().toString(), "Sending message: " + message);	
			if (sendEncrypted) {
				message = encrypt(message);
				Log.d(this.getClass().toString(), "Encrypting message as: " + message);	
			}			
			this.out.write(message);
			//Immediately send
			this.out.flush();
			
		} catch (Exception e) {
			Log.d(this.getClass().toString(), "Exception: " + e);	
		}
		
	}

	/*
	 * Appends a message to messageBox in a new UI thread 
	 */
	private void appendMessage(final String s) {
		Log.d(this.getClass().toString(), "Appending string to messageBox: " + s);

		messageBox.post(new Runnable() {
			public void run() {
				messageBox.append(s + "\n");
				ScrollView scrollView = (ScrollView)messageBox.getParent();
				scrollView.fullScroll(View.FOCUS_DOWN);
			}
			
		});	
	}

}
