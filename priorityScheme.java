/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package selfstabilizingspanningtree;

import java.util.ArrayList;

/**
 *
 * @author kking
 */
public class priorityScheme {
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
