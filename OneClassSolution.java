import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Niebisch Markus on 11.11.2019.
 */
public class OneClassSolution {
    static final Logger logger = Logger.getLogger(OneClassSolution.class.getName());

    private final int numberCandidates;
    private final int numberVoters;

    private final int[][] preferenceMatrix;

    private int manipulatingIndex = -1;
    private int[][] preferenceMatrixManipulated = null;

    private final int maximumBrutforceNumber = 1<<28; // Increased this a bit, algo is fast enough

    public OneClassSolution(int numberCandidates, int numberVoters, int[][] preferenceMatrix, boolean print) {
        this.numberCandidates = numberCandidates;
        this.numberVoters = numberVoters;

        if(preferenceMatrix == null)
        {
            this.preferenceMatrix = new int[numberVoters][numberCandidates]; //i chose this matrix, so its easier to replace a voting vector
            this.fillPreferencesRandomly(this.preferenceMatrix);
        }
        else
        {
            this.preferenceMatrix = preferenceMatrix;
        }
        if (print)printPreferenceMatrix(this.preferenceMatrix);
    }

    /*private void playerManinipulation(int[][] preferenceMatrix, int[] votes, int indexPlayer, VotingVectors vv) {

        // #### Old code ?

        int[][] workingCopyPreferenceMatrix = deepCopy(this.preferenceMatrix);
        int[] workingCopyVotes = votes.clone();

        int numberCandidates = getNumberCandidates(preferenceMatrix);
        int numberVoters = getNumberVoters(preferenceMatrix);
        int[] votingVectorArray = vv.getVotingVector(numberCandidates);
        addVotes(workingCopyVotes ,votingVectorArray,preferenceMatrix[indexPlayer],true);

        int numberOfPermutations =  factorial(numberCandidates);//brute force it
        if (numberOfPermutations>= maximumBrutforceNumber)
            System.out.println("Probably not good as it tries: " + numberOfPermutations + " permutations");

        // get all permutations
        List<int[]> permutations = new ArrayList<>();
        int[] preference = getVoterPreference(preferenceMatrix, indexPlayer);
        permute(preference, preference.length, permutations);

        int minRegret = 1000;
        int[] best = null;

        for (int[] permutation : permutations)
        {
            int[] removedManipulatingPlayerVotes = workingCopyVotes.clone();
            addVotes(removedManipulatingPlayerVotes, votingVectorArray, permutation, false);
            Integer[] order = getCandidatesOrder(removedManipulatingPlayerVotes);
            int regret = getHappinessOfVoter(preferenceMatrix[indexPlayer], order);

            if(regret < minRegret)
            {
                minRegret = regret;
                best = permutation;
            }
        }

        System.out.println("Manipulated regret: " + minRegret);
        System.out.println("Manipulated votes: " + Arrays.toString(best));
        System.out.println("Real preference: " + Arrays.toString(preferenceMatrix[indexPlayer]));
    }*/

    // ### Public functions ###

    public int[] playerManipulation(int indexPlayer, VotingVectors vv,boolean print) {
        int[] votes = evaluate(preferenceMatrix, vv);
        addVotes(votes, vv.getVotingVector(numberCandidates), preferenceMatrix[indexPlayer],true);

        int numberOfPermutations =  factorial(numberCandidates); //brute force it
        if (numberOfPermutations >= maximumBrutforceNumber)
            System.out.println("Probably not good as it tries: " + numberOfPermutations + " permutations");

        // get all permutations of player who wants to manipulate
        List<int[]> permutations = new ArrayList<>();
        int[] preference = getVoterPreference(preferenceMatrix, indexPlayer);
        permute(preference, preference.length, permutations);

        double maxHappy = -10000;
        int[] best = null;

        for (int[] permutation : permutations)
        {
            int[] votesCopy = votes.clone();
            addVotes(votesCopy, vv.getVotingVector(numberCandidates), permutation, false);
            Integer[] order = getCandidatesOrder(votesCopy);
            double happy = getHappinessOfVoter(preferenceMatrix[indexPlayer], order);

            if(happy  > maxHappy)
            {
                maxHappy = happy;
                best = permutation;
            }
        }

        if (print)System.out.println(Integer.toString(permutations.size()) + " permutations checked");

        return best;
    }

