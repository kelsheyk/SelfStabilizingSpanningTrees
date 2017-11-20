/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package selfstabilizingspanningtree;
import java.util.*;
import org.json.simple.*;
import org.json.simple.parser.*;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;



/**
 *
 * @author kking
 */
public class AggerwalAlgoServer {
    // Needed for communication
    String myHost;
    int myPort;
    ServerTable neighbors;
    
    int ID;
    tcpSocket tcpSocket;
    
    // "shared" vars
    priorityScheme priority; 
    int distance;
    int parent;
    int color; //TODO: init to -1
    boolean other_trees;
    JSONObject neighbor_colors;
    
    // "local" vars
    JSONObject neighbor_data;
    
    public AggerwalAlgoServer (int id, int numServ){
        //super(id,numServ);
        this.ID = id;
        Scanner scanner = new Scanner(System.in);
        String userInput;

        String[] servers = new String[numServ];
        for (int i = 0; i < numServ; i++) {
            System.out.println("Enter the id:host:port for each server");
            userInput = scanner.nextLine();
            // XXX: assumes perfect input. Should add err checking
            servers[i] = userInput;    
        }
        this.neighbors = new ServerTable(numServ, servers);
        System.out.println(this.ID);
        String s_id = Integer.toString(this.ID);
        ServerTable.ServerInfo myInfo = this.neighbors.servers.get(s_id);
        this.myHost = myInfo.hostAddress;
        this.myPort = myInfo.portNum;
        
        this.other_trees=false;
        this.parent = -1;
        this.color = -1;
        this.distance = -1;
        this.priority = new priorityScheme();
    }
    
    
    class priorityScheme {
        ArrayList<Integer> priority = new ArrayList<Integer>();
        
        public priorityScheme(ArrayList<Integer> newPriority) {
            this.priority = newPriority;
        }
        
        public priorityScheme() {
            this.priority = new ArrayList<Integer>();
        }
        
        boolean greaterThan(ArrayList<Integer> priority_v) {
            int minLen = Math.min(priority.size(),priority_v.size());
            boolean eq = true;
            for (int i=0; i<minLen; i++) {
                if (priority.get(i) < priority_v.get(i)) {
                    // priority is strongly less than priority_v
                    return false;
                }
                if (priority.get(i) != priority_v.get(i)) {
                    eq = false;
                }
            }
            if (eq && priority.size() <= priority_v.size()) {
                // priority is weakly less than priority_v
                return false;
            }
            return true;
        }
        
        boolean lessThanEq(ArrayList<Integer> priority_v) {
            return !(greaterThan(priority_v));
        }
        
