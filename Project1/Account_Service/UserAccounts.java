/**
 * Title: Server that manages accounts. 
 * Function: Stores and manages accounts.  Accounts are objects that store a username, a lifetime score int, 
 * and a boolean indicating if the user is currently signed in. It the server is bound to port 4000.
 * Author: Landon Odishaw-Dyck
 * Usage: java WordRepo
 * 
 * Assumptions/Limitations: The server is always on. There is no function to write to the a file such that it cannot go 
 * offline and come back online and maintain the same set of users. 
 *  
 * Future Improvements: Allow the server to write its user set to a file, such that the set is persistent across 
 * multiple teardowns and bootups. 
 *
 */

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.StringTokenizer;
import java.util.HashMap;

public class UserAccounts {
	protected DatagramSocket socket = null;
	protected BufferedReader in = null;
	static final int BIND_PORT = 4000;
	
	public UserAccounts() throws IOException {
		this(4000);
	}
	
	public UserAccounts(int port) throws IOException {
		socket = new DatagramSocket(port);
	}
	
	public void serve( HashMap<String,Account> accounts) {
		while(true) {
			try {			
				System.out.println("Listening for incoming requests...");
                byte[] inputbuf = new byte[256];
                byte[] outputbuf = new byte[256];
				
				// receive request
                DatagramPacket udpRequestPacket = new DatagramPacket(inputbuf, inputbuf.length);
                socket.receive(udpRequestPacket);

				// parse command.  Reading any arguments is done in the following switch block.
				String input = new String(inputbuf, StandardCharsets.UTF_8);  
				StringTokenizer st = new StringTokenizer(input);
				String command = st.nextToken().trim();
				String response;

				//handle different procedure calls
				switch( command.trim() ) {
					case "signIn":
						response = signInHandler(accounts, st);
					  break;
					case "signOut":
						response = signOutHandler(accounts, st);
					  	break;
					case "updateScore":

						response = updateScoreHandler( accounts, st);					
				
						break;
					case "getUserScore":
						response = getUserScoreHandler( accounts, st);	
					  	break;
					default:
					  response = "BAD REQUEST";
				}

				outputbuf = response.getBytes();
				  
        		// Send the response to the client.  Address and port are extracted from client request message. 
                InetAddress address = udpRequestPacket.getAddress();
                int port = udpRequestPacket.getPort();
                DatagramPacket udpReplyPacket = 
                		new DatagramPacket(outputbuf, outputbuf.length, address, port);
                socket.send(udpReplyPacket);



			} catch (SocketException e) {
				System.out.println(e.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}	
	}
	
    public static void main(String[] args) throws IOException {
    
		HashMap<String,Account> accounts = new HashMap<String,Account>(); 			// Read in word list

		// initate server
		int port = BIND_PORT;
		UserAccounts server = null;
		try {
	        server = new UserAccounts(port);
        } catch (NumberFormatException e) {
			System.err.println("Invalid port number: " + port + ".");
			System.exit(1);
		} catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                + port);
            System.out.println(e.getMessage());
        }
		
		server.serve(accounts);
		server.socket.close();
    }

	private String signInHandler ( HashMap<String,Account> accounts, StringTokenizer st ){
		
		String response = null;
		String userName = null;
		Account account;

		userName = st.nextToken().trim() ;
			
		account = accounts.get(userName);

		if( account != null )	//returning user
		{
			if( !account.getSignedIn() ){
				account.setSignedIn(true);
				response = "user_success";
			}
			else{ response = "fail"; }
		}
		else{
			Account newUser = new Account(userName, 0, true);  //new user account creation
			accounts.put(userName, newUser);
			response = "new_user_success";
		}

		return response;

	}

	private String signOutHandler ( HashMap<String,Account> accounts, StringTokenizer st ){

		String response = null;
		String userName = null;
		Account account;
		
		userName = st.nextToken().trim() ;
		account = accounts.get(userName);
	
		if( account != null )	//returning user
		{
			if( account.getSignedIn() ){
				account.setSignedIn(false);
				response = "success";
			}
			else{ response = "fail"; }
		}
		else{
			response = "DNE";
		}

		return response;
	}
	
	private String updateScoreHandler ( HashMap<String,Account> accounts, StringTokenizer st ){

		String response = null;
		String userName = null;
		Account account;

		userName = st.nextToken().trim() ;

		account = accounts.get(userName);

		if( account != null )	//returning user
		{
			int newScore = Integer.parseInt(st.nextToken().trim());

			account.setLifeScore(newScore);
			response = "success";

		} else response = "fail"; 

		return response;
	}

	private String getUserScoreHandler ( HashMap<String,Account> accounts, StringTokenizer st ){
	
		String response = null;
		String userName = null;
		Account account;

		userName = st.nextToken().trim() ;

		account = accounts.get(userName);

		if( account != null )	//returning user
		{
			int score = account.getLifeScore();
			response = Integer.toString(score);

		} else response = "fail";

		return response;
	}



}
