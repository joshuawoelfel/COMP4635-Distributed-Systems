# COMP 4635
## Assignment #1: Java Phrase Guessing Game

#### Part of the design had to adhere to instructor specifications.
**Please view `COMP4635_W23_A1.pdf` for full technical requirements.**

#### This was a group project, I am the sole author of the following: 
- **IndirectionServer.java:** This microservice serves as a coordination layer between various client requests and other microservices, acting as an intermediary to facilitate communication and service orchestration. 
    1. Service Coordination: The Indirection Server functions as a central hub that directs requests from clients to appropriate backend services, such as game logic, user management, and a word repository service. This helps decouple client interfaces from direct dependencies on specific service implementations.
    2. Load Balancing and Scalability: Manages incoming client connections and distributing requests across different services and resources, effectively load balancing and scaling operations dynamically based on demand.
    3. Session and Connection Management: It maintains client sessions and manages connections to other microservices, ensuring that all interactions are handled efficiently and without direct client involvement in service discovery and interaction.

- **GameServer.java:** This microservice is primarily responsible for initiating, managing, and terminating game sessions in response to client requests, using the Game class. 
    1. Client-Server Communication: It establishes and maintains TCP connections with IndirectionServer, ensuring reliable data transmission for game actions and commands. The server listens on a specified port (default 5000) and handles incoming client requests in real-time.
    2. Session Management: Manages individual game sessions for connected clients. It creates a new session for each connection, handles game-related commands (like starting a new game or ending the current game), and maintains the state of ongoing games.
    3. Concurrent Client Handling: Utilizes a thread pool to manage multiple client connections efficiently. The server can handle up to 128 concurrent clients.

- **Game.java:** Contains the game logic of the application. Utilized by GameServer.
    1. Game State Management: It maintains the state of the game, including the current phrase to be guessed, the representation of the phrase as it is revealed through player guesses, and the count of failed attempts allowed before the game is lost.
    2. Game Progress Feedback: It provides feedback to the player and the server about the current status of the game, including the updated visual representation of the phrase and the number of remaining attempts.
    3. End-of-Game Determination: The class determines when the game ends, either through successful completion of the phrase or exhaustion of the allowed failed attempts, and it resets or updates the game environment accordingly for subsequent rounds.
    
- **Command.java:** Serves as a utility for parsing and handling client commands and their arguments. Utilized by services GameServer and IndirectionServer.

- **User.java:** Used by IndirectionServer service to encapsulate and manage the data and actions related to an individual user within the application.

---

### Overview

This project involves developing a Java client-server distributed application that allows users to play a phrase guessing game. The client interacts with the server to guess words selected by the server based on a chosen game level.

#### Main Components:

- **Client Application:** Interacts with the user, connecting to the server to play the game, guessing letters or the whole phrase.
- **Indirection Server Application:** Serves as a coordination layer between various client requests and other microservices. See IndirectionServer.java above for more information.
- **Game Server Application:** Responsible for initiating, managing, and terminating game sessions in response to client requests. See GameServer.java above for more information.
- **Word Repository Microservice:** Handles word storage and retrieval, allowing addition, removal, and checks of words.
- **User Account Microservice:** Manages user accounts and their associated data on a server, including creation, updating, signing in/out, updating scores, and retrieving user scores.

#### Key Features:

##### Connection and Setup:
- **Secure Connection:** Clients sign in, and the server provides a list of available commands.
- **Game Initialization:** Clients request to start a game by specifying the desired level and number of allowed failed attempts.

##### Gameplay:
- **Guessing Phrases:** Clients guess by suggesting letters or the entire phrase.
- **Score and Attempts Tracking:** The server updates and displays the score and the number of remaining failed attempts.
- **Word Management:** Clients can manage words in the repository (add, remove, check).

##### Server Responses:
- **Game Updates:** Depending on the client's input, the server updates the phrase visibility, score, and failed attempts.
- **End Game Conditions:** The game ends either when the phrase is correctly guessed or when the failed attempts counter reaches zero.

#### User Interface:
- **Client Interface:** Command-line based, allowing inputs for game actions and word management.
- **Display Information:** During gameplay, the phrase, failed attempts counter, and score are continuously updated.

#### Advanced Features:
- **Multiplayer Capability:** Initially handling a single client, with scalability to support multiple players simultaneously.
- **Microservices Architecture:** Separate services for user accounts and word repository, enhancing modularity and scalability.
- **Optional Indirection Server:** Coordinates communication between clients and other microservices for advanced setups.

#### Communication:
- **Protocols:** Uses TCP for reliable communication between the client and server, and UDP for the word repository service.
- **Messages:** Clearly defined message formats and protocols for requesting and receiving data.

---

