/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.*;
import org.json.simple.*;
import org.json.simple.parser.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.FileReader;


public class AggerwalAlgoServer {
    // Needed for communication
    String myHost;
    int myPort;
    ServerTable neighbors;
    
    int ID;
    TCPSocket tcpSocket;
    
    // "shared" vars
    priorityScheme priority; 
    int distance;
    int parent;
    int color; 
    boolean other_trees;
    JSONObject neighbor_colors = new JSONObject();
    
    // "local" vars
    JSONObject neighbor_data;
    
    public AggerwalAlgoServer (int id, int numServ, String neighborFile){
        //super(id,numServ);
        this.ID = id;
        Scanner scanner = new Scanner(System.in);
        String userInput;

        String[] servers = new String[numServ];
        int i = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(neighborFile));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                servers[i] = line;
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
                i++;
            }
        } catch (Exception e) {
            System.out.println(e);
        } 
        this.neighbors = new ServerTable(numServ, servers);
        String s_id = Integer.toString(this.ID);
        ServerTable.ServerInfo myInfo = this.neighbors.servers.get(s_id);
        this.myHost = myInfo.hostAddress;
        this.myPort = myInfo.portNum;
        
        this.other_trees=false;
        this.parent = -1;
        this.color = -1;
        this.distance = 0;
        this.priority = new priorityScheme();
        
        // initialize neighbor_data
        this.neighbor_data = new JSONObject();
        HashMap data_v = new HashMap();
        data_v.put("priority", "0");
        data_v.put("distance", 0);
        data_v.put("parent", -1);
        data_v.put("color", -1);
        data_v.put("self_color", -1);
        data_v.put("other_trees", true);
        Iterator it = this.neighbors.servers.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<String, ServerTable.ServerInfo> pair = (HashMap.Entry)it.next();
            int ID_v = Integer.parseInt(pair.getKey());
            if (ID_v != this.ID) {
                this.neighbor_data.put(ID_v, data_v);
            }
        }
        
    }
       
    // copies neighbor data into local vars, performs coloring tasks
    void copy_neighbor_data() {
        Iterator it = this.neighbors.servers.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<String, ServerTable.ServerInfo> pair = (HashMap.Entry)it.next();
            int ID_v = Integer.parseInt(pair.getKey());
            if (ID_v != this.ID) {
                requestData(ID_v);
            }
        }
    }
    
    
    // become child of neighbor with max priority or become root
    void maximize_priority() {
        int max_node = -1; //this.ID;
        priorityScheme max_priority = new priorityScheme("-1"); //this.priority;
        int max_distance = -1; //this.distance;
        // we have to compare on priority AND distance.
        Iterator it = this.neighbors.servers.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<String, ServerTable.ServerInfo> pair = (HashMap.Entry)it.next();
            int ID_v = Integer.parseInt(pair.getKey());
            if (ID_v != this.ID) {
                HashMap data_v = (HashMap) this.neighbor_data.get(ID_v);
                priorityScheme priority_v = new priorityScheme((String) data_v.get("priority"));
                int distance_v =  Integer.parseInt(data_v.get("distance").toString());

                //TODO: since ID based priority is unique, shoud we base on distance,priority?
                if ((priority_v.greaterThan(max_priority.priority)) || 
                    ((priority_v.equals(max_priority.priority)) && (distance_v > max_distance))
                ) {
                    max_priority.priority = priority_v.priority;
                    max_distance = distance_v;
                    max_node = ID_v;
                    //System.out.println("MAX_NODE= " +max_node + "  " + priority_v.toString() + "    " + max_priority.toString());
                }
                
                // force root to extend 1st, if about to be overrun by a suffix ID?
                // NOT needed for correctness, see statement F
                
                // if u can improve its priority, by becoming child of another 
                // neighbor, do so, otherwise become root
                
                if ((max_priority.greaterThan(this.priority.priority)) ||
                    ((max_priority.equals(this.priority.priority)) && (max_distance > this.distance))
                ) {
                    this.priority = max_priority;
                    this.distance = max_distance + 1;
                    this.parent = max_node;
                    System.out.println("Node " + this.ID + " is child of " + max_node);
                } else {
                    this.distance = 0;
                    this.parent = -1;
                    System.out.println("Node " + this.ID + " is root");
                }
            }
        }
    }
    
    void detect_trees() {
        // Diff tress if priority the same, but color diff
        boolean same_tree = true;
        //boolean all_child_echo = true;
        boolean color_same = true;
        boolean other_tree_detected = false;
        Iterator it = this.neighbors.servers.entrySet().iterator();
        while (same_tree && it.hasNext()) {
            HashMap.Entry<String, ServerTable.ServerInfo> pair = (HashMap.Entry)it.next();
            int ID_v = Integer.parseInt(pair.getKey());
            if (ID_v != this.ID) {
                HashMap data_v = (HashMap) this.neighbor_data.get(ID_v);
                priorityScheme priority_v = new priorityScheme((String) data_v.get("priority"));
                int distance_v = Integer.parseInt(data_v.get("distance").toString());
                if ( (!(this.priority.equals(priority_v.priority))) ||
                    (Math.abs(this.distance-distance_v) > 1)
                ) {
                    same_tree = false;
                }
                int color_v = Integer.parseInt(data_v.get("color").toString());
                if (color_v != this.color) {
                    color_same = false;
                }
                int parent_v = Integer.parseInt(data_v.get("parent").toString());
                if (parent_v == this.ID) {
                    if ((boolean) data_v.get("other_trees") == true) {
                        other_tree_detected = true;
                    }
                }
            }
        }        
        if (same_tree) {
            if ((!(color_same) ||  other_tree_detected)) {
                this.other_trees = true;
            }
        }
        
    }
    
    void extend_priority() {
        if ((this.parent == -1) &&
            (this.other_trees == true)
        ) {
            appendEntry();
            resetColor(this.ID);
        }
        
    }
    
    void next_color() {
        /* If root, choose new color if necessary */
        if ((this.parent == -1) && 
            (this.other_trees == false)
        ) {
            resetColor(this.ID);
        }
    }
    
    public String packageSharedData(int ID_v) {
        Map<String, Object> myData = new HashMap<String, Object>();
        myData.put("priority",this.priority.toString().replaceAll("\\s+",""));
        myData.put("distance",this.distance);
        myData.put("parent", this.parent);
        myData.put("color", this.color);
        myData.put("other_trees", this.other_trees);
        // TODO: don't think I need this
        //myData.put("self_color", (int) this.neighbor_colors.get(ID_v));
        JSONObject json = new JSONObject();
        json.putAll(myData);
        String stringData = json.toJSONString();
        return stringData;
    }
    
    // Sends request for data to v
    void requestData(int ID_v) {
        String myData = this.packageSharedData(ID_v);
        this.tcpSocket._send(ID_v,"requestData " + this.ID + " " + myData);
    }
    
    //sends shared vars in stringified JSON form to v
    void sendData(int ID_v) {
        String myData = this.packageSharedData(ID_v);
        this.tcpSocket._send(ID_v, "sendData " + this.ID + " " + myData);
    }
    
    //receives data from v. copies to local vars. Coloring stuff
    void receiveData(int ID_v, String dataString) {
        JSONParser parser = new JSONParser();
        HashMap data = new HashMap();
        try {
            JSONObject dataJson = (JSONObject) parser.parse(dataString);
            //data = jsonToMap(dataJson);
            data.put("priority", dataJson.get("priority"));
            data.put("distance", dataJson.get("distance"));
            data.put("parent", dataJson.get("parent"));
            data.put("color", dataJson.get("color"));
            //data.put("mode", dataJson.get("mode"));
            data.put("other_trees", dataJson.get("other_trees"));
            // TODO: don't think I need this
            data.put("self_color", dataJson.get("self_color"));
        } catch (Exception e) {
            System.out.println("JSON parse error: " + e);
        }
        
        this.neighbor_data.put(ID_v,data);
        priorityScheme priority_v = new priorityScheme((String) data.get("priority"));
        int distance_v = Integer.parseInt(data.get("distance").toString());
        if ((this.priority.equals(priority_v.priority)) &&
            (Math.abs(distance - distance_v) <= 1)
        ) {
            //record color of neighbor if needed
            int color_v = Integer.parseInt(data.get("color").toString());
            if ((this.color != 0) && (color_v !=0)) {
                this.neighbor_colors.put(ID_v,color_v);
                if (this.color != color_v) {
                    this.other_trees = true;
                }
            }
            // if parent, copy color
            if ((this.parent == ID_v) && (color != color_v)) {
                resetColor(color_v);
            }
        }
    }
    
    void resetColor(int newColor) {
        this.color = newColor;
        //this.mode = 0; // or Broadcast
        this.other_trees = false;
        Iterator it = this.neighbors.servers.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<String, ServerTable.ServerInfo> pair = (HashMap.Entry)it.next();
            int ID_v = Integer.parseInt(pair.getKey());
            if (ID_v != this.ID) {
                this.neighbor_colors.put(ID_v,-1);
                HashMap data_v = (HashMap) this.neighbor_data.get(ID_v);
                data_v.put("self_color",-1);
                this.neighbor_data.put(ID_v, data_v);
                // if v is child, clear color
                if (Integer.parseInt((data_v.get("parent").toString())) == ID) {
                    data_v.put("color", -1);
                    this.neighbor_data.put(ID_v, data_v);   
                }
            }
        }
    }
    
    void appendEntry() { 
        // always append UID
        this.priority.priority.add(this.ID);
    }
    
    public static void main (String[] args) {
        int serverID;
        int numServ;
        if (args.length != 3) {
            System.out.println("ERROR: Provide 2 arguments");
            System.out.println("\t(1) <serverID>: the unique ID of this server");
            System.out.println("\t(2) <numServ>: the number of neighboring servers");
            System.out.println("\t(2) <numServ>: text file of neighbor info");
            System.exit(-1);
        }
        serverID = Integer.parseInt(args[0]);
        numServ = Integer.parseInt(args[1]);
        String neighborFileName = args[2];
            
        // Kick off Listener
        AggerwalAlgoServer ns = new AggerwalAlgoServer(serverID, numServ, neighborFileName);
        TCPSocket s1 = new TCPSocket(ns.myPort, ns);
        Thread t1=new Thread(s1);
        t1.start();

        // TODO: figure out a stopping condition
        int rounds = 0;
        while (rounds < 5) {
            ns.copy_neighbor_data();
            // TODO : wait for copy to complete
            ns.maximize_priority();
            ns.next_color();
            ns.detect_trees();
            ns.extend_priority();
            rounds++;
        }
        for (int i=0; i<999999; i++) {
            ;
        }
        System.exit(0);
    }
}
