/*
 * Author: Nevin George
 * Advisor: Dana Angluin
 * Program Description: The program takes in as input a mod-2-MA or SUBA and prints to stdout the mod-2-MA obtained after 
 * minimizing the input function (in the SUBA case, it first converts the function into an equivalent UFA then mod-2-MA).
 * 
 * References:
 * 1 Amos Beimel, Francesco Bergadano, Nader H. Bshouty, Eyal Kushilevitz, Stefano Varric- chio. Learning 
 *   functions represented as multiplicity automata. J. ACM, 47(3):506–530, May 2000.
 * 2 Dana Angluin. Learning regular sets from queries and counterexamples. Inf. Comput., 75(2):87–106, 1987.
 * 3 Dana Angluin, Timos Antonopoulos, Dana Fis`man. Strongly Unambiguous Büchi Automata Are Polynomially 
 *   Predictable with Membership Queries. 28th International Conference on Computer Science Logic, 8:1–8:17, 2020.
 * 4 Michael Thon and Herbert Jaeger. Links Between Multiplicity Automata, Observable Operator Models and 
 *   Predictive State Representations — a Unified Learning Framework. Journal of Machine Learning Research, 
 *   16(4):103−147, 2015.
 */

import java.util.Scanner;

public class minimize {

	public static void main(String[] args) throws Exception {
		int automataToMinimize = readInput();
		Mod2_MA.inMinimize = true;
		
		// If == 1, then the input is a mod-2-MA; if == 2, then the input is a SUBA.
		if (automataToMinimize == 1) {
			Mod2_MA.readInput();
		} else {
			SUBA.SUBAtoUFA();
			SUBA.UFAtoMod2MA();
		}
		
		Mod2_MA.minimize();
		
		Mod2_MA.in.close();
		
		if (Mod2_MA.finalCheck(25, 1000, true)) {
			displayResults();
		} else {
			throw new Exception("Algorithm failed: failed final check.");
		}
		
		Mod2_MA.displayRuntime();
	}
	
	public static int readInput() {
		Mod2_MA.in = new Scanner(System.in);
		int automataToMinimize;
		
		while (true) {
			try {
				System.out.println("Enter 1 to minimize a mod-2-MA and 2 to mimimize a SUBA.");
				automataToMinimize = Integer.parseInt(Mod2_MA.in.nextLine());
				if (automataToMinimize != 1 && automataToMinimize != 2) {
					throw new Exception("Invalid input.");
				}
				break;
			} catch (Exception e) {
				System.out.println("Invalid input.");
			}
		}
		
		Mod2_MA.startTime = System.nanoTime();
		
		return automataToMinimize;
	}
	
	public static void displayResults() {
		System.out.println("Minimized mod-2-MA");
		System.out.println("----------------");
		
		System.out.println("Dimension: " + Mod2_MA.minSize + '\n');
		
		System.out.print("Final Vector: ");
		Mod2_MA.displayMatrix(Mod2_MA.minFinalVector);
		
		System.out.println("Transition Matrices:\n");
		for (int i=0; i<Mod2_MA.minTransitionMatrices.length; i++) {
			System.out.println("Letter " + Mod2_MA.alphabet[i]);
			Mod2_MA.displayMatrix(Mod2_MA.minTransitionMatrices[i]);
		}
	}
}
