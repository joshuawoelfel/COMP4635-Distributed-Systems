/**
 * Author: Joshua Wolfel
 */
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import java.io.*;

/** 
 * GameServer
 * Game server that handles game requests from a client.
 * Possible requests are: 
 *  # -> to end the current game
 *  ! -> to start a game, requires params <difficulty> as int
 *       followed by one or more words to guess
 *  
 * 
 */
public class GameServer {
  
  private final static int MAX_THREAD_COUNT = 128;

  /** main
   * Starts the game server on port 5000. When new client connects 
   * spawns a new thread to handle client. Max clients that can be 
   * served at a time is 128. 
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {
    int port = 0;
    ServerSocket server = null;

    ExecutorService thread_pool = Executors.newFixedThreadPool(MAX_THREAD_COUNT);

    try {
      port = 5000;
      server = new ServerSocket(port);
      System.out.println("The game server is running...");
      while (true) {
        thread_pool.execute(new ClientHandler(server.accept()));
      }
    } catch (IOException e) {
      System.out.println(
          "Exception caught when trying to listen on port "
              + port
              + " or listening for a connection");
      System.out.println(e.getMessage());

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /** ClientHandler
   * Handles requests for GameServer. Expects tcp connection
   * socket of client. 
   */
  private static class ClientHandler implements Runnable {
    private final static String BAD_REQUEST = "BAD_REQUEST";
    private final static String EOS_SIGNAL = "EOS";
    private final static String END_GAME_REQUEST = "#";
    private static final String START_NEW_GAME_REQ = "!";
    private Socket client_socket;
    private Game curr_game = null;

    ClientHandler(Socket socket) {
      this.client_socket = socket;
    }

    /** run
     * Reads input from client socket connection, and calls sendResponse to
     * return the response from handling the request back to the client
     */
    @Override
    public void run() {
      String request = "";
      String response;
      System.out.println("Connected, handling new client: " + client_socket);
      try {
        Scanner in = new Scanner(new InputStreamReader(client_socket.getInputStream()));
        while (in.hasNextLine()) {
          request = in.nextLine();
          response = handleRequest(request);
          sendResponse(response);
          if (response.equals(EOS_SIGNAL)) {
            break;
          }
          System.out.println(
            "Received the following message from" + client_socket + ":" + request
          );
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        try {
          this.client_socket.close();
        } catch (IOException e) {
        }
        System.out.println("Closed: " + client_socket);
      }
    }

    /** sendResponse
     * sends server response to client request over socket connection
     */
    private void sendResponse(String response) throws IOException {
      PrintStream out = new PrintStream(this.client_socket.getOutputStream());

      out.println(response);
    }

    /** handleRequest
     * Identifies request type using Command object, and forwards request
     * @param request the client request
     * @return response to client request
     */
    private String handleRequest(String request) {
      Command req = new Command(request);
      String request_type = req.getCommandName();

      if (request_type.equals(END_GAME_REQUEST)) {
        return EOS_SIGNAL;
      } else if (request_type.equals(START_NEW_GAME_REQ)) {
          return handleStartGame(req);
      } else if (this.curr_game != null) {
        return handleClientGuess(request);
      } else {

      }
      return BAD_REQUEST;
    }

    /**
     * starts the game if arguments are correct, difficulty is an
     * int with value above zero and there exists at least one word
     * to guess 
     * @param req
     * @return
     */
    private String handleStartGame(Command req) {
      String[] args = req.getArgs();
      List<String> word_list = new ArrayList<String>();
      int difficulty;

      try {
        if (args.length < 2 || Integer.parseInt(args[0]) < 1) {
          return BAD_REQUEST;
        } else {
          difficulty = Integer.parseInt(args[0]);
          for (int i = 1; i < args.length; i+=1) {
            word_list.add(args[i]);
          }
          this.curr_game = new Game(word_list, difficulty);
          return curr_game.getBoard();
        }
      } catch (NumberFormatException e) {
        return BAD_REQUEST;
      }
 
    }

    /**
     * makes a guess, checks to see if game is in gameover state
     * afterwards, if it is, returns appropriate game over message
     * otherwise return game board
     * @param guess
     * @return
     */
    private String handleClientGuess(String guess) {
      boolean correct_guess = this.curr_game.makeGuess(guess);
      boolean isGameOver = this.curr_game.isGameOver();
      String message = "Lost this round!";

      if (isGameOver) {
        if (correct_guess) {
          message = "Congratulations you won!";
        }
        this.curr_game = null;
        return message;
      } else {
        return this.curr_game.getBoard();
      }

    }

  }
}
