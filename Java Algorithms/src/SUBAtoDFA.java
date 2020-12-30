import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.math3.linear.DecompositionSolver;

public class SUBAtoDFA {
	
	public static void main(String[] args) throws Exception {
		System.out.println("Input file name.");
		Scanner in = new Scanner(System.in);
		BufferedReader f = new BufferedReader(new FileReader(in.nextLine()));
		System.out.println("SUBA Dimension, Minimized M2MA Dimension, Minimal DFA Size");
		
		int numSUBA = Integer.parseInt(M2MA.readFile(f));
		// int[0] = sum of M2MA dimension, int[1] = count of M2MA dimension
		// int[2] = sum od DFA dimension, int[2] = count of DFA dimension
		HashMap<Integer, int[]> totalDimensions = new HashMap<Integer, int[]>();
		
		for (int i = 0; i < numSUBA; i++) {
			readInput(f);
			SUBA.UFAtoMod2MA();
			int[] minimizedDimensions = minimization();
			System.out.println(SUBA.SUBAStates + " " + minimizedDimensions[0] + " " + minimizedDimensions[1]);
			
			if (totalDimensions.get(SUBA.SUBAStates) == null) {
				int[] results = new int[3];
				results[0] = minimizedDimensions[0];
				results[1] = minimizedDimensions[1];
				results[2] = 1;
				totalDimensions.put(SUBA.SUBAStates, results);
			} else {
				totalDimensions.get(SUBA.SUBAStates)[0] += minimizedDimensions[0];
				totalDimensions.get(SUBA.SUBAStates)[1] += minimizedDimensions[1];
				totalDimensions.get(SUBA.SUBAStates)[2] += 1;
			}
		}
		
		System.out.println("\nAggregate Results");
		System.out.println("SUBA States, Total M2MA Dimension, Total DFA Size, Number of Automata, Average M2MA Dimension, Average DFA Size");
		Set<Integer> keySet = totalDimensions.keySet();
		
		for (int subaStates : keySet) {
			int[] results = totalDimensions.get(subaStates);
			System.out.println(subaStates + " " + results[0] + " " + results[1] + " " + results[2] + " " + (((double) results[0])/results[2]) + " " + ((double) results[1])/results[2]);
		}
		
		in.close();
	}
	
	@SuppressWarnings("unchecked")
	public static void readInput(BufferedReader f) throws Exception {
		// UFAStates = SUBAStates U (SUBAStates x SUBAStates x {0,1})
		SUBA.SUBAStates = Integer.parseInt(M2MA.readFile(f));
		SUBA.UFAStates = SUBA.SUBAStates + SUBA.SUBAStates * SUBA.SUBAStates * 2;
		
		// alphabet ΣU{$}
		StringTokenizer st = new StringTokenizer(M2MA.readFile(f));
		ArrayList<String> tempAlphabet = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			String letter = st.nextToken();
			if(letter.equals("$")) {
				M2MA.throwException(f, "Invalid input: invalid character in the alphabet.");
			}
			tempAlphabet.add(letter);
		}
		tempAlphabet.add("$");
		M2MA.alphabet = new String[tempAlphabet.size()];
		for (int i=0; i<tempAlphabet.size(); i++) {
			M2MA.alphabet[i] = tempAlphabet.get(i);
		}
		
		// map each letter in alphabet to an index
		M2MA.letterToIndex = new HashMap<String, Integer>();
		for (int i=0; i<M2MA.alphabet.length; i++) {
			M2MA.letterToIndex.put(M2MA.alphabet[i], i);
		}
		
		st = new StringTokenizer(M2MA.readFile(f));
		SUBA.SUBAFinalStates = new boolean[SUBA.SUBAStates+1];
		while (st.hasMoreTokens()) {
			int state = Integer.parseInt(st.nextToken());
			if (1 <= state && state <= SUBA.SUBAStates && !SUBA.SUBAFinalStates[state]) {
				SUBA.SUBAFinalStates[state] = true;
			} else {
				M2MA.throwException(f,"Invalid input: invalid or duplicate final state.");
			}
		}
		
