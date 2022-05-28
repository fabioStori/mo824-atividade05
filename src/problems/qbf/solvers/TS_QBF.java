package problems.qbf.solvers;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Collections;

import metaheuristics.tabusearch.AbstractTS;
import problems.qbf.QBF_Inverse;
import solutions.Solution;

/**
 * Metaheuristic TS (Tabu Search) for obtaining an optimal solution to a QBF
 * (Quadractive Binary Function -- {@link #QuadracticBinaryFunction}).
 * Since by default this TS considers minimization problems, an inverse QBF
 *  function is adopted.
 * 
 * @author ccavellucci, fusberti
 */
public class TS_QBF extends AbstractTS<Integer> {
	
	/**
	 * KQBFInverse obj function
	 */
	public QBF_Inverse QBFInverse;

	public List<Integer> allCandidateList;

	public Boolean useProbabilisticTS;

	private final Integer fake = Integer.valueOf(-1);

	public ArrayList<Integer> SF;
	
	/**
	 * a random number generator
	 */
	static Random rng = new Random();

	/**
	 * Constructor for the TS_QBF class. An inverse QBF objective function is
	 * passed as argument for the superclass constructor.
	 * 
	 * @param tenure
	 *            The Tabu tenure parameter.
	 * @param iterations
	 *            The number of iterations which the TS will be executed.
	 * @throws IOException
	 *             necessary for I/O operations.
	 */
	public TS_QBF(Integer tenure, Integer iterations, Integer maxTimeInSeconds, QBF_Inverse QBFInverse, Boolean useProbabilisticTS) throws IOException {
		super(QBFInverse, tenure, iterations, maxTimeInSeconds);
		this.QBFInverse = QBFInverse;
		this.useProbabilisticTS = useProbabilisticTS;
		this.allCandidateList = makeCL();
	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeCL()
	 */
	@Override
	public ArrayList<Integer> makeCL() {

		ArrayList<Integer> _CL = new ArrayList<Integer>();
		for (int i = 0; i < ObjFunction.getDomainSize(); i++) {
			Integer cand = Integer.valueOf(i);
			_CL.add(cand);
		}

		return _CL;

	}

	/* 
	 * 
	 */	
	public ArrayList<Integer> createSolutionFrequencyArray() {

		ArrayList<Integer> _SF = new ArrayList<Integer>();
		for (int i = 0; i < ObjFunction.getDomainSize(); i++) {			
			_SF.add(0);
		}
		return _SF;
	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeRCL()
	 */
	@Override
	public ArrayList<Integer> makeRCL() {

		ArrayList<Integer> _RCL = new ArrayList<Integer>();

		return _RCL;

	}
	
	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#makeTL()
	 */
	@Override
	public ArrayDeque<Integer> makeTL() {

		ArrayDeque<Integer> _TS = new ArrayDeque<Integer>(2*tenure);
		for (int i=0; i<2*tenure; i++) {
			_TS.add(fake);
		}

		return _TS;

	}

	/* (non-Javadoc)
	 * @see metaheuristics.tabusearch.AbstractTS#updateCL()
	 */
	@Override
	public void 
	updateCL() {
		Double[] weights = QBFInverse.getWeights();
		// System.out.println("weights " + weights);
		Double freeCapacity = QBFInverse.getCapacity() - sol.usedCapacity;
		// System.out.println("freeCapacity " + freeCapacity);

		/*
		* Select only viable candidates given the free capacity and update the CL.
		* */
		ArrayList<Integer> newCL = new ArrayList<>();
		for (Integer candidate : allCandidateList) {
			if (!sol.contains(candidate) && weights[candidate] <= freeCapacity) {
				newCL.add(candidate);
			}
		}
		CL = newCL;

	}

	/**
	 * {@inheritDoc}
	 * 
	 * This createEmptySol instantiates an empty solution and it attributes a
	 * zero cost, since it is known that a QBF solution with all variables set
	 * to zero has also zero cost.
	 */
	@Override
	public Solution<Integer> createEmptySol() {
		Solution<Integer> sol = new Solution<Integer>();
		sol.cost = 0.0;
		sol.usedCapacity = 0.0;
		return sol;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * The local search operator developed for the QBF objective function is
	 * composed by the neighborhood moves Insertion, Removal and 2-Exchange.
	 */
	@Override
	public Solution<Integer> neighborhoodMove() {
		// System.out.println("neighborhoodMove sol1: "+sol);
		Double minDeltaCost;
		Integer bestCandIn = null, bestCandOut = null;

		minDeltaCost = Double.POSITIVE_INFINITY;
		updateCL();
		// Evaluate insertions

		if (useProbabilisticTS && CL.size() > 0) {
			// System.out.println("CL.size() " + CL.size());
			// System.out.println("rng.nextInt(CL.size()) " + rng.nextInt(CL.size()));
			rng.setSeed(10);
			int randomCLSize = rng.nextInt(CL.size());
			Collections.shuffle(CL, rng);
			List<Integer> newRandomList = CL.subList(0, randomCLSize);
			CL = newRandomList;
		}

		for (Integer candIn : CL) {
			Double deltaCost = ObjFunction.evaluateInsertionCost(candIn, sol);
			if (!TL.contains(candIn) || sol.cost+deltaCost < bestSol.cost) {
				if (deltaCost < minDeltaCost) {
					minDeltaCost = deltaCost;
					bestCandIn = candIn;
					bestCandOut = null;
				}
			}
		}
		// Evaluate removals
		for (Integer candOut : sol) {
			Double deltaCost = ObjFunction.evaluateRemovalCost(candOut, sol);
			if (!TL.contains(candOut) || sol.cost+deltaCost < bestSol.cost) {
				if (deltaCost < minDeltaCost) {
					minDeltaCost = deltaCost;
					bestCandIn = null;
					bestCandOut = candOut;
				}
			}
		}
		// Evaluate exchanges
		for (Integer candIn : CL) {
			for (Integer candOut : sol) {
				Double deltaCost = ObjFunction.evaluateExchangeCost(candIn, candOut, sol);
				if ((!TL.contains(candIn) && !TL.contains(candOut)) || sol.cost+deltaCost < bestSol.cost) {
					if (deltaCost < minDeltaCost) {
						minDeltaCost = deltaCost;
						bestCandIn = candIn;
						bestCandOut = candOut;
					}
				}
			}
		}
		// Implement the best non-tabu move
		TL.poll();
		if (bestCandOut != null) {
			sol.remove(bestCandOut);
			CL.add(bestCandOut);
			TL.add(bestCandOut);
		} else {
			TL.add(fake);
		}
		TL.poll();
		if (bestCandIn != null) {
			sol.add(bestCandIn);
			CL.remove(bestCandIn);
			TL.add(bestCandIn);
		} else {
			TL.add(fake);
		}
		ObjFunction.evaluate(sol);
		
		return null;
	}

	public void updateElementFrequencyArray(ArrayList<Integer> sol)
	{		
		for (int i = 0; i < sol.size(); i++) {			
			SF.set(sol.get(i), SF.get(sol.get(i))+1);
		}		
	}

	public void updateTL()
	{	
		Double fixedElements = new Double(sol.size())/2.0;
		Double ceil = Math.ceil(fixedElements);
		
		// TL = makeTL();
		for (int i = 0; i < ceil; i++) {		
			Integer moreFreq =  SF.indexOf(Collections.max(SF));			
			TL.poll();
			TL.add(moreFreq);			
			SF.set(moreFreq, 0);		

		}
		SF = createSolutionFrequencyArray();
	}
	
	public Solution<Integer> solveIntensification() {

		Instant started = Instant.now();
		bestSol = createEmptySol();
		constructiveHeuristic();
		TL = makeTL();
		SF = createSolutionFrequencyArray();		

		for (int i = 0; i < iterations; i++) {
			neighborhoodMove();		
			
			if (bestSol.cost > sol.cost) {
				bestSol = new Solution<Integer>(sol);
				if (verbose)
					System.out.println("(Iter. " + i + ") BestSol = " + bestSol);
			}
			
			updateElementFrequencyArray(bestSol);
			
			if(i > 0 && (i % (iterations/10)) == 0){		
				updateTL();	
				sol = new Solution<Integer>(bestSol);				
			}	
						

			if (Instant.now().getEpochSecond() > started.plusSeconds(maxTimeInSeconds).getEpochSecond()) {
				System.out.println("Interrupting");
				break;
			}
		}
		return bestSol;
	}

	/**
	 * A main method used for testing the TS metaheuristic.
	 * 
	 */
	public static void main(String[] args) throws IOException {

		long startTime = System.currentTimeMillis();

		QBF_Inverse QBF_Inverse = new QBF_Inverse("instances/kqbf/kqbf040");
		int maxTimeInSeconds = 30 * 60; // 30 minutes
		int ternure = 20;
		int iterations = 5000;
		boolean useProbabilisticTS = false;

		TS_QBF tabusearch = new TS_QBF(ternure, iterations, maxTimeInSeconds, QBF_Inverse, useProbabilisticTS);
		Solution<Integer> bestSol = tabusearch.solveIntensification();
		System.out.println("maxVal = " + bestSol);
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Time = "+(double)totalTime/(double)1000+" seg");		

	}
}
 