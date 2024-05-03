// Landon Odishaw-Dyck
// Maintains a database of user accounts. Allows to setting and getting of each users score, and 
// logging in and out of each account. Multithreaded is supported

import java.rmi.*;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface UserAccounts extends Remote {
  public String getUserScore(String username, int seq) throws RemoteException;
  public boolean login(String username, int seq) throws RemoteException;
  public boolean updateUserScore(String username, int score, int seq) throws RemoteException;
  public boolean logout(String username, int seq) throws RemoteException;
}
