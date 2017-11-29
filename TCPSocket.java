/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.*;
import java.net.*;

/**
 *
 * @author kking
 */

public class TCPSocket extends Thread {
    Socket mySocket;
    AggerwalAlgoServer ns;
    boolean notDone = true;

    public TCPSocket(Socket s, AggerwalAlgoServer ns) {
        this.mySocket = s;
        this.ns = ns;
    }

    public void run() {

        BufferedReader in = null; 
         PrintWriter out = null; 
         try { 
            in = new BufferedReader(
               new InputStreamReader(mySocket.getInputStream()));
            
                String clientCommand = in.readLine();
                if (clientCommand != null) {
                    String[] tokens = clientCommand.split(" ");
                    // If new id:host:port before, add it to neighbors
                    String sendersStrID = tokens[1];
                    int sendersID = Integer.parseInt(tokens[1]);
                    if (ns.neighbors.servers.get(sendersStrID) == null) {
                        String newServer = sendersStrID+":"+mySocket.getInetAddress();
                        ns.neighbors.addServer(newServer);
                    }
                    if (tokens[0].equals("requestData")) {
                        //    sender's ID,              dataJsonString
                        ns.receiveData(sendersID, tokens[2]);
                        String myData = ns.packageSharedData(sendersID);
                        ServerTable.ServerInfo server_v = ns.neighbors.servers.get(sendersStrID);
                        PrintWriter pout = null;
                        Socket s = null;
                        try {
                            s = new Socket(server_v.hostAddress, server_v.portNum);
                            pout = new PrintWriter(s.getOutputStream());
                            pout.println("sendData " + sendersStrID + " " + myData);
                            pout.flush();
                        } catch (Exception e) { 
                            e.printStackTrace(); 
                        } 
                        finally { 
                            try {
                                pout.close();
                                s.close();
                            } catch(IOException ioe) { 
                                ioe.printStackTrace(); 
                            }
                        }
                    } else if (tokens[0].equals("sendData")) {
                        //          sender's ID,              dataJsonString            
                        ns.receiveData(sendersID, tokens[2]);
                    }
            } 
         } catch (Exception e) { 
            e.printStackTrace(); 
         } 
         finally { 
            try { 
               in.close(); 
               mySocket.close(); 
            } catch(IOException ioe) { 
               ioe.printStackTrace(); 
            } 
         }
         return;       
    }
}
