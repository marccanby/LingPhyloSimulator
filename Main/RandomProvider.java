package Main;

import umontreal.ssj.rng.RandomStream;

import java.util.HashMap;

//All details of the implementation of random number 
//generation are hidden into this class

public class RandomProvider {

	public RandomWrapper[] wrappers;
	public HashMap<String, Integer> indexMapping;

	public RandomProvider(int seed) {
		wrappers = new RandomWrapper[3];
		int topology_random_seed = seed;
		int rates_random_seed = seed;
		int evolution_random_seed = seed;
		int discard = 100;

		indexMapping =  new HashMap<String, Integer>();
		indexMapping.put("topology", 0);
		indexMapping.put("rates", 1);
		indexMapping.put("evolution", 2);
		wrappers[indexMapping.get("topology")] = new RandomWrapper(topology_random_seed, discard); // topology
		wrappers[indexMapping.get("rates")] = new RandomWrapper(rates_random_seed, discard); // rates
		wrappers[indexMapping.get("evolution")] = new RandomWrapper(evolution_random_seed, discard); // evolution
	}

	public void addBetaDistribution(String name, double alpha, double beta) {
		wrappers[indexMapping.get("topology")].addBetaDistribution(name, alpha, beta);
		wrappers[indexMapping.get("rates")].addBetaDistribution(name, alpha, beta);
		wrappers[indexMapping.get("evolution")].addBetaDistribution(name, alpha, beta);

	}

	public double nextDouble(int type) {
		//Returns a random number uniformly 
		//distributed between 0 and 1
		double result = 0.0;
		if (type < wrappers.length) {
			RandomWrapper my_wrapper = wrappers[type];
			result = my_wrapper.nextDouble();
		} else {
			System.out.println("Invalid random number type requested");
			System.out.println("in Main.RandomProvider.java. Should stop now");
			assert false;
		}
		return result;
	}

	public double nextGaussian(int type) {
		//Returns a random number with normal
		//distribution centered on 0.0 and 
		//variance 1
		double result = 0.0;
		if (type < wrappers.length) {
			RandomWrapper my_wrapper = wrappers[type];
			result = my_wrapper.nextGaussian();
		} else {
			System.out.println("Invalid random number type requested");
			System.out.println("in Main.RandomProvider.java. Should stop now");
			assert false;
		}
		return result;
	}

//	public double nextGamma(int type) {
//		//Returns a number from Gamma(alpha, beta)
//		double result = 0.0;
//		if (type < wrappers.length) {
//			RandomWrapper my_wrapper = wrappers[type];
//			result = my_wrapper.nextGamma();
//		} else {
//			System.out.println("Invalid random number type requested");
//			System.out.println("in Main.RandomProvider.java. Should stop now");
//			assert false;
//		}
//		return result;
//	}

		public double nextBeta(int type, String name) {
		//Returns a number from Beta(alpha, beta)
		double result = 0.0;
		if (type < wrappers.length) {
			RandomWrapper my_wrapper = wrappers[type];
			result = my_wrapper.nextBeta(name);
		} else {
			System.out.println("Invalid random number type requested");
			System.out.println("in Main.RandomProvider.java. Should stop now");
			assert false;
		}
		return result;
	}

}
