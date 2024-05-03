/**
 * Title: Word Repository Server
 * Function: Stores words, and allow the removal, retrieval, and addition of new and existing words.
 * The server is boudn to port 4001.
 * Author: Landon Odishaw-Dyck
 * Usage: java WordRepo
 * 
 * Assumptions/Limitations: The server is always on. There is no function to write to the a file such that it cannot go 
 * offline and come back online and maintain the same list of words.  At startup, the same list of words is 
 * read in. 
 * Specifically for the game application: if one user changes the word repo, it affects all user.
 *  
 * Future Improvements: Allow the server to write its word list to a file, such that the word list is, along
 * with newly added words, persistent across multiple teardowns and bootups. 
 * Specifically: Allow seperate repos for each user.
 * 
 */

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.StringTokenizer;
import java.util.ArrayList;

public class WordRepo {
	protected DatagramSocket socket = null;
	protected BufferedReader in = null;
	static final int BIND_PORT = 4001;

	public WordRepo() throws IOException {
		this(4001);
	}
	
	public WordRepo(int port) throws IOException {
		socket = new DatagramSocket(port);
	}
	
	public void serve( ArrayList<String> words) {
		while(true) {
			try {			
				System.out.println("Listening for incoming requests...");
                byte[] inputbuf = new byte[256];
                byte[] outputbuf = new byte[256];
				String word;
				Boolean success;
				String response = null;

                // receive request
                DatagramPacket udpRequestPacket = new DatagramPacket(inputbuf, inputbuf.length);
                socket.receive(udpRequestPacket);

				// Parse command from packet.  Reading any arguments is done in the following switch block.
				String input = new String(inputbuf, StandardCharsets.UTF_8);  
				StringTokenizer st = new StringTokenizer(input);
				String command = st.nextToken();

				//handle different procedure calls
				switch( command.trim() ) {
					case "addWord":
						word = st.nextToken();
						success = !words.contains(word.trim() ) && words.add( word.trim() );
						response = "Added " + success;
					  break;
					case "removeWord":
						word = st.nextToken();
						success = words.remove( word.trim() );
						response = "Removed " + success;
					  break;
					case "checkWord":
						
						word = st.nextToken().intern();
						success = words.contains( word.trim() );
						response = "checked " + success;
					break;
					case "getWord":
						String rand_word = getRandomWord( words );
						if( rand_word != null ){
							response = rand_word;	
						}
						else{
							response = "repo_fail";
						}
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
    
		ArrayList<String> words = readFile(); 			// Read in word list

		// initate server
		int port = BIND_PORT;
		WordRepo server = null;
		try {
	        //port = Integer.parseInt(args[0]);
			server = new WordRepo(port);
        } catch (NumberFormatException e) {
			System.err.println("Invalid port number: " + port + ".");
			System.exit(1);
		} catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                + port);
            System.out.println(e.getMessage());
        }
		
		
		//multi thread here?
		server.serve(words);
		server.socket.close();
    }

	public String getRandomWord( ArrayList<String> words ) {

		int index = (int)(Math.random() * words.size());

		return words.get(index);
	}

	public static ArrayList<String> readFile() {

		try {
			ArrayList<String> words = new ArrayList<String>();
			String word = null;
			
			// use fileReader to utilize its byte-to-char functionality.  Use a buffered reader to minimize OS calls, increase efficienc
			FileReader fileReader = new FileReader("words.txt");
			BufferedReader reader = new BufferedReader(fileReader);

			
			word = reader.readLine();
			while (word != null) {
								
				words.add(word);
				word = reader.readLine();
			}

			reader.close();

			return words;


		} catch (FileNotFoundException e) {
			System.out.println("Cannot Open File");
			e.printStackTrace();
		} catch ( IOException i ){
			System.out.println("File Read Error");
			i.printStackTrace();
		}
		

		return null;

	}
	

}
