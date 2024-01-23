package Main;

/**
 * Created by marccanby on 7/17/20.
 */
public class Edge {

    Vertex parent;
    Vertex child;
    double length;
    int Id;
    String NewickRepresentation; // Represents clade BELOW it, used to describe edge

    public Edge(int id, Vertex p, Vertex c, double l) {
        Id = id;
        parent = p;
        child = c;
        length = l;
    }



    public void printEdge() {
        System.out.println("Edge #" + String.valueOf(Id) + " from Node #" + String.valueOf(parent.Id) + " to Node #" + String.valueOf(child.Id) + " - Length: " + String.valueOf(length));
    }

}
