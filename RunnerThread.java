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
        while (rounds < 50) {
            if (rounds == 0 ) {
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {}
            } else {
                try {
                    ns.detect_trees();
                } catch (Exception e) {}
                try {
                    ns.maximize_priority();
                } catch (Exception e) {}
                try {
                    ns.next_color();
                } catch (Exception e) {}
                try {
                    ns.extend_priority();
                } catch (Exception e) {}
                try {
                    Thread.sleep(2500);
                } catch (Exception e) {}
            }
            try {
                ns.copy_neighbor_data();
                rounds++;
            } catch (Exception e) {}
        }
        ns.stop = true;
        return;
    }
}
