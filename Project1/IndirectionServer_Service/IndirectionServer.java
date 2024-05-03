/**
 * Author Joshua Wolfel
 */
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Scanner;

import java.io.*;

public class IndirectionServer {
  private final static int MAX_THREAD_COUNT = 128;

  public static void main(String[] args) throws IOException {
    int port = 0;
    ServerSocket server = null;

    ExecutorService thread_pool = Executors.newFixedThreadPool(MAX_THREAD_COUNT);

    try {
      port = 5599;
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

  private static class ClientHandler implements Runnable {
    private static final String SIGN_IN_REQUEST = "login";
    private static final String SIGN_IN_USER = "signIn";
    private static final String SIGN_OUT = "signOut";
    private static final String BAD_REQUEST = "BAD REQUEST";
    private static final String UDP_HOST = "127.0.0.1";
    private static final int UDP_PORT_DB = 4000;
    private static final int UDP_PORT_REPO = 4001;
    private static final String START_GAME_REQUEST = "start";
    private static final String START_NEW_GAME_REQ = "!";
    private static final String GET_SCORE_REQUEST = "$";
    private static final String GET_WORD_REQUEST = "getWord";
    private static final String ADD_WORD_REQUEST = "addWord";
    private static final String REMOVE_WORD_REQUEST = "removeWord";
    private static final String CLIENT_CHECK_REQUEST = "?";
    private static final String CHECK_WORD_REQUEST = "checkWord";
    private static final String EXIT_GAME_REQUEST = "@";
    private static final String END_GAME_REQUEST = "#";
    private static final String USER_SCORE_REQUEST = "getUserScore";
    private static final String REQUEST_SCORE_UPDATE = "updateScore";
    private static final String EOS_SIGNAL = "EOS";
    private static final String GAME_HOST = "localhost";
    private static final int GAME_PORT = 5000;

    private Socket client_socket;
    private User curr_user = null;
    private boolean game_in_prog = false;
    private TCPClientConnection game_connection = null;

    ClientHandler(Socket socket) {
      this.client_socket = socket;
    }

    @Override
    public void run() {

      String request = "";
      String response;
      System.out.println("Connected, handling new client: " + client_socket);
      try {
        Scanner in = new Scanner(
          new InputStreamReader(client_socket.getInputStream())
        );
        while (in.hasNextLine()) {
          request = in.nextLine();
          response = handleRequest(request);
          if (response.equals(EOS_SIGNAL)) {
            break;
          }
          sendResponse(response);
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

    private void sendResponse(String response) throws IOException {
      PrintStream out = new PrintStream(this.client_socket.getOutputStream());

      out.println(response);
    }

    private String handleRequest(String request) {
      Command req = new Command(request);
      String request_type = req.getCommandName();

      if (request_type.equals(EXIT_GAME_REQUEST)) {
        return handleExitGame();
      } else if (this.curr_user == null) {
        if (request_type.equals(SIGN_IN_REQUEST)) {
          return handleSignInRequest(req);
        } else {
          return BAD_REQUEST;
        }
      } else if (request_type.equals(END_GAME_REQUEST)) {
        return endGameRequest();
      } else if (request_type.equals(GET_SCORE_REQUEST)) {
        return handleScoreRequest(req);
      } else if (request_type.equals(START_NEW_GAME_REQ)) {
        return handleStartGame(req);
      } else if (request_type.equals(CLIENT_CHECK_REQUEST)) {
        return handleEditRepo(req, CHECK_WORD_REQUEST);
      } else if (this.game_in_prog == true) {
        return handleClientGuess(request);
      } else if (request_type.equals(START_GAME_REQUEST)) {
        return handleStartGame(req);
      } else if (request_type.equals(ADD_WORD_REQUEST)) {
        return handleEditRepo(req, ADD_WORD_REQUEST);
      } else if (request_type.equals(REMOVE_WORD_REQUEST)) {
        return handleEditRepo(req, REMOVE_WORD_REQUEST);
      }
      return BAD_REQUEST;
    }

    private String handleExitGame() {
      if (this.curr_user != null) {
        signOutRequest();
        endGameRequest();
        this.curr_user = null;
      }
      return EOS_SIGNAL;
    }

    private String endGameRequest() {
      if (this.game_connection == null) {
        return "SUCCESS";
      } else {
        try {
          this.game_connection.sendRequest(END_GAME_REQUEST);
          this.game_connection.closeConnection();
          this.game_connection = null;
          this.game_in_prog = false;
          return "SUCCESS";
        } catch (IOException e) {
          this.game_connection = null;
          this.game_in_prog = false;
          return "FAIL";
        }
      }
    }

    private String signOutRequest() {
      String username = this.curr_user.getUsername();
      return makeServiceRequest(
          SIGN_OUT + " " + username, UDP_PORT_DB
      ).trim();
    }

    private String handleScoreRequest(Command c) {
      String username = this.curr_user.getUsername();
      String response = makeServiceRequest(
          USER_SCORE_REQUEST + " " + username, UDP_PORT_DB
      );

      if (!response.equals("fail")) {
        return "Total Score: " + response;
      } else {
        return "Unable to retrieve score";
      }
    }

    private String handleEditRepo(Command request, String forward_request) {
      String[] args = request.getArgs();

      if (args.length != 1) {
        return BAD_REQUEST;
      } else {
        return makeServiceRequest(
            forward_request + " " + args[0], UDP_PORT_REPO
        );
      }
    }

    private String handleStartGame(Command request) {
      String[] args = request.getArgs();
      int num_words;
      int difficulty;
      String game_words;
      String game_response;

      try {
        if (args.length != 2) {
          return BAD_REQUEST;
        } else {
          num_words = Integer.parseInt(args[0]);
          difficulty = Integer.parseInt(args[1]);
          game_words = getGameWords(num_words);
          if (game_words.equals("REPO_FAIL")) {
            // todo: handle repo fail
            return "REPO_FAIL";
          } else {
            game_response = sendStartGameRequest(game_words, difficulty);
            if (
              !game_response.equals(BAD_REQUEST) 
              && !game_response.equals("CONN_ERR_GAME")
            ) {
              this.game_in_prog = true;
            } 
            return game_response;
          } 
        }
      } catch (NumberFormatException e) {
        return BAD_REQUEST;
      } catch (IOException e) {
        System.out.println(e.getMessage());
        return BAD_REQUEST;
      }
    }

    private String sendStartGameRequest(String game_words, int difficulty) {
      try {
        if (this.game_connection == null) {
          this.game_connection = new TCPClientConnection(GAME_HOST, GAME_PORT);
        } 
        return this.game_connection.sendRequest(START_NEW_GAME_REQ +
          " " + Integer.toString(difficulty) +
          " " + game_words
        );
      } catch(IOException e) {
        return "CONN_ERR_GAME";
      }
    }

    private String getGameWords(int num_words) throws IOException {
      String response;
      String phrase = "";

      for (int i = 0; i < num_words; i += 1) {
        response = makeServiceRequest(GET_WORD_REQUEST, UDP_PORT_REPO);
        if (response.equals("REPO_FAIL")) {
          throw new IOException("REPO_FAIL");
        } else {
          phrase += response + " ";
        }
      }
      return phrase;
    }

    private String handleClientGuess(String guess) {
      int point_val = 0;
      String sync_response;
      String response;
      try {
        response = this.game_connection.sendRequest(guess);
    
        if (response.equals("Congratulations you won!")) {
          point_val = 1;
        } else if (response.equals("Lost this round!")) {
          point_val = -1;
        }
        if (point_val != 0) {
          this.curr_user.updateScore(point_val);
          this.game_in_prog = false;

          sync_response = syncUser();
          if (
            sync_response.equals("fail") || sync_response.equals("SERVICE_ERR")
          ) {
            return sync_response;
          }
          return response + " Total Score: " + this.curr_user.getScore();
        } else {
          return response;
        } 
      } catch (IOException e) {
        return e.getMessage();
      }
    }

    private String syncUser() {
      // TODO: update History ?
      String username = this.curr_user.getUsername();
      String score = Integer.toString(this.curr_user.getScore());

      return makeServiceRequest(
          REQUEST_SCORE_UPDATE + " " + username + " " + score, UDP_PORT_DB);
    }

    private String handleSignInRequest(Command request) {
      String response = makeServiceRequest(
          SIGN_IN_USER + " " + request.argsAsString(), UDP_PORT_DB).trim();
      String[] request_args = request.getArgs();
      String username = request_args.length > 0 ? request_args[0] : "";
      String user_score;

      if (response.equals("fail")) {
        return "Unable to log in user " + username;
      } else {
        user_score = makeServiceRequest(
            USER_SCORE_REQUEST + " " + username, UDP_PORT_DB);
        if (user_score.equals("fail")) {
          return "Unable to log in user " + username;
        } else {
          updateCurrentUser(username, Integer.parseInt(user_score));
          if (response.equals("new_user_success")) {
            return "Welcome " + username;
          } else {
            return "Welcome back " + username;
          }
        }
      }
    }

    private void updateCurrentUser(String username, int score) {
      this.curr_user = new User(username, score);
    }

    private String makeServiceRequest(String request, int port) {
      UDPClientRequest udp_request = new UDPClientRequest(UDP_HOST, port);
      String udp_response = "";

      try {
        udp_response = udp_request.sendRequest(request);
      } catch (IOException e) {
        return "SERVICE_ERR";
        // TODO: handle crashed server
      }

      return udp_response.trim();
    }

  }

  
  private static class TCPClientConnection {
    private Socket link;
    private Scanner server_input;
    private PrintStream client_output;
    
    TCPClientConnection(String host, int port) throws IOException {
      this.link = new Socket (host, port);
      this.server_input = new Scanner(new InputStreamReader(link.getInputStream()));
      this.client_output = new PrintStream(link.getOutputStream());
    }

    
    public String sendRequest(String request) throws IOException {
      String reply;
        
      client_output.println(request);
      reply = server_input.nextLine();
      return reply;
    }

    public void closeConnection() throws IOException {
        this.link.close();
    }

  }

  private static class UDPClientRequest {
    private static final int BUFFER_LIMIT = 1000;

    private String host;
    private int port;

    UDPClientRequest(String host, int port) {
      this.host = host;
      this.port = port;
    }

    public String sendRequest(String request) throws IOException {
      System.out.println("\nSending the request: "
          + request + " to the server!");

      // get a datagram socket
      DatagramSocket socket = new DatagramSocket();

      // send request
      byte[] requestBuf = new byte[BUFFER_LIMIT];
      requestBuf = request.getBytes();

      InetAddress address = InetAddress.getByName(this.host);
      DatagramPacket packet = new DatagramPacket(
        requestBuf, requestBuf.length, address, this.port
      );
      socket.send(packet);

      return requestResponse(socket);
    }

    private String requestResponse(DatagramSocket socket) throws IOException {
      byte[] responseBuf = new byte[BUFFER_LIMIT];
      DatagramPacket packet = new DatagramPacket(responseBuf, BUFFER_LIMIT);

      socket.receive(packet);

      return new String(packet.getData());
    }

  }

}