/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package selfstabilizingspanningtree;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 *
 * @author kking
 */

public class TCPSocket implements Runnable {
    int portNum;
    AggerwalAlgoServer ns;
    ServerSocket listener;
    Socket currentSocket;

    public TCPSocket(int portNum, AggerwalAlgoServer ns) {
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
            //TODO: if cannot connect assume crash? remove from neighbors?
            System.err.println("Send error: " + e);
        }
    }  

    void _receive(Socket s, String msg) {
        this.currentSocket = s;        
        String[] tokens = msg.split(" ");
        // If new id:host:port before, add it to neighbors
        String sendersID = tokens[1];
        if (ns.neighbors.servers.get(sendersID) == null) {
            String newServer = sendersID+":"+s.getInetAddress();
            ns.neighbors.addServer(newServer);
        }
        if (tokens[0].equals("requestData")) {
            //    sender's ID
            ns.sendData(Integer.parseInt(sendersID));
        } else if (tokens[0].equals("sendData")) {
            //          sender's ID,              dataJsonString            
            ns.receiveData(Integer.parseInt(sendersID), tokens[2]);
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
