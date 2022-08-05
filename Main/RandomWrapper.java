package Main;

import umontreal.ssj.probdist.NormalDist;
import umontreal.ssj.randvar.NormalGen;
import umontreal.ssj.rng.GenF2w32;
import umontreal.ssj.rng.RandomStream;

import java.util.Random;

//All details of the implementation of random number 
//generation ar hidden into this class

public class RandomWrapper {
	public final int seed;
	public final boolean use_libssj;
	public final Random java_generator;
	public final RandomStream ssj_generator;

	public RandomWrapper(boolean my_libssj,
                         int my_seed,
						 int discard) { // Number of first draws to be discarded initially from the random stream;
		seed = my_seed;
		use_libssj = my_libssj;
		java_generator = new Random((long)my_seed);
		for (int i = 0; i < discard ;i++) {
			java_generator.nextDouble();
		}
		if (use_libssj) {
			int[] seed = new int[25];
			for (int i = 0; i< 25; i++) {
				seed[i] = java_generator.nextInt();
			}
			GenF2w32.setPackageSeed(seed);
			ssj_generator = new GenF2w32();
		} else {
			ssj_generator = null;
		}
	}

	public double nextDouble() {
		double result;
		if (use_libssj) {
			result = ssj_generator.nextDouble();
		} else {
			result = java_generator.nextDouble();
		}
		return result;
	}

	public double nextGaussian() {
		double result;
		if (use_libssj) {
			NormalDist normal_dist = new NormalDist();
			NormalGen normal_gen = new NormalGen(ssj_generator,
							     normal_dist);
			result = normal_gen.nextDouble();
		} else {
			result = java_generator.nextGaussian();
		}
		return result;
	}

	public RandomStream getStream() {
		if (!use_libssj) {
			System.out.println("SSJ object requested in non-ssj mode");
			System.out.println("in RamdomWrapper. Should stop now.");
		}
		return ssj_generator;
	}
}

