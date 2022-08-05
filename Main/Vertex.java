package Main;

import java.util.ArrayList;

/**
 * Created by marccanby on 7/16/20.
 */
public class Vertex {

    public Edge IncomingEdge;
    public ArrayList<Edge> OutgoingEdges = new ArrayList<>();
    public int Id;
    public String Taxon;
    public double Time;
    public ReticulateEdge reticulateEdge = null;

    public Vertex(int id, double time, String taxon) {
        Id = id;
        Taxon = taxon;
        Time = time;
    }

    public Vertex(int id, double time) {
        Id = id;
        Taxon = null;
        Time = time;
    }

    // Printing stuff
    public void printVertex() {
        System.out.println("Node #" + Integer.toString(Id) + (Taxon != null ? " - Taxon: " + Taxon : "") + "; Time: " + String.valueOf(Time));
    }
}
