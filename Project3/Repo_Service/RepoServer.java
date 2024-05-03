// Landon Odishaw-Dyck
// Creates and registers an object with the words, along with methods to manipulate them.

import java.rmi.*;
import java.rmi.registry.LocateRegistry;

public class RepoServer {
	
        private static final String HOST = "localhost";
	
	public static void main(String[] args) throws Exception {
        // Start rmiregistry.
        try {
            LocateRegistry.getRegistry(1099).list();
        } catch (RemoteException e) {
            LocateRegistry.createRegistry(1099);
        }
		
		//Create a reference to an implementation object...
		WordRepositoryServerImpl WordRepositoryServerInstance = new WordRepositoryServerImpl();
		
		//Create the string URL holding the object�s name...
		String rmiObjectName = "rmi://127.0.0.1/WordRepositoryServer";
				
		//�Bind� the object reference to the name...
		Naming.rebind(rmiObjectName, WordRepositoryServerInstance);


		System.out.println("Binding complete...\n");		
	}
}