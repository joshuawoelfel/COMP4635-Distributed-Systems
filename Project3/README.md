# COMP 4635
## Assignment #3: Java RMI Client-Server Phrase Guessing Game with Advanced Features

#### Part of the design had to adhere to instructor specifications.
**Please view `COMP4635_W23_A2.pdf and COMP4635_W23_A3.pdf` and  for full technical requirements.**

#### This was a group project, I am the sole author of the following: 
- **PhraseGuessingGameServerImpl.java:** Manages game interactions and state for multiple clients, ensuring synchronized access and consistent gameplay across different sessions 
    1. Game Session Management: Handles multiple game sessions by initiating games, processing guesses, and managing game endings, maintaining accurate tracking of player progress and scores.
    2. Client Management: Manages client states using a token-based system, ensuring each client's actions are synchronized and consistent across sessions.
    3. Failure Handling: Actively monitors client activity and cleans up client records that do not respond, ensuring the system's integrity and resource management.
    4. Idempotency and Deduplication:
      - Sequence Numbers: Utilizes sequence numbers in method invocations to ensure that each operation is processed only once, preventing inconsistencies and duplications in game state and scoring.
      - State Management: Employs client-specific sequence tracking and caching mechanisms to respond to duplicate requests with cached results, avoiding unnecessary processing.
      - Locking Mechanisms: Uses ReentrantLock to synchronize access to shared resources, ensuring that concurrent operations do not interfere with each other.
      
- **ClientState.java:** Provides methods for managing individual client sessions. Used by PhraseGuessingGameServerImpl.
    1. State Management: The ClientState class encapsulates all necessary details about a client’s current session, including user information (User object), current game state (Game object), and session activity status.
    2. Cached Responses: The cached_response field stores the last response sent to the client. This feature supports deduplication by allowing the server to resend the same response for duplicate requests, rather than 
                         recalculating or reprocessing the request. 
    3. Activity Tracking: The class tracks whether a client is active or not, which is used to determine if the client is still connected or has timed out due to inactivity. 
    
- **Game.java:** Contains the game logic of the application. Utilized by PhraseGuessingGameServerImpl.
    1. Game State Management: It maintains the state of the game, including the current phrase to be guessed, the representation of the phrase as it is revealed through player guesses, and the count of failed attempts 
                              allowed before the game is lost.
    2. Game Progress Feedback: It provides feedback to the player and the server about the current status of the game, including the updated visual representation of the phrase and the number of remaining attempts.
    3. End-of-Game Determination: The class determines when the game ends, either through successful completion of the phrase or exhaustion of the allowed failed attempts, and it resets or updates the game environment accordingly for subsequent rounds.
    
- **User.java:** Used by ClientState to encapsulate and manage the data and actions related to an individual user within the application.

---

### Overview
This project is a Java RMI-based client-server distributed application for the phrase guessing game in the first project. The game utilizes advanced features such as failure detection, recovery mechanisms, and request deduplication.

### Functional Requirements
- **Game Interaction**: Clients start games by specifying the game level, influencing the difficulty and structure of the phrase to be guessed.
- **Phrase Guessing**: The client attempts to guess words by suggesting letters or the entire phrase, with the server updating the game state based on correct or incorrect guesses.
- **Scoring System**: The server tracks each client's score, incrementing for correct completions and decrementing for failures. Scores are tracked across sessions for returning clients.
- **Failure Handling**: Implement mechanisms to detect client failures and handle them gracefully, ensuring the integrity of the game state and client records.

### Implementation Details
- **Java RMI**: Utilizes Java RMI for all client-server interactions. The system does not use explicit socket programming, relying instead on RMI's capabilities for remote object management and method invocation.

### Key Components
#### Server
- **Remote Interface Implementation**: Implements the `PhraseGuessingGameServer` interface with methods for starting games, making guesses, ending games, and managing heartbeats.
- **Failure Detection**: Periodically checks for clients that fail to send heartbeats and cleans up their records.
- **Request Deduplication**: Incorporates idempotency keys in method calls to prevent duplicate processing of requests, enhancing reliability and consistency.

#### Word Repository Microservice
- **Functionality**: Allows clients to add, remove, or check words in a repository, implemented as a separate RMI service.
- **Remote Interface**: Defines methods like `createWord`, `removeWord`, and `checkWord` to interact with the word repository.

#### User Accounts Microservice
- **User Management**: Manages user accounts and game history, ensuring data consistency and enabling session persistence across games.
- **Design Choice**: The service can be accessed either directly by clients or only via the game server, based on system architecture preferences.

### Server Interfaces
```java
import java.rmi.*;

public interface PhraseGuessingGameServer extends Remote {
  public String login(String username, int seq) throws RemoteException;
  public String logout(String player, int seq) throws RemoteException;
  public String startGame(
    String player,
    int number_of_words,
    int failed_attempt_factor,
    int seq
  ) throws RemoteException;
  public String guessLetter(String player, char letter, int seq) throws RemoteException;
  public String guessPhrase(String player, String phrase, int seq) throws RemoteException;
  public String endGame(String player, int seq) throws RemoteException;
  public String restartGame(String player, int seq) throws RemoteException;
  public String addWord(String player, String word, int seq) throws RemoteException;
  public String removeWord(String player, String word, int seq) throws RemoteException;
  public String checkWord(String player, String word, int seq) throws RemoteException;
  public boolean heartbeat(String player) throws RemoteException;
}

public interface WordRepositoryServer extends Remote {
    boolean createWord(String word) throws RemoteException;
    boolean removeWord(String word) throws RemoteException;
    boolean checkWord(String word) throws RemoteException;
}


public interface UserAccounts extends Remote {
  public String getUserScore(String username, int seq) throws RemoteException;
  public boolean login(String username, int seq) throws RemoteException;
  public boolean updateUserScore(String username, int score, int seq) throws RemoteException;
  public boolean logout(String username, int seq) throws RemoteException;
}
```

### Client
- **Gameplay Interface**: Implements a simple command-line interface for initiating games, making guesses, and managing words.
- **Heartbeat Mechanism**: Spawns a separate thread to send heartbeats to the server, ensuring ongoing session validity.

### Future Enhancements
- **Data Persistence**: Enhance the server to save game states and user data to disk, allowing recovery from restarts and maintaining consistency across sessions.
- **Scoreboard Service**: Implement a microservice to calculate and display a scoreboard, ranking players based on their performance.

