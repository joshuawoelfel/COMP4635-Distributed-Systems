// Landon Odishaw-Dyck
// Implements the UserAccounts interface.  Multithreaded is supported

import java.rmi.*;
import java.rmi.server.*;
import java.util.HashMap;

public class UserAccountsImpl extends UnicastRemoteObject implements UserAccounts {
 
    HashMap<String,Account> accounts;
	HashMap<Integer,String> requestHistory;
	int nextExpectedSequenceNumber;

	private Object mutex = new Object();

 
    public UserAccountsImpl() throws RemoteException { 

        accounts = new HashMap<String,Account>();
		requestHistory = new HashMap<Integer,String>();
		
	}

    public String getUserScore(String username, int seq) throws RemoteException{

		Account account;
        String response;

		synchronized (mutex) {

			if( seq < nextExpectedSequenceNumber ){
				response = requestHistory.get(seq);		//return recorded request
			}
			else{

				account = accounts.get(username);
			
				if( account != null )	//returning user
				{
					int score = account.getLifeScore();
					response = Integer.toString(score);

				} else response = "fail";

				requestHistory.put(seq, response);
				nextExpectedSequenceNumber = seq+1;
			}
		}

		return response;

    }

	
    public boolean login(String username, int seq) throws RemoteException{
		
        Account account;
        boolean response;
        
		synchronized( mutex ){
			
			if( seq < nextExpectedSequenceNumber ){
				response = Boolean.parseBoolean(requestHistory.get(seq));		//return recorded request
			}
			else{

				account = accounts.get(username);

				if( account != null )	//returning user
				{
					if( !account.getSignedIn() ){

						account.setSignedIn(true);
						response = true;
					}
					else{ response = false; }
				}
				else{
					Account newUser = new Account(username, 0, true);  //new user account creation
					accounts.put(username, newUser);
					response = true;
				}
				
				requestHistory.put(seq, Boolean.toString(response));
				nextExpectedSequenceNumber = seq+1;
			}			
		}

		return response;
    }

    public boolean updateUserScore(String username, int score, int seq) throws RemoteException{

		Account account;
        boolean response;


		synchronized (mutex) {

			if( seq < nextExpectedSequenceNumber ){
				response = Boolean.parseBoolean(requestHistory.get(seq));		//return recorded request
			}
			else{

				account = accounts.get(username);

				if( account != null )	//returning user
				{
					account.setLifeScore(score);
					response = true;

				} else response = false; 

				requestHistory.put(seq, Boolean.toString(response));
				nextExpectedSequenceNumber = seq+1;
			}
		}

		return response;

    }

    public boolean logout(String username, int seq) throws RemoteException{

		Account account;
        boolean response;
	
		synchronized (mutex){

			if( seq < nextExpectedSequenceNumber ){
				response = Boolean.parseBoolean(requestHistory.get(seq));		//return recorded request
			}
			else{

				account = accounts.get(username);

				if( account != null )	//returning user
				{
					if( account.getSignedIn() ){
						account.setSignedIn(false);
						response = true;
					}
					else{ response = false; }
				}
				else{
					response = false;
				}

				requestHistory.put(seq, Boolean.toString(response));
				nextExpectedSequenceNumber = seq+1;
			}
		}

		return response;

    }

}
