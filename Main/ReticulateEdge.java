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

    public static double drawTransmissionStrength(double mu_trm, double sigma_trm, RandomProvider randomProvider) {
        double draw_value = -1;
        if (sigma_trm == 0) {
            draw_value = mu_trm;
        } else {
            draw_value = randomProvider.nextGaussian(randomProvider.indexMapping.get("topology"));
            draw_value = draw_value * sigma_trm + mu_trm; // transform into N(mu, sigma^2)
        }
        return draw_value;
    }

    public void printEdge() {
        System.out.println("RETICULATE Edge #" + String.valueOf(Id) + " between Node #" + String.valueOf(left.Id) + " and Node #" + String.valueOf(right.Id) + "; Strength: "+String.valueOf(transmission_strength));
    }

}
