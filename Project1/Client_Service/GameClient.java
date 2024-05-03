// COMP 4635 Assignment 1
// GameClient.java
// Title: Client for hangman game.
// Function: Has the logic for generating a user interface and recieving user input. Passes input to server for handling.
// Author: Jesse Viehweger
// 
// Assumptions/Limitations: It is assumed that before running this code that the server and microservices are already running. It is assumed that
// the maximum fail count is 99. Its assumed that you cannot add or remove a word from the database while playing the game.

import java.net.*;
import java.util.Scanner;
import java.io.IOException;
import java.io.PrintWriter;


//Client for hangman game.
public class GameClient {
	private static InetAddress host;
	private static final int PORT = 5599;
	
	public static void main(String[] args) {
		GameClient instance = new GameClient();
		try {
			host = InetAddress.getLocalHost();
		} catch (UnknownHostException uhEx) {
		}
		instance.accessServer();
	}

	// Connects to server and contains main game logic.

	private void accessServer() {
		Socket link = null; 
		String message, response;
		
		Boolean inGame = false;
		Scanner userEntry = new Scanner(System.in);
		//Get login credentials from user
		System.out.println("Welcome To Our Guessing Game Please Login!");
			System.out.print("Username: ");
			String username = userEntry.nextLine();
			username = "login " + username;

		try {
			link = new Socket(host, PORT); 

			Scanner input = new Scanner(link.getInputStream()); 
			PrintWriter output = new PrintWriter(link.getOutputStream(), true);
			output.println(username);
			response = input.nextLine();
			
			//Handles invalid login credentials 
			while (response.equals("unknown") || response.equals("BAD REQUEST") || response.contains("Unable to log in") ) {
				if (response.contains("Unable to log in")){
					System.out.println(response);
				} else {
					System.out.println("Not a valid username ");
				}
				System.out.print("Username: ");
				username = userEntry.nextLine();
				username = "login " + username;
				output.println(username);
				response = input.nextLine();
			}
			System.out.println(response);
			printOptions(username);

			//main game loop
			do {
				message = userEntry.nextLine();
				if (message.startsWith("guess")) {
					if(inGame){
						inGame = makeGuess(userEntry, input, output, inGame);
					} else {
						System.out.println("You must start a game before making a guess");
					}			
				} else if (message.startsWith("$")) {
					getScore(input, output);
				} else if (message.startsWith("!")) {
					newGame(userEntry, input, output);
					inGame = true;
				} else if (message.startsWith("#")) {
					endGame(input, output);
					printOptions(username);
					inGame = false;
				} else if (message.startsWith("?")) {
					checkWord(userEntry, input, output);
				} else if (message.startsWith("add")) {
					if (inGame){
						System.out.println("You cannot add words during a game");
					} else {
						addWord(userEntry, input, output);
					}	
				} else if (message.startsWith("remove")) {
					if (inGame){
						System.out.println("You cannot remove words during a game");
					} else {
						removeWord(userEntry, input, output);
					}	
				} else if (message.startsWith("close")) {
					String end = "@";
					output.println(end);
				} else if (message.startsWith("start")) {
					startGame(userEntry, input, output);
					inGame = true;
				} else if (message.startsWith("commands")){
					printOptions(username);
				} else {
					System.out.println("invalid command");
				}
			} while (!message.equals("close"));

		} catch (IOException ioEx) {
			System.out.println("Unable to connect to the server");
		} finally {
			try {
				link.close(); 
			} catch (IOException ioEx) {
				System.out.println("Unable to close connection to the server");
			}
		}
	}

	//Print the available commands to the user
	public static void printOptions(String username) {
	
		System.out.println("Commands:");
		System.out.println("start - start game");
		System.out.println("guess - guess a letter or the entire phrase");
		System.out.println("$ - check score");
		System.out.println("! - new game");
		System.out.println("# - end current game session");
		System.out.println("? - check if word is in database");
		System.out.println("add - add a word to the database");
		System.out.println("remove - remove a word from the database");
		System.out.println("close - terminates the program");
		System.out.println("commands - prints this list again");
	}
	// This function checks if the given string can be converted into an integer and returns true if it can and false if it cant.
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

