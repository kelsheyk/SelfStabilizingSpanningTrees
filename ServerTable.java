package selfstabilizingspanningtree;
import java.util.*;

// TODO: Make a HashMap so we can store IDs not indexed at 0 
//       and so we can add/ remove as needed

public class ServerTable{
    public final HashMap<String, ServerInfo> servers = new HashMap();
        
        public class ServerInfo{
            public final String ID;
            public final String hostAddress;
            public final int portNum;

            public ServerInfo(String idHostPort){
                String[] idHostPortArray = idHostPort.split(":");
                this.ID = idHostPortArray[0];
                this.hostAddress = idHostPortArray[1];
                this.portNum = Integer.parseInt(idHostPortArray[2]);
            }
        }
        
        public ServerTable(int numOfServers, String[] arrayOfServerInfo){
            System.out.println("HEREEEE" + numOfServers);
            for(int i = 0; i < numOfServers; i++){
                ServerInfo thisServ = new ServerInfo(arrayOfServerInfo[i]);
                System.out.println(thisServ.ID + " : " + thisServ.hostAddress);
                this.servers.put(thisServ.ID, thisServ);
            }
        }
    }