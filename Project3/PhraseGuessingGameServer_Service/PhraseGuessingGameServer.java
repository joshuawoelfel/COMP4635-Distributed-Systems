/*
 * Author: Joshua Wolfel
 */
import java.rmi.Remote;
import java.rmi.RemoteException;

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
