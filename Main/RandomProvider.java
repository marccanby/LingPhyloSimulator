package Main;

import umontreal.ssj.rng.RandomStream;

import java.util.HashMap;

//All details of the implementation of random number 
//generation are hidden into this class

public class RandomProvider {

	public final boolean use_libssj;
	public RandomWrapper[] wrappers;
	public HashMap<String, Integer> indexMapping;

	public RandomProvider() {
		wrappers = new RandomWrapper[4];
		int topology_random_seed = 1;
		int ultrametric_random_seed = 1;
		int rates_random_seed = 1;
		int evolution_random_seed = 1;
		int discard = 100;
		use_libssj = false;

		indexMapping =  new HashMap<String, Integer>();
		indexMapping.put("topology", 0);
		indexMapping.put("ultrametric", 1);
		indexMapping.put("rates", 2);
		indexMapping.put("evolution", 3);
		wrappers[indexMapping.get("topology")] = new RandomWrapper(use_libssj, topology_random_seed, discard); // topology
		wrappers[indexMapping.get("ultrametric")] = new RandomWrapper(use_libssj, ultrametric_random_seed, discard); // ultrametric
		wrappers[indexMapping.get("rates")] = new RandomWrapper(use_libssj,  rates_random_seed, discard); // rates
		wrappers[indexMapping.get("evolution")] = new RandomWrapper(use_libssj, evolution_random_seed, discard); // evolution
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
		}
		return result;
	}

	public RandomStream getStream(int type) {
		RandomStream result = null;
		if (type >= wrappers.length) {
			System.out.println("Invalid random number type requested");
			System.out.println("in Main.RandomProvider.java. Should stop now");
		} else if (!use_libssj) {
			System.out.println("Not using SSJ: cannot request RandomStream");
			System.out.println("in Main.RandomProvider.java.Should stop now");
		} else {	
			RandomWrapper my_wrapper = wrappers[type];
			result = my_wrapper.getStream();
		}
		return result;
	}
}