    public void setManipulation(int indexPlayer, int[] manipulatedPreference) {
        preferenceMatrixManipulated = deepCopy(preferenceMatrix);
        manipulatingIndex = indexPlayer;
        preferenceMatrixManipulated[indexPlayer] = manipulatedPreference;
    }
    public void resetManipulation() {
        manipulatingIndex = -1;
        preferenceMatrixManipulated = null;
    }

    public void printAll(VotingVectors vv){
        int[] votes = evaluate(preferenceMatrix, vv);
        Integer[] order = getCandidatesOrder(votes);
        int[] votesManipulated = null;
        Integer[] orderManipulated = null;
        if(manipulatingIndex != -1)
        {
            votesManipulated = evaluate(preferenceMatrixManipulated, vv);
            orderManipulated = getCandidatesOrder(votesManipulated);

            System.out.print("Voter " + String.format("%2d",manipulatingIndex) + " honest:         " );
            for (int i = 0; i < preferenceMatrix[manipulatingIndex].length; i++) {
                System.out.print(String.format("%2d",preferenceMatrix[manipulatingIndex][i]) + " ");
            }
            System.out.println();

            System.out.print("Voter " + String.format("%2d",manipulatingIndex) + " manipulated:    " );
            for (int i = 0; i < preferenceMatrixManipulated[manipulatingIndex].length; i++) {
                System.out.print(String.format("%2d",preferenceMatrixManipulated[manipulatingIndex][i]) + " ");
            }
            System.out.println();
            System.out.println();
        }

        printVotes(votes, votesManipulated);
        printCandidateOrder(order, orderManipulated);
        System.out.println();
    }

    // ### Private functions, mostly static to show that they only use input parameters ###

    private static int getNumberVoters(int[][] preferenceMatrix) {
        return preferenceMatrix.length;
    }
    private static int getNumberCandidates(int[][] preferenceMatrix) {
        return preferenceMatrix[0].length;
    }

    private void printCandidateOrder(Integer[] order, Integer[] orderManipulated) {

        System.out.print("Honest Ranking:          " );
        for (int i = 0; i < numberCandidates; i++) {
            System.out.print(String.format("%2d",order[i]) + " ");
        }
        System.out.println();
        if(orderManipulated != null)
        {
            System.out.print("Manipulated Ranking:     ");
            for (int i = 0; i < numberCandidates; i++) {
                System.out.print(String.format("%2d",orderManipulated[i]) + " ");
            }
            System.out.println();
        }
        System.out.println();

        System.out.println("Regret score:");
        System.out.print("Candidate:               " ); // replace the candidate with spaces
        for (int i = 0; i < numberVoters; i++) {
            System.out.print(String.format("%2d",i) + "     ");
        }
        System.out.println();
        System.out.print("______________________" + "__"); // replace the candidate with spaces
        for (int i = 0; i < numberVoters; i++) {
            System.out.print("___");
        }
        System.out.println();
        System.out.print("Honest happiness:         ");
        for(int i = 0; i < numberVoters; i++)
        {
            double happinessOfVoter = getHappinessOfVoter(getVoterPreference(preferenceMatrix,i),order);
            System.out.print(String.format("%.4f", happinessOfVoter) + " ");
        }
        System.out.println();
        if(orderManipulated != null)
        {
            System.out.print("Manipulated happiness:    ");
            for (int i = 0; i < numberVoters; i++) {
                double happinessOfVoter = getHappinessOfVoter(getVoterPreference(preferenceMatrix, i), orderManipulated);
                System.out.print(String.format("%.4f", happinessOfVoter) + " ");
            }
            System.out.println();
        }
        System.out.println();

        /*
        for (int indexVoter = 0; indexVoter < getNumberVoters(preferenceMatrix); indexVoter++) {
           int regretOfVoter = getHappinessOfVoter(getVoterPreference(preferenceMatrix,indexVoter),order);
            System.out.print("Voter" + indexVoter + " has an regret of " + regretOfVoter);
            System.out.print(" with a preference vector of: ");
            int[] pref = getVoterPreference(preferenceMatrix,indexVoter);
            for (int i = 0; i < pref.length; i++) {
                System.out.print(pref[i] + " ");
            }
            System.out.println();

        }*/
    }

