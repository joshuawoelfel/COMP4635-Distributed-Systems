// Landon Odishaw-Dyck
// Implements the WordRepositoryServer interface. Multithreaded is supported

import java.rmi.*;
import java.rmi.server.*;

import java.util.ArrayList;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class WordRepositoryServerImpl extends UnicastRemoteObject implements WordRepositoryServer {
	
    ArrayList<String> words;
	HashMap<Integer,String> requestHistory;
	int nextExpectedSequenceNumber;
	private Object mutex = new Object();

    public WordRepositoryServerImpl() throws RemoteException { 

        //Initialize Repo
        words = readFile(); 			// Read in word list
		requestHistory = new HashMap<Integer,String>();

	}

    public boolean createWord(String word, int seq) throws RemoteException{

		boolean success;

		
		synchronized (mutex) {
			
			if( seq < nextExpectedSequenceNumber ){
				success = Boolean.parseBoolean(requestHistory.get(seq));		//return recorded request
			}
			else{
		        success = !words.contains( word.trim() ) && words.add( word.trim() );
				requestHistory.put(seq, Boolean.toString(success));
				nextExpectedSequenceNumber = seq+1;
			}
		}
        return success;
    }

    public boolean removeWord(String word, int seq) throws RemoteException{
        
		boolean success;

		synchronized (mutex) {
			if( seq < nextExpectedSequenceNumber ){
				success = Boolean.parseBoolean(requestHistory.get(seq));		//return recorded request
			}
			else{
        		success = words.remove( word.trim() );
				requestHistory.put(seq, Boolean.toString(success));
				nextExpectedSequenceNumber = seq+1;
			}
		}

        return success;
    }

    public boolean checkWord(String word, int seq) throws RemoteException{
        
		boolean success;
        
		synchronized (mutex) {
			if( seq < nextExpectedSequenceNumber ){
				success = Boolean.parseBoolean(requestHistory.get(seq));		//return recorded request
			}
			else{
				success = words.contains( word.trim() );
				requestHistory.put(seq, Boolean.toString(success));
				nextExpectedSequenceNumber = seq+1;
			}
		}

        return success;
    }

    public String getRandomWord(int length, int seq) throws RemoteException{
        
		String rand_word;

		synchronized (mutex) {
			if( seq < nextExpectedSequenceNumber ){
				rand_word = requestHistory.get(seq);		//return recorded request
			}
			else{
	        	rand_word = getRandomWord( words);  //returns null if empty repo list
				requestHistory.put(seq, rand_word);
				nextExpectedSequenceNumber = seq+1;
			}				
		}
        return rand_word;
    }

    public String getRandomWord( ArrayList<String> words) {

		int index;
		String word;

		synchronized (mutex) {
		index = (int)(Math.random() * words.size());
		word = words.get(index);
		}

		return word;
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


