package Main;

//import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.special.Beta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

//All details of the implementation of random number 
//generation ar hidden into this class

public class RandomWrapper {
	public final int seed;
	public final Random java_generator;
	public HashMap<String, BetaDistribution> beta_distributions = new HashMap<>();
	public final int discard;

	public RandomWrapper(int my_seed,
						 int discard // Number of first draws to be discarded initially from the random stream;
						 ) {
		seed = my_seed;
		java_generator = new Random((long)my_seed);
		for (int i = 0; i < discard ;i++) {
			java_generator.nextDouble();
		}
		this.discard = discard;

	}

	public void addBetaDistribution(String name, double alpha, double beta) {
		BetaDistribution beta_distribution = null;
		assert (alpha == -1 && beta == -1) || (alpha > 0 && beta > 0);
		if (alpha != -1) {
			beta_distribution = new BetaDistribution(alpha, beta);
			for (int i = 0; i < discard ;i++) {
				beta_distribution.sample();
			}
		}
		this.beta_distributions.put(name, beta_distribution);
	}

	public double nextDouble() {
		double result;
		result = java_generator.nextDouble();
		return result;
	}

	public double nextGaussian() {
		double result;
		result = java_generator.nextGaussian();
		return result;
	}

//	public double nextGamma() {
//		assert gamma_distribution != null;
//		double result;
//		result = gamma_distribution.sample();
//		assert result > 0;
//		if (result > 1) result = 1.0; // Force it not to be greater than 1......
//		return result;
//	}

	public double nextBeta(String name) {
		assert beta_distributions.containsKey(name);
		double result;
		BetaDistribution beta_distribution = beta_distributions.get(name);
		result = beta_distribution == null ? 0.0 : beta_distribution.sample();
		assert result >= 0 && result <= 1;
		return result;
	}

}

