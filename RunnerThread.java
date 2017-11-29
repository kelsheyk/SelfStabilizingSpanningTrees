import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

class RunnerThread extends Thread implements Runnable {
    AggerwalAlgoServer ns;

    //Constructor
    RunnerThread(AggerwalAlgoServer ns) {
        this.ns = ns;
    }

    public void run() {
        int rounds = 0;
        while (rounds < 10) {
            if (rounds == 0 ) {
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {}
            } else {
                ns.detect_trees();
                ns.maximize_priority();
                ns.extend_priority();
                ns.next_color(); 
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {}
            }
            ns.copy_neighbor_data();
            System.out.println("Server " + ns.ID + " round " + rounds + " done.");
            rounds++;
        }
        ns.stop = true;
        return;
    }
}