	//Formats message being sent to server to start a game
	public static String formatGame(String words, String fail) {
		String result = "start " + words + " " + fail;
		// System.out.println(result);
		return result;
	}

	//Formats message being sent to server to start a new game
	public static String formatNewGame(String words, String fail) {
		String result = "! " + words + " " + fail;
		return result;
	}

	//Formats message being sent to server to make a guess
	public static String formatGuess(String guess) {
		String result = "guess " + guess;
		return result;
	}

	//Formats message being sent to server to see if a word exists in the database
	public static String formatExists(String word) {
		String result = "? " + word;
		return result;
	}

	//Formats message being sent to server to add a word to the database
	public static String formatAdd(String word) {
		String result = "addWord " + word;
		return result;
	}

	//Formats message being sent to server to remove a word to the database
	public static String formatRemove(String word) {
		String result = "removeWord " + word;
		return result;
	}

	//Gets input needed to create a game, contacts the server, receives the response and outputs
	//the starting game state
	public static void startGame(Scanner userEntry, Scanner input, PrintWriter output) {
		Boolean valid = false;
		String words = null;
		String fail = null;
		String message;
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

		message = formatGame(words, fail);
		output.println(message);
		String response = input.nextLine();
		if (!response.contains("BAD REQUEST")) {
			System.out.println(response);
		} else {
			System.out.println("Improperly formatted input");
		}
	}

	//Gets input needed to make a guess, contacts the server, receives the response and outputs
	//the game state
	public static Boolean makeGuess(Scanner userEntry, Scanner input, PrintWriter output, Boolean inGame) {
		String guess;
		System.out.print("Enter your character/phrase guess:");
		guess = userEntry.nextLine();
		output.println(guess);
		String response = input.nextLine();

		if (response.startsWith("Congratulation")){
			inGame = false;
		} else if (response.startsWith("Lost")){
			inGame = false;
		}
		if (!response.contains("BAD REQUEST")) {
			System.out.println(response);
		} else {
			System.out.println("Improperly formatted input");
		}
		return inGame;
	}

	// checks to see if a string containing numeric values is above 99. (ex "49" returns true, "101" returns false)
	public static Boolean under99(String check) {
		Boolean result = true;
		try {
			Integer.parseInt(check);

		} catch (NumberFormatException ex) {
			return false;
		}
		return result;
	}

	// Formats response for creating a game and outputs it.
	// This function is not used. 
	public static void formatGameResponse(String str) {

		String failCounter = str.length() > 3 ? str.substring(str.length() - 3) : str;
		failCounter = failCounter.substring(0, failCounter.length() - 1);

		str = str.substring(0, str.length() - 1);
		str = str.substring(0, str.length() - 1);
		str = str.substring(0, str.length() - 1);
		if (str.endsWith("C")) {
			str = str.substring(0, str.length() - 1);
		}

		str = str.replace("phrase ", "");

		System.out.println(str);
		// System.out.println("Remaining Fail Count: " + failCounter);
	}

	// Formats response for completing a game and outputs it.
	// This function is not used. 
	public static void gameComplete(String response) {
		String[] split = response.split(":");
		split[0] = split[0].replace("complete ", "");
		split[1] = split[1].replace("\n", "");
		System.out.println(split[0]);
		System.out.println("Score: " + split[1]);
	}

	// Formats response for losing a game and outputs it.
	// This function is not used. 
	public static void gameLost(String response) {
		String[] split = response.split(":");
		split[0] = split[0].replace("lose ", "");
		split[1] = split[1].replace("\n", "");
		System.out.println(split[0]);
		System.out.println("Score: " + split[1]);
	}