		/* 
		 * Following the paper by Bousquet and Löding, UFATransitions contains (where q,p,p'∈SUBAStates)
		 * - SUBATransitions
		 * - all transitions of the form (q,$,(q,q,0))
		 * - all transitions of the form ((q,p,i),a,(q,p',i')), where (p,a,p')∈SUBATransitions, and
		 * 	 i' = 1 if p'∈SUBAFinalStates and i if p'∉SUBAFinalStates
		 * 
		 * Transitions will be stored in a (UFAStates x alphabetSize x UFAStates) adjacency matrix.
		 * The first states of UFATransitions will be SUBAStates.
		 * The remaining states will be of the form (q_j,q_k,i), where q_j,q_k∈SUBAStates and i∈{0,1}.
		 * State (q_j,q_k,i) will be found at index (2*SUBAStates*j)+(2*k)-(SUBAStates)+(i-1) of UFATransitions.
		*/
		int numTransitions = Integer.parseInt(M2MA.readFile(f));
		if (numTransitions > ((M2MA.alphabet.length - 1) * SUBA.SUBAStates * SUBA.SUBAStates)) {
			M2MA.throwException(f, "Invalid input: invalid number of transitions.");
		}
		
		/*
		 *  For each index (q,a) where q∈SUBAStates and a∈alphabet, transition_SUBA[q][a] is an ArrayList containing 
		 *  all of the reachable states from (q,a).
		 *  The alphabet for the SUBA does not include $.
		 */
		SUBA.SUBATransitions = new ArrayList[SUBA.SUBAStates + 1][M2MA.alphabet.length - 1];
		for (int i=1; i<=SUBA.SUBAStates; i++) {
			for (int j=0; j<M2MA.alphabet.length-1; j++) {
				SUBA.SUBATransitions[i][j] = new ArrayList<Integer>();
			}
		}
		
		// (start state, letter, end state)
		SUBA.UFATransitions = new boolean[SUBA.UFAStates + 1][M2MA.alphabet.length][SUBA.UFAStates + 1];
		
		// lines of the form q_j a q_k, where q_j,q_k∈SUBAStates and a∈alphabet
		for (int i=0; i<numTransitions; i++) {
			st = new StringTokenizer(M2MA.readFile(f));
			int p_start = Integer.parseInt(st.nextToken());
			
			String letter = st.nextToken();
			
			int a = M2MA.letterToIndex.get(letter);
			int p_end = Integer.parseInt(st.nextToken());
			if (p_start < 1 || p_start > SUBA.SUBAStates || p_end < 1 || p_end > SUBA.SUBAStates) {
				M2MA.throwException(f, "Invalid input: invalid transition.");
			}
			
			// SUBATransitions ⊆ UFATransitions 
			SUBA.SUBATransitions[p_start][a].add(p_end);
			SUBA.UFATransitions[p_start][a][p_end] = true;
			
			// transitions of the form ((q,p,i),a,(q,p',i'))
			// p'∈SUBAFinalStates so i'=1
			if (SUBA.SUBAFinalStates[p_end]) {
				for (int q=1; q<=SUBA.SUBAStates; q++) {
					// ((q,p,0),a,(q,p',1))
					SUBA.UFATransitions[SUBA.getIndex(q, p_start, 0)][a][SUBA.getIndex(q, p_end, 1)] = true;
					// ((q,p,1),a,(q,p',1))
					SUBA.UFATransitions[SUBA.getIndex(q, p_start, 1)][a][SUBA.getIndex(q, p_end, 1)] = true;
				}
			}
			// p'∉SUBAFinalStates so i'=i
			else {
				for (int q=1; q<=SUBA.SUBAStates; q++) {
					// ((q,p,0),a,(q,p',0))
					SUBA.UFATransitions[SUBA.getIndex(q, p_start, 0)][a][SUBA.getIndex(q, p_end, 0)] = true;
					// ((q,p,1),a,(q,p',1))
					SUBA.UFATransitions[SUBA.getIndex(q, p_start, 1)][a][SUBA.getIndex(q, p_end, 1)] = true;
				}
			}
		}
		
