package Main;

/**
 * Created by marccanby on 10/2/20.
 */
public class ReticulateEdge {

    Vertex left;
    Vertex right;
    int Id;
    double transmission_strength; // kappa_e

    public ReticulateEdge(int id, Vertex l, Vertex r) {
        Id = id;
        left = l;
        right = r;
    }

    public static double drawTransmissionStrength(RandomProvider randomProvider) {
        // NOTE: Random provider already has gamma distribution with correct alpha and beta for this purpose.
        double draw_value = -1;
        // draw_value = randomProvider.nextGamma(randomProvider.indexMapping.get("topology"));
        draw_value = randomProvider.nextBeta(randomProvider.indexMapping.get("topology"), "edge");
        assert draw_value > 0 && draw_value < 1;
        return draw_value;
    }

    public void printEdge() {
        System.out.println("RETICULATE Edge #" + String.valueOf(Id) + " between Node #" + String.valueOf(left.Id) + " and Node #" + String.valueOf(right.Id) + "; Strength: "+String.valueOf(transmission_strength));
    }

}