	// Formats response for making a guess and outputs it.
	// This function is not used. 
	public static void guessMade(String str) {
		String failCounter = str.length() > 3 ? str.substring(str.length() - 3) : str;
		failCounter = failCounter.substring(0, failCounter.length() - 1);

		str = str.substring(0, str.length() - 1);
		str = str.substring(0, str.length() - 1);
		str = str.substring(0, str.length() - 1);
		if (str.endsWith("C")) {
			str = str.substring(0, str.length() - 1);
		}

		str = str.replace("guess ", "");

		System.out.println(str);
	}

	//Contacts the server to get the score, receives the response and outputs
	//the game state 
	public static void getScore(Scanner input, PrintWriter output) {

		String scoreRequest = "$";

		output.println(scoreRequest);
		String response = input.nextLine();

		if (!response.contains("BAD REQUEST")) {
			System.out.println(response);
			;

		} else {
			System.out.println("Improperly formatted input");
		}
	}

	// Formats response for getting the score and outputs it.
	// This function is not used. 
	public static void formatScoreResponse(String response) {

		response = response.replace("score ", "");
		response = response.replace("\n", "");
		System.out.println("Score: " + response);
	}

	//Gets input needed to create a new game, contacts the server, receives the response and outputs
	//the starting game state
	public static void newGame(Scanner userEntry, Scanner input, PrintWriter output) {
		Boolean valid = false;
		String words = null;
		String fail = null;
		String message;
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
			if (isInteger(fail) && under99(fail)) {
				valid = true;
			} else {
				System.out.println("Please enter an integer value");
			}
		}
		message = formatNewGame(words, fail);
		output.println(message);

		String response = input.nextLine();

		if (!response.contains("BAD REQUEST")) {
			System.out.println(response);
		} else {
			System.out.println("Improperly formatted input");
		}
	}

	//Contacts the server to end the game, receives the response and outputs
	//a message and the game score.
	public static void endGame(Scanner input, PrintWriter output) {
		String endRequest = "#";
		output.println(endRequest);
		String response = input.nextLine();
		// System.out.println(response);
		System.out.println("Game over");


	}

	//Contacts the server to see if a word exists in the database, receives the response and indicates if the word is in the database.
	public static void checkWord(Scanner userEntry, Scanner input, PrintWriter output) {
		String word;
		System.out.println("What word would you like to check?");
		System.out.print("Word: ");
		word = userEntry.nextLine();
		word = formatExists(word);
		output.println(word);
		String response = input.nextLine();
		if (!response.contains("BAD REQUEST")) {
			if(response.contains("true")){
				System.out.println("The word is contained in the database");
			} else if (response.contains("false")){
				System.out.println("The word is not contained in the database");
			}

		} else {
			System.out.println("Improperly formatted input");
		}
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

	//Gets input needed to add a word to the database, contacts the server, receives the response and indicates
	//if the word was succesfully added.
	public static void addWord(Scanner userEntry, Scanner input, PrintWriter output) {
		String word;
		System.out.println("What word would you like to add?");
		System.out.print("Word: ");
		word = userEntry.nextLine();
		word = formatAdd(word);
		output.println(word);
		String response = input.nextLine();
		if (!response.contains("BAD REQUEST")) {
			if(response.contains("true")){
				System.out.println("The word was successfully added");
			} else if (response.contains("false")){
				System.out.println("The word failed to be added from the database");
			}
		} else {
			System.out.println("Improperly formatted input");
		}
	}

	//Gets input needed to remove a word to the database, contacts the server, receives the response and indicates
	//if the word was succesfully removed.
	public static void removeWord(Scanner userEntry, Scanner input, PrintWriter output) {
		String word;
		System.out.println("What word would you like to remove?");
		System.out.print("Word: ");
		word = userEntry.nextLine();
		word = formatRemove(word);
		output.println(word);
		String response = input.nextLine();
		if (!response.contains("BAD REQUEST")) {
			if(response.contains("true")){
				System.out.println("The word was successfully removed");
			} else if (response.contains("false")){
				System.out.println("The word failed to be removed from the database");
			}
		} else {
			System.out.println("Improperly formatted input");
		}
	}

}