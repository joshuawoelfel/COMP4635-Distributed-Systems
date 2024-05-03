// COMP 4635 Assignment 3
// GameClient.java
// Title: Client for hangman game.
// Function: Has the logic for generating a user interface and recieving user input. It then uses RMI to service the users needs.
// Author: Jesse Viehweger
// 
// Assumptions/Limitations: It is assumed that the maximum fail count is 99. Its assumed that you cannot add or remove a word from the database while playing the game.

import java.util.Scanner;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

//Client for hangman game.
public class GameClient implements Runnable{

	private PhraseGuessingGameServer server;
	private static final int TIMELIMIT_SECONDS = 5;
	public String player = "";
	public boolean is_logged_in = false;

	public PhraseGuessingGameServer obtainServer() {
		return this.server;
	}

	

	public GameClient() {
		try {
			server = (PhraseGuessingGameServer) Naming.lookup("rmi://127.0.0.1/PhraseGuessingGameServer");
		} catch (Exception e) {
			System.out.println("The runtime failed: " + e.getMessage());
			System.exit(0);
		}
		System.out.println("Connected to the game server!");
	}

	public static void main(String[] args) {

		GameClient instance = new GameClient();
		instance.accessServer();
	}

	// Runs the main game logic.

	private void accessServer() {

		String message = "";
		Object[] result;
		Boolean loggedIn = false;
		Boolean inGame = false;
		Scanner userEntry = new Scanner(System.in);
		// Get login credentials from user
		System.out.println("Welcome To Our Guessing Game Please Login!");
		System.out.print("Username: ");
		String username = userEntry.nextLine();
		String player = "LOGIN_FAIL";
		int seq = 0;
		try {
			if (Math.random() < 0.5) {
				// code to be executed with 50% chance
				player = server.login(username, seq);
				player = server.login(username, seq);
			} else {
				player = server.login(username, seq);
			}
			seq = seq + 1;
			this.player = player;
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Handles invalid login credentials
		while (player.equals("LOGIN_FAIL")) {
			System.out.println("Not a valid username ");
			System.out.print("Username: ");
			username = userEntry.nextLine();
			try {
				if (Math.random() < 0.5) {
					// code to be executed with 50% chance
					player = server.login(username, seq);
					player = server.login(username, seq);
				} else {
					player = server.login(username, seq);
				}
				seq = seq + 1;
				this.player = player;
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		loggedIn = true;
		this.is_logged_in = true;
		System.out.println("Welcome " + username);
		printOptions(username);

		// main game loop
		do {
			if (!this.is_logged_in) {
				result = logIn(player, userEntry, seq);
				seq = (int) result[0];
				player = (String) result[1];
				this.player = player;
				loggedIn = true;
				this.is_logged_in = true;
			}
			do {
				new Thread(this).start();

				message = userEntry.nextLine();
				if (message.startsWith("guess")) {
					if (inGame) {
						result = makeGuess(userEntry, player, inGame, seq);
						seq = (int) result[0];
						inGame = (Boolean) result[1];
					} else {
						System.out.println("You must start a game before making a guess");
					}
				} else if (message.startsWith("restart")) {
					if (!inGame) {
						System.out.println("You cannot restart when not in a game");
					} else {
						seq = restartGame(player, seq);
					}
				} else if (message.startsWith("#")) {
					seq = endGame(player, seq);
					inGame = false;
				} else if (message.startsWith("?")) {
					seq = checkWord(player, userEntry, seq);
				} else if (message.startsWith("add")) {
					if (inGame) {
						System.out.println("You cannot add words during a game");
					} else {
						seq = addWord(player, userEntry, seq);
					}
				} else if (message.startsWith("remove")) {
					if (inGame) {
						System.out.println("You cannot remove words during a game");
					} else {
						seq = removeWord(player, userEntry, seq);
					}
				} else if (message.equals("close")) {
					seq = logout(player, seq);
					loggedIn = false;
				} else if (message.startsWith("start")) {
					if (inGame) {
						System.out.println("You cannot start a game while in a game");
					} else {
						seq = startGame(userEntry, player, seq);
						inGame = true;
					}

				} else if (message.startsWith("commands")) {
					printOptions(username);
				} else if (message.startsWith("logout")) {
					seq = logout(player, seq);
					loggedIn = false;
					break;
				} else {
					System.out.println("invalid command");
				}
			} while (!message.equals("close") && this.is_logged_in);
		} while (!message.equals("close"));
	}

	// Print the available commands to the user
	public static void printOptions(String username) {

		System.out.println("Commands:");
		System.out.println("start - start game");
		System.out.println("guess - guess a letter or the entire phrase");
		System.out.println("restart - restarts the current game");
		System.out.println("# - end current game session");
		System.out.println("? - check if word is in database");
		System.out.println("add - add a word to the database");
		System.out.println("remove - remove a word from the database");
		System.out.println("logout - logouts the current user");
		System.out.println("close - terminates the program");
		System.out.println("commands - prints this list again");
	}

	// This function checks if the given string can be converted into an integer and
	// returns true if it can and false if it cant.
	// https://stackoverflow.com/questions/237159/whats-the-best-way-to-check-if-a-string-represents-an-integer-in-java
	public static boolean isInteger(String str) {
		if (str == null) {
			return false;
		}
		int length = str.length();
		if (length == 0) {
			return false;
		}
		int i = 0;
		if (str.charAt(0) == '-') {
			if (length == 1) {
				return false;
			}
			i = 1;
		}
		for (; i < length; i++) {
			char c = str.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}
		return true;
	}

	// Gets input needed to create a game, contacts the server, receives the
	// response and outputs
	// the starting game state
	public int startGame(Scanner userEntry, String player, int seq) {
		Boolean valid = false;
		String words = null;
		String fail = null;
		int wordsInt = 0, failInt = 0;
		String response = "BAD_REQUEST";
		PhraseGuessingGameServer server = obtainServer();

		System.out.println("New Game!");
		System.out.println("How many words would you like in your guessing phrase?");
		while (!valid) {
			System.out.print("Words: ");
			words = userEntry.nextLine();
			if (isInteger(words)) {
				valid = true;
			} else {
				System.out.println("Please enter an integer value");
			}
		}
		valid = false;
		System.out.println("What would you like your fail factor to be to the nearest whole value (99 max)?");
		while (!valid) {
			System.out.print("Fail Factor: ");
			fail = userEntry.nextLine();
			if (isInteger(fail)) {
				valid = true;
			} else {
				System.out.println("Please enter an integer value");
			}
		}

		try {
			wordsInt = Integer.parseInt(words);
			failInt = Integer.parseInt(fail);

		} catch (NumberFormatException ex) {
			ex.printStackTrace();
		}

		try {
			if (Math.random() < 0.5) {
				// code to be executed with 50% chance
				response = server.startGame(player, wordsInt, failInt, seq);
				response = server.startGame(player, wordsInt, failInt, seq);
			} else {
				response = server.startGame(player, wordsInt, failInt, seq);
			}
			seq = seq + 1;
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (!response.contains("BAD_REQUEST")) {
			System.out.println(response);
		} else {
			System.out.println("Improperly formatted input");
		}
		return seq;
	}

	// Gets input needed to make a guess, contacts the server, receives the response
	// and outputs
	// the game state
	public Object[] makeGuess(Scanner userEntry, String player, Boolean inGame, int seq) {
		String guess;
		String response = "BAD_REQUEST";
		char guessChar;
		PhraseGuessingGameServer server = obtainServer();

		System.out.print("Enter your character/phrase guess:");
		guess = userEntry.nextLine();

		if (guess.length() == 1) {
			guessChar = guess.charAt(0);
			try {
				if (Math.random() < 0.5) {
					// code to be executed with 50% chance
					response = server.guessLetter(player, guessChar, seq);
					response = server.guessLetter(player, guessChar, seq);
				} else {
					response = server.guessLetter(player, guessChar, seq);
				}		
				seq = seq + 1;
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				if (Math.random() < 0.5) {
					// code to be executed with 50% chance
					response = server.guessPhrase(player, guess, seq);
					response = server.guessPhrase(player, guess, seq);
				  } else {
					response = server.guessPhrase(player, guess, seq);
				  }
				seq = seq + 1;
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (response.startsWith("Congratulations you won! ") || response.contains("Lost this round! Total Score:")) {
			inGame = false;
		}

		if (!response.contains("BAD_REQUEST")) {
			System.out.println(response);
		} else {
			System.out.println("Improperly formatted input");
		}
		Object[] result = {seq, inGame};
		return result;
	}

	// checks to see if a string containing numeric values is above 99. (ex "49"
	// returns true, "101" returns false)
	public static Boolean under99(String check) {
		Boolean result = true;
		try {
			Integer.parseInt(check);

		} catch (NumberFormatException ex) {
			return false;
		}
		return result;
	}

	// Contacts the server to end the game, receives the response and outputs
	// a message and the game score.
	public int endGame(String player, int seq) {
		String response = "BAD_REQUEST";
		PhraseGuessingGameServer server = obtainServer();
		try {
			if (Math.random() < 0.5) {
				// code to be executed with 50% chance
				response = server.endGame(player, seq);
				response = server.endGame(player, seq);
			  } else {
				response = server.endGame(player, seq);
			  }
			  seq = seq + 1;
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!response.equals("BAD_REQUEST")) {
			System.out.println(response);
		} else {
			System.out.println("You are not currently in a game");
		}
		return seq;
	}

	// Contacts the server to see if a word exists in the database, receives the
	// response and indicates if the word is in the database.
	public int checkWord(String player, Scanner userEntry, int seq) {
		String word;
		String response = "FAIL";
		PhraseGuessingGameServer server = obtainServer();

		System.out.println("What word would you like to check?");
		System.out.print("Word: ");
		word = userEntry.nextLine();

		try {
			if (Math.random() < 0.5) {
				// code to be executed with 50% chance
				response = server.checkWord(player, word, seq);
				response = server.checkWord(player, word, seq);
			  } else {
				response = server.checkWord(player, word, seq);
			  }
			seq = seq + 1;
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (!response.contains("FAIL")) {
			System.out.println("The word is contained in the database");
		} else {
			System.out.println("Improperly formatted input");
		}
		return seq;
	}

	// Formats response for seeing if a word is in the database and outputs it.
	// This function is not used.
	public static void formatExistsResponse(String response) {
		response = response.replace("exists ", "");
		response = response.replace("\n", "");
		if (response.startsWith("y")) {
			System.out.println("The word is found in the database");
		} else if (response.startsWith("n")) {
			System.out.println("The word is not found in the database");
		}
	}

	// Gets input needed to add a word to the database, contacts the server,
	// receives the response and indicates
	// if the word was succesfully added.
	public int addWord(String player, Scanner userEntry, int seq) {
		String word;
		String response = "FAIL";
		PhraseGuessingGameServer server = obtainServer();

		System.out.println("What word would you like to add?");
		System.out.print("Word: ");
		word = userEntry.nextLine();

		try {
			
			if (Math.random() < 0.5) {
				// code to be executed with 50% chance
				response = server.addWord(player, word, seq);
				response = server.addWord(player, word, seq);
			} else {
				response = server.addWord(player, word, seq);
			}
			seq = seq + 1;
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (!response.contains("BAD REQUEST")) {
			System.out.println("The word has been added to the database");
		} else {
			System.out.println("A failure occurred when adding the word");
		}
		return seq;
	}

	// Gets input needed to remove a word to the database, contacts the server,
	// receives the response and indicates
	// if the word was succesfully removed.
	public int removeWord(String player, Scanner userEntry, int seq) {
		String word;
		String response = "FAIL";
		PhraseGuessingGameServer server = obtainServer();

		System.out.println("What word would you like to remove?");
		System.out.print("Word: ");
		word = userEntry.nextLine();

		try {
			if (Math.random() < 0.5) {
				// code to be executed with 50% chance
				response = server.removeWord(player, word, seq);
				response = server.removeWord(player, word, seq);
			} else {
				response = server.removeWord(player, word, seq);
			}
			seq = seq + 1;
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (!response.contains("FAIL")) {
			System.out.println("The word has been removed from the database");
		} else {
			System.out.println("A failure occurred when removing the word");
		}
		return seq;
	}

	//Restarts a game using the same words and fail count, must be in a game to restart
	public int restartGame(String player, int seq) {
		String response = "BAD_REQUEST";
		PhraseGuessingGameServer server = obtainServer();

		try {
			if (Math.random() < 0.5) {
				response = server.restartGame(player, seq);
				response = server.restartGame(player, seq);
			} else {
				response = server.restartGame(player, seq);
			}	
			seq = seq + 1;
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (response.contains("BAD REQUEST")) {
			System.out.println("You must be in a game to restart");
		}
		return seq;
	}

	//Logs out player from the server
	public int logout(String player, int seq) {
		String response = "FAIL";
		PhraseGuessingGameServer server = obtainServer();

		try {
			if (Math.random() < 0.5) {
				// code to be executed with 50% chance
				response = server.logout(player, seq);
				response = server.logout(player, seq);
			} else {
				response = server.logout(player, seq);
			}
			 seq = seq + 1; 
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (!response.contains("FAIL")) {
			System.out.println("You have been logged out");
		} else {
			System.out.println("A failure occurred when logging you out");
		}
		return seq;
	}

	//Logs player into the server
	public Object[] logIn(String player, Scanner userEntry, int seq) {
		System.out.println("Welcome To Our Guessing Game Please Login!");
		System.out.print("Username: ");
		String username = userEntry.nextLine();
		player = "LOGIN_FAIL";
		try {
			if (Math.random() < 0.5) {
				player = server.login(username, seq);
				player = server.login(username, seq);
			} else {
				player = server.login(username, seq);
			}
			seq = seq + 1;
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Handles invalid login credentials
		while (player.equals("LOGIN_FAIL")) {
			System.out.println("Not a valid username ");
			System.out.print("Username: ");
			username = userEntry.nextLine();
			try {
				if (Math.random() < 0.5) {
					player = server.login(username, seq);
					player = server.login(username, seq);
				} else {
					player = server.login(username, seq);
				}
				seq = seq + 1;
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("Welcome " + username);
		printOptions(username);
		Object[] result = {seq, player};
		return result;
	}



	@Override
	public void run () {
		while(this.is_logged_in) {
			try {
				TimeUnit.SECONDS.sleep(TIMELIMIT_SECONDS);
				if (!server.heartbeat(this.player)) {
					this.is_logged_in = false;
				}

			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
	}



	// @Override
	// public void run() {
	// 	// TODO Auto-generated method stub
	// 	throw new UnsupportedOperationException("Unimplemented method 'run'");
	// }

}