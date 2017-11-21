/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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

    public TCPSocket(int portNum, AggerwalAlgoServer ns) {
        this.portNum = portNum;
        this.ns = ns;
        ns.tcpSocket = this;
    }

    void _send(int ID_v, String msg) {
        try {
            ServerTable.ServerInfo server_v = ns.neighbors.servers.get(Integer.toString(ID_v));
            Socket s = new Socket(server_v.hostAddress, server_v.portNum);
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
        String[] tokens = msg.split(" ");
        // If new id:host:port before, add it to neighbors
        String sendersID = tokens[1];
        if (ns.neighbors.servers.get(sendersID) == null) {
            String newServer = sendersID+":"+s.getInetAddress();
            ns.neighbors.addServer(newServer);
        }
        if (tokens[0].equals("requestData")) {
            //    sender's ID
            ns.receiveData(Integer.parseInt(sendersID), tokens[2]);
            ns.sendData(Integer.parseInt(sendersID));
        } else if (tokens[0].equals("sendData")) {
            //          sender's ID,              dataJsonString            
            ns.receiveData(Integer.parseInt(sendersID), tokens[2]);
        }
    }

    public void run() {
        try {
            ServerSocket listener = new ServerSocket(this.portNum);
            Socket s;
            String command;
            while((s = listener.accept()) != null) {
                Scanner sc = new Scanner(s.getInputStream());
                command = sc.nextLine();
                System.out.println(command);
                _receive(s,command);
            }
        } catch (Exception e) {
          System.err.println("Server aborted:" + e);
        }
    }
}
