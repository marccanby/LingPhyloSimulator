package Main;

import org.apache.commons.math3.distribution.GammaDistribution;

import java.util.Random;

//All details of the implementation of random number 
//generation ar hidden into this class

public class RandomWrapper {
	public final int seed;
	public final Random java_generator;
	public final GammaDistribution gamma_distribution;

	public RandomWrapper(int my_seed,
						 int discard,
						 double gamma_alpha,
						 double gamma_beta) { // Number of first draws to be discarded initially from the random stream;
		seed = my_seed;
		java_generator = new Random((long)my_seed);
		for (int i = 0; i < discard ;i++) {
			java_generator.nextDouble();
		}

		assert (gamma_alpha == -1 && gamma_beta == -1) || (gamma_alpha > 0 && gamma_beta > 0);
		if (gamma_alpha != -1) {
			// NOTE: This class uses the k-theta parametrization on wikipedia. so have to convert
			double k = gamma_alpha;
			double theta = 1/gamma_beta;
			gamma_distribution = new GammaDistribution(k, theta);
			for (int i = 0; i < discard ;i++) {
				gamma_distribution.sample();
			}
		}
		else {
			gamma_distribution = null;
		}

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

	public double nextGamma() {
		assert gamma_distribution != null;
		double result;
		result = gamma_distribution.sample();
		assert result > 0;
		if (result > 1) result = 1.0; // Force it not to be greater than 1......
		return result;
	}

}