    private static double getHappinessOfVoter(int[] voterPreference, Integer[] order) {
        int n = voterPreference.length;
        int[] preferenceIndecies = getPreferenceInecies(voterPreference); //this one tells for every candidate which place it is
        int totalDistance = 0;
        for (int i = 0; i < n; i++) {
            int reality = order[i];
            int prefLocation = preferenceIndecies[reality];

            totalDistance += (n-prefLocation)*(i-prefLocation);
        }
        double happy = 1./(1.+Math.abs(totalDistance));
        return happy;
    }

    private static int[] getPreferenceInecies(int[] voterPreference) {
        int n = voterPreference.length;
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[voterPreference[i]]=i;
        }
        return arr;
    }

    private static int[] getVoterPreference(int[][] preferenceMatrix, int indexVoter) {
        return preferenceMatrix[indexVoter];
    }

    private static Integer[] getCandidatesOrder(int[] votes) {
        int num = votes.length;
        Integer[] order = new Integer[num];
        for (int i = 0; i < num; i++) {
            order[i] = i;
        }
        Arrays.sort(order, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                int votesFirst = votes[o1];
                int votesSecond = votes[o2];
                if (votesFirst >votesSecond)return -1;
                if (votesFirst <votesSecond)return 1;
                return 0;
            }
        });
        return order;
    }

    private void printVotes(int[] votes, int[] votesManipulated) {
        int size = votes.length;
        System.out.println("Value per candidate:");
        System.out.print("Candidate:               " ); // replace the candidate with spaces
        for (int i = 0; i < size; i++) {
            System.out.print(String.format("%2d",i) + " ");
        }
        System.out.println();
        System.out.print("______________________" + "__"); // replace the candidate with spaces
        for (int i = 0; i < size; i++) {
            System.out.print("___");
        }
        System.out.println();
        System.out.print("Honest:                  ");
        for (int i = 0; i < size; i++) {
            System.out.print(String.format("%2d",votes[i])+ " ");
        }
        if(votesManipulated != null)
        {
            System.out.println();
            System.out.print("Manipulated by voter " + String.format("%2d", manipulatingIndex) + ": ");
            for (int i = 0; i < size; i++) {
                System.out.print(String.format("%2d",votesManipulated[i])+ " ");
            }
        }
        System.out.println();
        System.out.println();

    }

    private static int[] evaluate(int[][] preferenceMatrix, VotingVectors votingVector) {
        int sizeVoters = preferenceMatrix.length;
        int sizeCandidates = preferenceMatrix[0].length;
        int[] votes = new int[sizeCandidates];
        int[] votingVectorArray = votingVector.getVotingVector(sizeCandidates);
        for (int voterIndex = 0; voterIndex < sizeVoters; voterIndex++) {
            addVotes(votes,votingVectorArray,preferenceMatrix[voterIndex],false);
        }
        return votes;
    }

    private static void addVotes(int[] votes, int[] votingVector, int[] preferenceVectorOfVoter, boolean removeVotesInstead) {
        int numberOfCandidates = votes.length;
        int sign = 1; if (removeVotesInstead)sign = -1;
        for (int j = 0; j < numberOfCandidates; j++) {
            votes[preferenceVectorOfVoter[j]] += votingVector[j] * sign;
        }

    }

    private static void printPreferenceMatrix(int[][] preferenceMatrix) {
        System.out.println("Preference Matrix: (lower number is better)");
        System.out.println();

        int size = preferenceMatrix.length;
        int size2 = preferenceMatrix[0].length;
        System.out.print("Voter:   " + "  "+ "   "); // replace the candidate with spaces
        for (int i = 0; i < size; i++) {
            System.out.print(String.format("%2d",i) + " ");
        }
        System.out.println();
        System.out.print("_________" + "__"+ "__"); // replace the candidate with spaces
        for (int i = 0; i < size; i++) {
            System.out.print("___");
        }
        System.out.println();

        for (int j = 0; j < size2; j++) {
            System.out.print("Preference" + String.format("%2d",j)+ ": ");
            for (int i = 0; i < size; i++) {
                System.out.print(String.format("%2d", preferenceMatrix[i][j]) + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    private static void fillPreferencesRandomly(int[][] preferenceMatrix) {
        int size = preferenceMatrix.length;
        int size2 = preferenceMatrix[0].length;

        for (int i = 0; i < size; i++) {
            preferenceMatrix[i] = getPreferenceVector(size2);
        }
    }
    private static int[] getPreferenceVector(int size2) {
        int[] w = new int[size2];
        ArrayList<Integer> choiseBag = new ArrayList<>(size2);

        for (int i = 0; i < size2; i++) {
            choiseBag.add(i);
        }

        Random r = new Random();
        int count = 0;
        while (choiseBag.size()!=0){
            int chosenIndex = r.nextInt(choiseBag.size());
            int choise = choiseBag.get(chosenIndex);
            w[count++] = choise;
            choiseBag.set(chosenIndex, choiseBag.get(choiseBag.size()-1));
            choiseBag.remove(choiseBag.size()-1);//remove the last, to avoid array copies.
        }

        return w;
    }

    // ### All helper functions ###

    public static int factorial(int numberCandidates) {
        int mult = numberCandidates;
        for (int i = 2; i <= numberCandidates; i++) {
            mult*=i;
        }
        return mult;
    }
    public static int[][] loadPreferenceMatrix(String filename) {
        BufferedReader csvReader = null;
        try
        {
            csvReader = new BufferedReader(new FileReader(filename));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        int[][] matrix = null;
        boolean first = true;
        int index = 0;

        while (true)
        {
            String row = null;
            try
            {
                if (!((row = csvReader.readLine()) != null)) break;
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            String[] data = row.split(",");
            if(first)
            {
                first = false;
                matrix = new int[Integer.parseInt(data[0])][Integer.parseInt(data[1])];
            }
            else
            {
                for(int i = 0; i < data.length; i++)
                {
                    matrix[i][index] = Integer.parseInt(data[i]);
                }
                index++;
            }
        }

        try
        {
            csvReader.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return matrix;
    }
    public static int[][] deepCopy(int[][] preferenceMatrix) {
        int size = preferenceMatrix.length;
        int[][] preferenceMatrix2 = preferenceMatrix.clone();
        for (int i = 0; i < size; i++) {
            preferenceMatrix2[i] = preferenceMatrix[i].clone();
        }
        return preferenceMatrix2;
    }
    public static void permute(int[] a, int n, List<int[]> result) {
        if (n == 1) {
            result.add(a.clone());
            return;
        }

        int t;
        for (int i = 0; i < n; i++) {
            t = a[i];
            a[i] = a[n-1];
            a[n-1] = t;

            permute(a, n-1, result);

            t = a[i];
            a[i] = a[n-1];
            a[n-1] = t;
        }
    }

    public static void main(String[] args) {
        if (false){
            experiment();
            return;
        }
        int numberVoters = 13;
        int numberCandidates = 8;
        int evilVoter = 2;
        int[][] preferenceMatrix = null;

        if(args.length == 1)
        {
            preferenceMatrix = loadPreferenceMatrix(args[0]);
            numberVoters = preferenceMatrix.length;
            numberCandidates = preferenceMatrix[0].length;
            System.out.println("Matrix loaded with " + Integer.toString(numberVoters) + " voters and " + Integer.toString(numberCandidates) + " candidates.");
        }

        OneClassSolution ocs = new OneClassSolution(numberCandidates, numberVoters, preferenceMatrix,true);

        for (VotingVectors vv: VotingVectors.values())
        {
            System.out.println("### " + vv.toString() + " ###");
            double happyOriginal = getHappinessOfVoter(ocs.preferenceMatrix,vv,ocs.preferenceMatrix[evilVoter]);
            int[] manipulatedPreference = ocs.playerManipulation(evilVoter, vv,true);
            ocs.setManipulation(evilVoter, manipulatedPreference);
            ocs.printAll(vv);
            double happyManipulated = getHappinessOfVoter(ocs.preferenceMatrixManipulated,vv,ocs.preferenceMatrix[evilVoter]);
            double delta = happyManipulated-happyOriginal;
            System.out.println("Original happiness:" +happyOriginal);
            System.out.println("Manipulated happiness:" +happyManipulated);
            System.out.println("The voter " + evilVoter + " improved his happiness by " + delta);
            System.out.println();
        }
    }

    private static void experiment() {
        int minNumberVoters = 2;
        int increaseIncrementNumberVoters = 20;
        int maxNumberVoters = 2000;
        int minNumberCandidates = 2;
        int maxNumberCandidates = 9;
        int runs = 30;
        int evilVoter = 1;
        int[][] preferenceMatrix = null;

        for (VotingVectors vv: VotingVectors.values())
        {
            if (!vv.equals(VotingVectors.AntiPluralityVoting))continue;
        for (int numberCandidates = minNumberCandidates; numberCandidates <= maxNumberCandidates ; numberCandidates++) {
            for (int numberVoters = minNumberVoters; numberVoters <= maxNumberVoters; numberVoters = increase(numberVoters,increaseIncrementNumberVoters)) {

                ArrayList<Double> deltas = new ArrayList<>();
                for (int i = 0; i <runs; i++) {
                    OneClassSolution ocs = new OneClassSolution(numberCandidates, numberVoters, preferenceMatrix,false);
                    double happyOriginal = getHappinessOfVoter(ocs.preferenceMatrix,vv,ocs.preferenceMatrix[evilVoter]);
                    int[] manipulatedPreference = ocs.playerManipulation(evilVoter, vv,false);
                    ocs.setManipulation(evilVoter, manipulatedPreference);
                    //ocs.printAll(vv);
                    double happyManipulated = getHappinessOfVoter(ocs.preferenceMatrixManipulated,vv,ocs.preferenceMatrix[evilVoter]);
                    double delta = happyManipulated-happyOriginal;
                    deltas.add(delta);
                }
                System.out.print(vv + ": " + "numberCandidates: " + numberCandidates + " numberVoters: " + numberVoters);
                printOut(deltas,false);
            }
        }
        }

    }

    private static int increase(int numberVoters, int increaseIncrementNumberVoters) {
        if (numberVoters<increaseIncrementNumberVoters)return numberVoters+1;
        return numberVoters*2;
    }

    private static void printOut(ArrayList<Double> deltas, boolean longPrint) {
        double sum = 0;
        double best = Double.NEGATIVE_INFINITY;
        if (longPrint)System.out.println();
        for (Double d :
                deltas) {
            if (longPrint)System.out.print(d + ",");
            if (d>best) best = d;
            sum +=d;
        }
        Collections.sort(deltas);
        double avg = sum/deltas.size();
        if (longPrint)System.out.println();
        if (longPrint)System.out.println("avg improvement: " + avg);
        double median = deltas.get(deltas.size()/2);
        if (longPrint)System.out.println("median improvement: " + median);
        if (longPrint)System.out.println("best improvement: " + best);
        if (!longPrint)System.out.print(" avg: " + avg + " med: " + median + " bst: " + best);
        System.out.println();
    }

    private static double getHappinessOfVoter(int[][] preferenceMatrix, VotingVectors vv, int[] honestPreference) {
        int[] votes = evaluate(preferenceMatrix, vv);
        Integer[] order = getCandidatesOrder(votes);

        double happy = getHappinessOfVoter(honestPreference, order);
        return happy;
    }

    private enum VotingVectors {
        PluralityVoting(){
            private int storedN = 0;
            private int[] vec = {1};

            @Override
            int[] getVotingVector(int n) {
                if (n == storedN) return vec;
                storedN = n;
                vec = new int[n];
                vec[0] = 1;
                return vec;
            }
        },VotingForTwo {
            private int storedN;
            private int[] vec;
            @Override
            int[] getVotingVector(int n) {
                if (n == storedN) return vec;
                storedN = n;
                vec = new int[n];
                vec[0] = 1;
                vec[1] = 1;
                return vec;
            }
        },AntiPluralityVoting {
            private int storedN;
            private int[] vec;
            @Override
            int[] getVotingVector(int n) {
                if (n == storedN) return vec;
                storedN = n;
                vec = new int[n];
                vec[n-1] = -1; //Probably wrong, but from the slides

                return vec;
            }
        },BordaVoting {
            private int storedN;
            private int[] vec;

            @Override
            int[] getVotingVector(int n) {
                if (n == storedN) return vec;
                storedN = n;
                vec = new int[n];
                int counter = n-1;
                for (int i = 0; i < n; i++) {
                    vec[i] = counter;
                    counter--;
                }
                return vec;
            }

        };



        abstract int[] getVotingVector(int n);

    }
}
