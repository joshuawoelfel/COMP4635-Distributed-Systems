/*Author:Joshua Wolfel 201574083*/
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class PhraseGuessingGameServerImpl extends UnicastRemoteObject implements PhraseGuessingGameServer {
  private HashMap<String, ClientState> access_token_map =
    new HashMap<String, ClientState>();
  private ReentrantLock token_map_lock = new ReentrantLock();
  private ReentrantLock acct_seq_num_lock = new ReentrantLock();
  private ReentrantLock repo_seq_num_lock = new ReentrantLock();
  private UserAccounts user_acct_service;
  private WordRepositoryServer word_repo_service;
  private int repo_sequence_num = 0;
  private int users_sequence_num = 0;

  private static final String USAGE = "java PhraseGuessingGameServerImpl "
    + "[rmi path users] [rmi path word repo] or java PhraseGuessingGameServer -d"
    + " for default path";
  private static final String DEFAULT_RMI_PATH_ACCTS = "rmi://127.0.0.1/UserAccounts";
  private static final String DEFAULT_RMI_PATH_REPO = "rmi://127.0.0.1/WordRepositoryServer";
  private static final String HOST = "127.0.0.1";
  private static final String LOGIN_FAIL = "LOGIN_FAIL";
  private static final String LOGOUT_FAIL = "LOGOUT_FAIL";
  private static final String LOGOUT_SUCCESS = "LOGOUT_SUCCESS";
  private static final String BAD_REQUEST = "BAD_REQUEST";
  private static final boolean VALID_PARAMS = true;
  private static final boolean INVALID_PARAMS = false;
  private static final boolean DEBUG = false;
  private static final String WORD_REPO_SUCCESS = "SUCCESS";
  private static final String WORD_REPO_FAIL = "FAIL";
  private static final int TIMELIMIT_SECONDS = 10;

  PhraseGuessingGameServerImpl(
    UserAccounts user_acct_remote, WordRepositoryServer word_repo_remote
  ) throws RemoteException {
    this.user_acct_service = user_acct_remote;
    this.word_repo_service = word_repo_remote;
  }

  /**
 * Uses a ScheduledExecutorService as a dedicated thread to accurately check for inactive 
 * clients every TIMELIMIT_SECONDS. Inactive clients are logged out, and their scores 
 * are updated based on any ongoing games. 
 */
private void runClientWatchDog() {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(() -> {
      ClientState client;
      Game client_game;
           
      token_map_lock.lock();
      try {
        Iterator<Map.Entry<String, ClientState>> iterator = access_token_map
        .entrySet().iterator();

        if (iterator != null) {
          while (iterator.hasNext()) {
            Map.Entry<String, ClientState> entry = iterator.next();
            client = entry.getValue();
            client.lock();
            try {
              // set active client as inactive
              if (client.isActive()) {
                client.setUnactive();
              } else {
                // logout inactive client and end any ongoing game
                acct_seq_num_lock.lock();
                try{
                  if (this.user_acct_service.logout(
                    client.getUser().getUsername(), this.users_sequence_num
                  )){
                    this.users_sequence_num += 1;
                    client_game = client.getGame();
                    if (client_game != null) {
                      handleEndGame(client);
                    }
                    iterator.remove();
                  }
                } catch (RemoteException e) {
                  // todo
                  //
                } finally {
                  acct_seq_num_lock.unlock();
                }
              }
            } finally {
              client.unlock();
            }
          }
        }
      } finally {
        token_map_lock.unlock();
      }
    }, TIMELIMIT_SECONDS, TIMELIMIT_SECONDS, TimeUnit.SECONDS); 
  }

  public boolean heartbeat(String player) throws RemoteException {
    ClientState client;

    this.token_map_lock.lock();
    try {
      client = this.access_token_map.get(player);
      if (client == null) {
        return false;
      } else {
        client.lock();
      }
    } finally {
      this.token_map_lock.unlock();
    }
    try {
      client.setActive();
    } finally {
      client.unlock();
    }

    return true;
  }

  /* login
   * returns String that represents the access token of the user on successful login. 
   * This is "player" parameter in startGame, guessLetter guessPhrase, endGame, and restartGame.
   * Returns "LOGIN_FAIL" on failed login.
   * Note that we need to hold both the client's lock and the tocken map lock, this is in 
   * the case the access token leaked and used by another thread, to prevent desynchronization
   * of UserAccount service and the game server.
   */
  public String login(String username, int seq) throws RemoteException {
    String access_token;
    ClientState client;
    String user_key = "";

    token_map_lock.lock();
    try {
      if (requestUserLogin(username)) {
        client = setupClientState(username, seq);
        access_token = generateAccessToken(username);
        while(this.access_token_map.containsKey(access_token)) {
          access_token = generateAccessToken(username);
        }
        client.setSequenceNum(seq);
        this.access_token_map.put(access_token, client);
        client.increaseSequenceNum();
      } else {
        user_key = this.findClientKey(username);
        client = access_token_map.get(user_key);
        client.lock();
        try{ 
          if (user_key.equals("")) {
            return LOGIN_FAIL;
          } else if (this.logout(user_key, client.getSequenceNum()).equals(LOGOUT_SUCCESS)){
            return this.login(username, seq);
          } else {
            return LOGIN_FAIL;
          }
        } finally {
          client.unlock();
        }
      }
    } catch (RemoteException e) {
      // todo handle service fail
      throw e;
    } catch (NoSuchAlgorithmException e) {
      return LOGIN_FAIL;
    } finally {
      token_map_lock.unlock();
    }
      return access_token;
  }

  private String findClientKey(String username) {
    ClientState client;
    String user_key = "";
    token_map_lock.lock();
      try {
        Iterator<Map.Entry<String, ClientState>> iterator = access_token_map
        .entrySet().iterator();

        if (iterator != null) {
          while (iterator.hasNext()) {
            Map.Entry<String, ClientState> entry = iterator.next();
            client = entry.getValue();
            client.lock();
            try {
              if (client.getUser().getUsername().equals(username)) {
                return entry.getKey();
              }
            } finally {
              client.unlock();
            }
          }
        }
      } finally {
        token_map_lock.unlock();
      }
      return user_key;
  }

  /* logout
   * player param is player access token returned by login function. Returns "SUCCESS" on successful logout,
   * "FAIL" on failed logout, "BAD_REQUEST" if user does not exist on server. Note that we need to hold both
   * the client's lock and the tocken map lock, this is in the case the access token leaked and used by
   * another thread, to prevent desynchronization of UserAccount service and the game server.
   */
  public String logout(String player, int seq) throws RemoteException{
    ClientState client;
    Game client_game;

    this.token_map_lock.lock();
    try {
      client = this.access_token_map.get(player);
      if (client == null) {
        return LOGOUT_SUCCESS;
      } 
      if (seq < client.getSequenceNum()) {
        return LOGOUT_FAIL;
      } else {
        client.lock();
        try {
          this.acct_seq_num_lock.lock();
          try{
            if (this.user_acct_service.logout(
              client.getUser().getUsername(), this.users_sequence_num
            )){
              this.users_sequence_num += 1;
              client_game = client.getGame();
              if (client_game != null) {
                handleEndGame(client);
              }
              this.access_token_map.remove(player);
              return LOGOUT_SUCCESS;
            } else {
              this.users_sequence_num += 1;
              return LOGOUT_FAIL;
            }
          } finally {
            this.acct_seq_num_lock.unlock();
          }
        } finally {
          client.unlock();
        }
      }
    } catch (RemoteException e) {
      //todo 
      throw e;
    } finally {
      this.token_map_lock.unlock();
    }
  }

  /** guessLetter
   * returns String that represents result of the guess. This can be the
   * updated state of the game with the fail counter, or the game over
   * message with the appropriate message depending on if the game was
   * won/lost in addition to the user's score. Returns "BAD_REQUEST" if
   * user tries to guess without having a current game started.
   * Note that we need to hold client lock to prevent possible data corruption
   * if access token is leaked. 
   */
  public String guessLetter(String player, char letter, int seq) throws RemoteException {
    ClientState client;
    Game client_game;
    String guess_result = BAD_REQUEST;

    this.token_map_lock.lock();
    try {
      client = this.access_token_map.get(player);
      if (client == null) {
        return BAD_REQUEST;
      } else {
        client.lock();
      }
    } finally {
      this.token_map_lock.unlock();
    }
    try {
      if (seq < client.getSequenceNum()) {
        return client.getCachedResponse();
      }
      client_game = client.getGame();
      if (client_game != null) {
        guess_result = handleGuess(client_game.guessLetter(letter), client);
      }
      client.setCachedResponse(guess_result);
      client.increaseSequenceNum();
      return guess_result;
    } catch (RemoteException e) {
      //todo 
      throw e;
    } finally {
      client.unlock();
    }
  }

  /** restartGame
   * 
   */
  public String restartGame(String player, int seq) throws RemoteException {
    ClientState client;
    Game client_game;
    List<String> game_words;
    int game_difficulty;
    String response = BAD_REQUEST;

    this.token_map_lock.lock();
    try {
      client = this.access_token_map.get(player);
      if (client == null) {
        return BAD_REQUEST;
      } else {
        client.lock();
      }
    } finally {
      this.token_map_lock.unlock();
    }
    try {
      if (seq < client.getSequenceNum()) {
        return client.getCachedResponse();
      }
      client_game = client.getGame();
      if (client_game != null) {
        game_words = client_game.getGameParamWords();
        game_difficulty = client_game.getGameParamDifficulty();
        handleEndGame(client);
        client.updateGame(new Game(game_words, game_difficulty));
        
        response =  "Total Score: " 
          + client.getUser().getScore() 
          + "\n" 
          + client.getGame().getBoard();
      }
      client.setCachedResponse(response);
      client.increaseSequenceNum();
      return response;
    } catch (RemoteException e) {
      //todo 
      throw e;
    } finally {
      client.unlock();
    }
  }
  

  public String endGame(String player, int seq) throws RemoteException {
    ClientState client;
    Game client_game;
    String response = BAD_REQUEST;

    this.token_map_lock.lock();
    try {
      client = this.access_token_map.get(player);
      if (client == null) {
        return BAD_REQUEST;
      } else {
        client.lock();
      }
    } finally {
      this.token_map_lock.unlock();
    }
    try {
      if (seq < client.getSequenceNum()) {
        return client.getCachedResponse();
      }
      client_game = client.getGame();
      if (client_game != null) {
        handleEndGame(client);
        response =  "Total Score: " + client.getUser().getScore();
      }
      client.setCachedResponse(response);
      client.increaseSequenceNum();
      return response;
    } catch (RemoteException e) {
      //todo 
      throw e;
    } finally {
      client.unlock();
    }
  }

  private void handleEndGame(ClientState client) throws RemoteException {
    User client_user = client.getUser();

    client.updateGame(null);
    client_user.updateScore(-1);
    syncUser(client_user);
    client.updateGame(null);
  }

  public String guessPhrase(String player, String phrase, int seq) throws RemoteException {
    ClientState client;
    Game client_game;
    String response = BAD_REQUEST;

    this.token_map_lock.lock();
    try {
      client = this.access_token_map.get(player);
      if (client == null) {
        return BAD_REQUEST;
      } else {
        client.lock();
      }
    } finally {
      this.token_map_lock.unlock();
    }
    try {
      if (seq < client.getSequenceNum()) {
        return client.getCachedResponse();
      }
      client_game = client.getGame();
      if (client_game != null) {
        response = handleGuess(client_game.guessPhrase(phrase), client);
      }
      client.setCachedResponse(response);
      client.increaseSequenceNum();
      return response;
    } catch (RemoteException e) {
      //todo 
      throw e;
    } finally {
      client.unlock();
    }
  }

  private String handleGuess(boolean correct_guess, ClientState client) throws RemoteException{
    Game game = client.getGame();
    User client_user = client.getUser();
    boolean isGameOver = game.isGameOver();
    String message = "Lost this round!";
    int points = -1;

    if (isGameOver) {
      if (correct_guess) {
        points = 1;
        message = "Congratulations you won!";
      }
      client_user.updateScore(points);
      syncUser(client_user);
      client.updateGame(null);
      message += " Total Score: " + client_user.getScore();
      return message;
    } else {
      return game.getBoard();
    }
  }
  
  private void syncUser(User user) throws RemoteException{
    if (DEBUG) {
      return;
    } else {
      this.acct_seq_num_lock.lock();
      try {
        user_acct_service.updateUserScore(user.getUsername(), user.getScore(), this.users_sequence_num);
        this.users_sequence_num += 1;
      } finally {
        this.acct_seq_num_lock.unlock();
      }
    }
   
  }

  private ClientState setupClientState(String username, int seq) throws RemoteException {
    User user = new User(username, getUserScore(username));
    return new ClientState(user);
  }

  private int getUserScore(String username) throws RemoteException{
    int user_score = 0;
    if (DEBUG) {
      return 0;
    } else {
      this.acct_seq_num_lock.lock();
      try {
        user_score = Integer.parseInt(user_acct_service.getUserScore(username, this.users_sequence_num));
        this.users_sequence_num += 1;
        return user_score;
      } finally {
        this.acct_seq_num_lock.unlock();
      }
    }
  }

  public String startGame(
    String player, int number_of_words, int failed_attempt_factor, int seq
  ) throws RemoteException {
    ClientState client;
    Game client_game;
    List<String> game_words;
    String response = BAD_REQUEST;

    token_map_lock.lock();
    try{
      client = this.access_token_map.get(player);
      if (client == null) {
        return BAD_REQUEST;
      } else {
        client.lock();
      }
    } finally {
      token_map_lock.unlock();
    }
    try {
      if (seq < client.getSequenceNum()) {
        return client.getCachedResponse();
      } 
      if (validGameParams(number_of_words, failed_attempt_factor)) {
        client_game = client.getGame();
        if (client_game == null) {
          game_words = getGameWords(number_of_words);
          client_game = new Game(game_words, failed_attempt_factor);
          client.updateGame(client_game);
          response = client_game.getBoard();
        }
      }
      client.setCachedResponse(response);
      client.increaseSequenceNum();
      return response;
    } catch (RemoteException e) {
      // todo
      throw e;
    } finally {
      client.unlock();
    }
  }

  public String addWord(String player, String word, int seq) throws RemoteException {
    ClientState client;
    String response;
    if (DEBUG) {
      return "SUCCESS";
    }
    token_map_lock.lock();
    try{
      client = this.access_token_map.get(player);
      if (client == null) {
        return BAD_REQUEST;
      } else {
        client.lock();
      }
    } finally {
      token_map_lock.unlock();
    }
    try {
      if (seq < client.getSequenceNum()) {
        return client.getCachedResponse();
      }
      this.repo_seq_num_lock.lock();
      try {
        if (!this.word_repo_service.createWord(word, repo_sequence_num)) {
          repo_sequence_num += 1;
          response = WORD_REPO_FAIL;
        } else {
          repo_sequence_num +=1;
          response = WORD_REPO_SUCCESS;
        }
        client.setCachedResponse(response);
        client.increaseSequenceNum();
        return response;
      } catch (RemoteException e) {
        // todo
        throw e;
      } finally {
        this.repo_seq_num_lock.unlock();
      }
    } finally {
      client.unlock();
    }
  }

  public String removeWord(String player, String word, int seq) throws RemoteException {
    ClientState client;
    String response;
    if (DEBUG) {
      return "SUCCESS";
    }
    token_map_lock.lock();
    try{
      client = this.access_token_map.get(player);
      if (client == null) {
        return BAD_REQUEST;
      } else {
        client.lock();
      }
    } finally {
      token_map_lock.unlock();
    }
    try {
      if (seq < client.getSequenceNum()) {
        return client.getCachedResponse();
      }
      this.repo_seq_num_lock.lock();
      try {
        if (!this.word_repo_service.removeWord(word, this.repo_sequence_num)) {
          this.repo_sequence_num += 1;
          response = WORD_REPO_FAIL;
        } else {
          this.repo_sequence_num += 1;
          response = WORD_REPO_SUCCESS;
        }
        client.setCachedResponse(response);
        client.increaseSequenceNum();
        return response;
      } catch (RemoteException e) {
        // todo
        throw e;
      } finally {
        this.repo_seq_num_lock.unlock();
      }
    } finally {
      client.unlock();
    }
  }

  public String checkWord(String player, String word, int seq) throws RemoteException {
    ClientState client;
    String response;
    if (DEBUG) {
      return "SUCCESS";
    }
    token_map_lock.lock();
    try{
      client = this.access_token_map.get(player);
      if (client == null) {
        return BAD_REQUEST;
      } else {
        client.lock();
      }
    } finally {
      token_map_lock.unlock();
    }
    try {
      if (seq < client.getSequenceNum()) {
        return client.getCachedResponse();
      }
      this.repo_seq_num_lock.lock();
      try {
        if (!this.word_repo_service.checkWord(word, this.repo_sequence_num)) {
          this.repo_sequence_num += 1;
          response = WORD_REPO_FAIL;
        } else {
          this.repo_sequence_num += 1;
          response = WORD_REPO_SUCCESS;
        }
        client.setCachedResponse(response);
        client.increaseSequenceNum();
        return response;
      } catch (RemoteException e) {
        // todo
        throw e;
      } finally {
        this.repo_seq_num_lock.unlock();
      }
    } finally {
      client.unlock();
    }
  }

  private List<String> getGameWords(int num_words) throws RemoteException{
    List<String> game_words = new ArrayList<String>(num_words);
    if (DEBUG) {
      String[] debug_words = new String[] {
        "Distributed",
        "Systems",
        "is",
        "fun"
      };
      for (int i = 0; i < num_words; i += 1) {
        game_words.add(debug_words[i % debug_words.length]);
      } 
    } else {
      for (int i = 0; i < num_words; i += 1) {
        this.repo_seq_num_lock.lock();
        try {
          game_words.add(word_repo_service.getRandomWord(0, this.repo_sequence_num));
          this.repo_sequence_num += 1;
        } finally {
          this.repo_seq_num_lock.unlock();
        }
      }
    }

    return game_words;
  }

  private boolean validGameParams(int num_words, int fail_factor) {
    if (num_words < 1 || fail_factor < 1) {
      return INVALID_PARAMS;
    }
    return VALID_PARAMS;
  }

  private String generateAccessToken(String username) throws NoSuchAlgorithmException{
    String secret = Long.toString(System.nanoTime()) + username;

    // the following is taken from https://www.baeldung.com/sha-256-hashing-java
    MessageDigest digest;
    try {
        digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedhash = digest.digest(
          secret.getBytes(StandardCharsets.UTF_8)
        );
        return bytesToHex(encodedhash);
    } catch (NoSuchAlgorithmException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        throw e;
    }
  }

  //taken from https://www.baeldung.com/sha-256-hashing-java
  private static String bytesToHex(byte[] hash) {
    StringBuilder hexString = new StringBuilder(2 * hash.length);
    for (int i = 0; i < hash.length; i++) {
        String hex = Integer.toHexString(0xff & hash[i]);
        if(hex.length() == 1) {
            hexString.append('0');
        }
        hexString.append(hex);
    }
    return hexString.toString();
  }

  private boolean requestUserLogin(String username) throws RemoteException {
    boolean result;
    if (DEBUG) {
      return true;
    } else {
      this.acct_seq_num_lock.lock();
      try {
        result = this.user_acct_service.login(username, this.users_sequence_num);
        this.users_sequence_num += 1;
        return result;
      } finally {
        this.acct_seq_num_lock.unlock();
      }
    }
    // TODO: request login from UserAccount Service
  }


  public static void main(String[] args) throws RemoteException, MalformedURLException {
    String accounts_rmi_path = DEFAULT_RMI_PATH_ACCTS;
    String repo_rmi_path = DEFAULT_RMI_PATH_REPO;
    UserAccounts user_service = null;
    WordRepositoryServer word_repo_service = null;

    /*if (args.length != 2) {
      if (args.length != 1) {
        System.err.println(USAGE);
        System.exit(1);
      }
    } else {
      accounts_rmi_path = args[0];
      repo_rmi_path = args[1];
    }
*/
    startRegistry();

    try{
      if (DEBUG) {
        user_service = null;
        word_repo_service = null;
      } else {
        user_service = (UserAccounts)Naming.lookup(accounts_rmi_path);
        word_repo_service = (WordRepositoryServer)Naming.lookup(repo_rmi_path);
      }
    } catch (Exception e) {
      System.out.println(e);
    }

    PhraseGuessingGameServerImpl server_instance = 
      new PhraseGuessingGameServerImpl(user_service, word_repo_service);

    String rmi_obj_name = "rmi://" + HOST + "/PhraseGuessingGameServer";
	Naming.rebind(rmi_obj_name, server_instance);
	
    server_instance.runClientWatchDog();
    System.out.println("Binding complete...\n");		
  }

  public static void startRegistry() throws RemoteException{
    try {
      LocateRegistry.getRegistry(1099).list();
      
    } catch (RemoteException e) {
      LocateRegistry.createRegistry(1099);
    }
  }
}