        boolean equals(ArrayList<Integer> priority_v) {
            if (priority.size() != priority_v.size()) {
                return false;
            }
            for (int i=0; i<priority.size(); i++) {
                if (priority.get(i) != priority_v.get(i)) {
                    return false;
                }
            }
            return true;
        }
    }
       
    
    // copies neighbor data into local vars, performs coloring tasks
    void copy_neighbor_data() {
        Iterator it = this.neighbors.servers.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            int ID_v = (int) pair.getKey();
            requestData(ID_v);
        }
    }
    
    
    // become child of neighbor with max priority or become root
    void maximize_priority() {
        int max_node = -1; //this.ID;
        priorityScheme max_priority = new priorityScheme(); //this.priority;
        int max_distance = -1; //this.distance;
        // we have to compare on priority AND distance.
        Iterator it = this.neighbors.servers.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            int ID_v = (int) pair.getKey();
            HashMap data_v = (HashMap) this.neighbor_data.get(ID_v);
            priorityScheme priority_v = new priorityScheme((ArrayList<Integer>) data_v.get("priority"));
            int distance_v = (int) data_v.get("distance");
            //TODO: since ID based priority is unique, shoud we base on distance,priority?
            if ((priority_v.greaterThan(max_priority.priority)) || 
                ((priority_v.equals(max_priority.priority)) && (distance_v > max_distance))
            ) {
                max_priority = priority_v;
                max_distance = distance_v;
                max_node = ID_v;
            }
            
            // force root to extend 1st, if about to be overrun by a suffix ID?
            // NOT needed for correctness, see statement F
            //if ((this.parent == -1) && (this.priority <)) {
            // appendEntry()
            
            // if u can improve its priority, by becoming child of another 
            // neighbor, do so, otherwise become root 
            if ((max_priority.greaterThan(this.priority.priority)) ||
                ((max_priority.equals(this.priority.priority)) && (max_distance > this.distance))
            ) {
                this.priority = max_priority;
                this.distance = max_distance + 1;
                this.parent = max_node;
            } else {
                this.distance = 0;
                this.parent = -1;
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
            HashMap.Entry pair = (HashMap.Entry)it.next();
            int ID_v = (int) pair.getKey();
            HashMap data_v = (HashMap) this.neighbor_data.get(ID_v);
            ArrayList<Integer> priority_v = (ArrayList<Integer>) data_v.get("priority");
            int distance_v = (int) data_v.get("distance");
            if ( (!(this.priority.equals(priority_v))) ||
                (Math.abs(this.distance-distance_v) > 1)
            ) {
                same_tree = false;
            }
            if ((int) data_v.get("color") != this.color) {
                color_same = false;
            }
            if ((int) data_v.get("parent") == this.ID) {
                if ((boolean) data_v.get("other_trees") == true) {
                    other_tree_detected = true;
                }
            }
        }        
        if (same_tree) {
            if ((!(color_same) ||  other_tree_detected)) {
                this.other_trees = true;
            }
        }
        
    }
    
    void extend_id() {
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
    
    // Sends request for data to v
    void requestData(int ID_v) {
        this.tcpSocket._send(ID_v,"requestData " + this.ID);
    }
    
    //sends shared vars in stringified JSON form to v
    void sendData(int ID_v) {
        Map<String, Object> myData = new HashMap<String, Object>();
        myData.put("priority",this.priority);
        myData.put("distance",this.distance);
        myData.put("parent", this.parent);
        myData.put("color", this.color);
        myData.put("other_trees", this.other_trees);
        // TODO: don't think I need this
        myData.put("self_color", (int) this.neighbor_colors.get(ID_v));
        JSONObject json = new JSONObject();
        json.putAll(myData);
        String stringData = json.toJSONString();
        this.tcpSocket._send(ID_v, "sendData " + this.ID + " " + stringData);
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
        
        // TODO: debug -- surely this won't work 1st try
        //System.out.println(data.toString());
        this.neighbor_data.put(ID_v,data);
        
        ArrayList<Integer> priority_v = (ArrayList<Integer>) data.get("priority");
        
        if ((this.priority.equals(priority_v)) &&
            (Math.abs(distance - (int) data.get("distance")) <= 1)
        ) {
            //record color of neighbor if needed
            int color_v = (int) data.get("color");
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
            HashMap.Entry pair = (HashMap.Entry)it.next();
            int ID_v = (int) pair.getKey();
            this.neighbor_colors.put(ID_v,-1);
            HashMap data_v = (HashMap) this.neighbor_data.get(ID_v);
            data_v.put("self_color",-1);
            this.neighbor_data.put(ID_v, data_v);
            // if v is child, clear color
            if ((int) data_v.get("parent") == ID) {
                data_v.put("color", -1);
                this.neighbor_data.put(ID_v, data_v);   
            }
        }
    }
    
    void appendEntry() { 
        // always append UID
        this.priority.priority.add(this.ID);
    }
    
    public static class tcpSocket implements Runnable {
        int portNum;
        AggerwalAlgoServer ns;
        ServerSocket listener;
        Socket currentSocket;
      
        public tcpSocket(int portNum, AggerwalAlgoServer ns) {
            this.portNum = portNum;
            this.ns = ns;
            ns.tcpSocket = this;
        }
        
        void _send(int ID_v, String msg) {
            try {
                Socket s = this.currentSocket;
                PrintWriter pout = new PrintWriter(s.getOutputStream());
                pout.println(msg);
                pout.flush();
                s.close();
            } catch (IOException e) {
                System.err.println("Send error: " + e);
            }
        }  
    
        void _receive(Socket s, String msg) {
            this.currentSocket = s;
            String[] tokens = msg.split(" ");
            if (tokens[0].equals("requestData")) {
                //    sender's ID
                ns.sendData(Integer.parseInt(tokens[1]));
            } else if (tokens[0].equals("sendData")) {
                //          sender's ID,              dataJsonString            
                ns.receiveData(Integer.parseInt(tokens[1]), tokens[2]);
            }
        }

        public void run() {
            try {
                this.listener = new ServerSocket(this.portNum);
                Socket s;
                while((s = this.listener.accept()) != null) {
                    Scanner sc = new Scanner(s.getInputStream());
                    String command;
                    command = sc.nextLine();
                    _receive(s,command);
                }
            } catch (Exception e) {
              System.err.println("Server aborted:" + e);
            }
        }
    }
    
    public static void main (String[] args) {
        int serverID;
        int numServ;
        /*if (args.length != 2) {
            System.out.println("ERROR: Provide 2 arguments");
            System.out.println("\t(1) <serverID>: the unique ID of this server");
            System.out.println("\t(2) <numServ>: the number of neighboring servers");

            System.exit(-1);
        }
        serverID = Integer.parseInt(args[0]);
        numServ = Integer.parseInt(args[1]);  
        */
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter ID of Server:");
        serverID = Integer.parseInt(scanner.nextLine());
        System.out.println("Enter # of Servers:");
        numServ = Integer.parseInt(scanner.nextLine());
            

        //Listener
        AggerwalAlgoServer ns = new AggerwalAlgoServer(serverID, numServ);
        System.out.println("Server started:");
        tcpSocket s1 = new tcpSocket(ns.myPort, ns);
        Thread t1=new Thread(s1);
        t1.start();   
    }
}
