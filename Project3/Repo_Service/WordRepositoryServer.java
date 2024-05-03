// Landon Odishaw-Dyck
// Maintains a list of words, and allows the creation, removal, retrieval, and checking of the words therein. 

import java.rmi.*;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WordRepositoryServer extends Remote{

    public boolean createWord(String word, int seq) throws RemoteException;
    public boolean removeWord(String word, int seq) throws RemoteException;
    public boolean checkWord(String word, int seq) throws RemoteException;
    public String getRandomWord(int length, int seq) throws RemoteException;

}
