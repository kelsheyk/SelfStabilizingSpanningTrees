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

    public void listenSocket(){
  
  while(true){
    ThreadWorker w;
    try{
//server.accept returns a client connection
      w = new ThreadWorker(server.accept(), textArea);
      Thread t = new Thread(w);
      t.start();
    } catch (IOException e) {
      System.out.println("Accept failed: 4444");
      System.exit(-1);
    }
  }
}
}