		// transitions for the UFA of the form (q,$,(q,q,0)), where q∈SUBAStates
		// final states for the UFA of the form (q,q,1), where q∈SUBAStates
		SUBA.UFAFinalStates = new boolean[SUBA.UFAStates+1];
		for (int q=1; q<=SUBA.SUBAStates; q++) {
			SUBA.UFATransitions[q][M2MA.letterToIndex.get("$")][SUBA.getIndex(q, q, 0)] = true;
			SUBA.UFAFinalStates[SUBA.getIndex(q, q, 1)] = true;
		}
	}

	@SuppressWarnings("unchecked")
	public static int[] minimization() throws Exception {		
		ArrayList<String> stateSpaceBasisIndices = new ArrayList<String>();
		HashMap<String, HashMap<Integer, ArrayList<Integer>>> stateSpaceIndexToVector = new HashMap<String, HashMap<Integer, ArrayList<Integer>>>();
		HashMap<Integer, ArrayList<Integer>> stateSpaceBasis = M2MA.basis(M2MA.inputFinalVector, M2MA.inputTransitionMatrices, stateSpaceIndexToVector, stateSpaceBasisIndices, true);
		
		ArrayList<String> coStateSpaceBasisIndices = new ArrayList<String>();
		HashMap<String, HashMap<Integer, ArrayList<Integer>>> coStateSpaceIndexToVector = new HashMap<String, HashMap<Integer, ArrayList<Integer>>>();
		HashMap<Integer, ArrayList<Integer>> coStateSpaceBasis = M2MA.basis(M2MA.inputFinalVector, M2MA.inputTransitionMatrices, coStateSpaceIndexToVector, coStateSpaceBasisIndices, false);

		HashMap<Integer, ArrayList<Integer>> observationTable = M2MA.multiply(stateSpaceBasis, coStateSpaceBasis);
		
		M2MA.minRowIndices = new ArrayList<String>();
		HashMap<Integer, ArrayList<Integer>> linIndRowsObservationTable = M2MA.linIndSubMatrixRows(observationTable, stateSpaceBasisIndices, M2MA.minRowIndices);
		
		M2MA.minColIndices = new ArrayList<String>();
		HashMap<Integer, ArrayList<Integer>> minObservationTable = M2MA.linIndSubMatrixCols(linIndRowsObservationTable, coStateSpaceBasisIndices, M2MA.minColIndices);
		
		M2MA.minSize = minObservationTable.get(0).get(0);
		
		int[] out = new int[2];
		out[0] = M2MA.minSize;
		
		if ((int) minObservationTable.get(0).get(0) == 1 && minObservationTable.get(1) == null) {
			M2MA.minFinalVector = M2MA.initialize(1, 1);
			M2MA.minTransitionMatrices = new HashMap[M2MA.alphabet.length];		
			for (int i=0; i<M2MA.alphabet.length; i++) {
				M2MA.minTransitionMatrices[i] = M2MA.initialize(1, 1);
			}
		}
		
		else {
			DecompositionSolver solver = new solver(M2MA.sparseToReal(minObservationTable)).getSolver();
			HashMap<Integer, ArrayList<Integer>> tableInverse = M2MA.realToSparse(solver.getInverse());
			
			M2MA.Hankel = new HashMap<String, Integer>();
			
			M2MA.minTransitionMatrices = new HashMap[M2MA.alphabet.length];
			int dim = minObservationTable.get(0).get(0);
			for (int i=0; i<M2MA.alphabet.length; i++) {	
				M2MA.minTransitionMatrices[i] = M2MA.initialize(dim, dim);
				HashMap<Integer, ArrayList<Integer>> xSigma = M2MA.initialize(dim, dim);
				
				for (int j=0; j<dim; j++) {			
					for (int k=0; k<dim; k++) {
						HashMap<Integer, ArrayList<Integer>> stateVector = stateSpaceIndexToVector.get(M2MA.minRowIndices.get(j));
						HashMap<Integer, ArrayList<Integer>> coStateVector = coStateSpaceIndexToVector.get(M2MA.minColIndices.get(k));
						
						if (M2MA.dotProduct(M2MA.multiply(stateVector, M2MA.inputTransitionMatrices[i]).get(1), coStateVector.get(-1)) == 1) {
							M2MA.addElement(xSigma, j+1, k+1);
						}
					}
				}

				M2MA.minTransitionMatrices[i] = M2MA.multiply(xSigma, tableInverse);
			}
			
			M2MA.minFinalVector = M2MA.initialize(1, minObservationTable.get(0).get(0));
			if (minObservationTable.get(-1) != null) {
				for (int num : minObservationTable.get(-1)) {
					M2MA.addElement(M2MA.minFinalVector, 1, num);
				}
			}
		}
		
		out[1] = M2MA.dimensionMinDFA(true);
		
		return out;
	}
}
