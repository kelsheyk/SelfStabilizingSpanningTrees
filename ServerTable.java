import java.util.*;

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
            for(int i = 0; i < numOfServers; i++){
                ServerInfo thisServ = new ServerInfo(arrayOfServerInfo[i]);
                this.servers.put(thisServ.ID, thisServ);
            }
        }
        
        public void addServer(String idHostPort) {
            ServerInfo newServer = new ServerInfo(idHostPort);
            this.servers.put(newServer.ID, newServer);
        }

        public void removeServer(int ID_v) {
            this.servers.remove(Integer.toString(ID_v));
        }
    }
