import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Created by Niebisch Markus on 11.12.2019.
 */
public class OnceBrassCollusion {
    static final Logger logger = Logger.getLogger(OnceBrassCollusion.class.getName());
    public static Random rng;

    public static void main(String[] args){
        OnceBrassCollusion.setRandomNumberGenerator(new Random(0));
        OnceBrassCollusion obc = new OnceBrassCollusion();

        obc.doAllExperiments();
    }

    private void doAllExperiments() {
        int minK = 6;
        int maxK = 12;
        int minN = 6;
        int maxN = 100;
        int threasholdNIncrement = 12;
        double incrementalFactorN = 1.5;
        int minR = 4;
        int maxR = 10;

        double sMax = 1000;
        double e = 1.;
        boolean pure = false;



        for (int k = minK; k <= maxK; k++) {
            for (int n = minN; n <= maxN; n = increase(n, threasholdNIncrement, incrementalFactorN)) {
                for (int r = minR; r < maxR; r = increase(r, 1, 1.5)) {
                    BiddingStrategy[] bs = BiddingStrategy.getUniformSelection(k);
                    BiddingStrategy[] bs2 = BiddingStrategy.getAllOfOneType(BiddingStrategy.Version1,k);
                    BiddingStrategy[] bs3 = BiddingStrategy.compareTwoStrategies(BiddingStrategy.Version1,BiddingStrategy.Version2,k);

                    Simulation s = new Simulation(bs,n,r,sMax,e,pure);
                    s.simulate();
                    s.printout();
                }
            }

        }
    }


    private static int increase(int n, int threasholdForIncreasedIncrement, double incrementalFactor) {
        if (n<threasholdForIncreasedIncrement)return n+1;
        return (int) Math.round(n*incrementalFactor);
    }

    public static void setRandomNumberGenerator(Random randomNumberGenerator) {
        rng = randomNumberGenerator;
    }


    private class Simulation {
        private final double sMax;
        private final BiddingStrategy[] bss;
        private final int r;
        private final int n;
        double[][] profitSellerInRoundR;
        double[][] profitBuyerInRoundR;

        public Simulation(BiddingStrategy[] bs, int n, int r, double sMax, double e, boolean pure) {
            this.sMax = sMax;
            this.bss = bs;
            this.n = n;
            profitSellerInRoundR = new double[r][n];
            profitBuyerInRoundR = new double[r][bs.length];
            this.r = r;

        }

        public void simulate() {
            for (int r_i = 0; r_i < r; r_i++) { //for each round

                double[] stuffToBuyStartingPrice = createStartingPrices(n,sMax);
                double[][] bidsPerItem = recieveBidsPerItem(stuffToBuyStartingPrice,bss,r_i);
                double[] markedPrices = calcMarketPrices(bidsPerItem);

                double[] profitSellerInCurrentRound = profitSellerInRoundR[r_i];
                double[] profitBuyerInCurrentRound = profitBuyerInRoundR[r_i];
                determineWinnerAndCalculateProfit(markedPrices,bidsPerItem,profitBuyerInCurrentRound,profitSellerInCurrentRound);
            }

        }



        private Integer[] getBiddingsOrder(double[] bids) {
            int num = bids.length;
            Integer[] order = new Integer[num];
            for (int i = 0; i < num; i++) {
                order[i] = i;
            }
            Arrays.sort(order, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    double bidsFirst = bids[o1];
                    double bidsSecond = bids[o2];
                    if (bidsFirst >bidsSecond)return -1;
                    if (bidsFirst <bidsSecond)return 1;
                    return 0;
                }
            });
            return order;
        }

        private void determineWinnerAndCalculateProfit(double[] markedPrices, double[][] bidsPerItem, double[] profitBuyerInCurrentRound, double[] profitSellerInCurrentRound) {
            int numberBuyers = bidsPerItem[0].length;
            boolean[] hasPlayerWonInThisRound = new boolean[numberBuyers];
            int numberOfSellers = markedPrices.length;

            //determine winner for one round
            for (int i = 0; i < numberOfSellers; i++) {
                double[] bidsOfParticularItem = bidsPerItem[i];
                double marketPriceOfParticularItem = markedPrices[i];

                //get a ranking of buyers determined by bid
                Integer[] ranking = getBiddingsOrder(bidsOfParticularItem);

                //iterate through the ranking until winner is reached
                int indexOfWinnerInRanking = 0;
                while (bidsOfParticularItem[ranking[indexOfWinnerInRanking]] > marketPriceOfParticularItem ||
                    hasPlayerWonInThisRound[indexOfWinnerInRanking])indexOfWinnerInRanking++;
                //we have a winner
                double payPrice = bidsOfParticularItem[ranking[indexOfWinnerInRanking+1]];
                hasPlayerWonInThisRound[indexOfWinnerInRanking] = true;
                profitSellerInCurrentRound[i]+=payPrice;
                double profitBuyer = marketPriceOfParticularItem-payPrice;

                assert (profitBuyerInCurrentRound[indexOfWinnerInRanking]<0.0000001);

                profitBuyerInCurrentRound[i]=profitBuyer;
            }


        }

        private double[] calcMarketPrices(double[][] bidsPerItem) {
            double[] markedPrices = new double[bidsPerItem.length];
            int counter = 0;
            for (double[] bidsForParticularItem: bidsPerItem) {
                double sum = 0;
                for (double bids:bidsForParticularItem){
                    sum+= bids;
                }
                sum/=bidsForParticularItem.length;
                markedPrices[counter] = sum;
                counter++;
            }
            return markedPrices;
        }

        /**
         *
         * @param stuffToBuyStartingPrice
         * @param bss the bidding strategies
         * @param r curent round
         * @return an array which contains for every item a bid of every buyer
         */
        private double[][] recieveBidsPerItem(double[] stuffToBuyStartingPrice, BiddingStrategy[] bss,int r) {
            int k = stuffToBuyStartingPrice.length;
            int n = bss.length;

            double[][] bidsPerItem = new double[k][n];
            for (int k_i = 0; k_i < k; k_i++) {
                double startingPrice = stuffToBuyStartingPrice[k_i];
                double[] bidsForParticularItem = bidsPerItem[k_i];
                for (int n_i = 0; n_i < n; n_i++) {
                    bidsForParticularItem[n_i] = bss[n_i].bid(profitSellerInRoundR,profitBuyerInRoundR,r,startingPrice);
                }

            }
            return bidsPerItem;
        }

        private double[] createStartingPrices(int n, double sMax) {
            return new double[0];
        }

        public void printout() {

        }
    }

    private enum BiddingStrategy {
        Version1{
            @Override
            public double bid(double[][] profitSellerInRoundR, double[][] profitBuyerInRoundR, int r, double startingPrice) {
                return 0;
            }
        },Version2{
            @Override
            public double bid(double[][] profitSellerInRoundR, double[][] profitBuyerInRoundR, int r, double startingPrice) {
                return 0;
            }
        },Version3{
            @Override
            public double bid(double[][] profitSellerInRoundR, double[][] profitBuyerInRoundR, int r, double startingPrice) {
                return 0;
            }
        };

        public static BiddingStrategy[] compareTwoStrategies(BiddingStrategy b1,BiddingStrategy b2, int k){
            BiddingStrategy[] bsa = new BiddingStrategy[k];
            boolean flip =true;
            for (int i = 0; i < k; i++) {
                bsa[i] = flip?b1:b2;
                flip = !flip;
            }
            return bsa;
        }
        public static BiddingStrategy[] getAllOfOneType(BiddingStrategy bs, int k){
            BiddingStrategy[] bsa = new BiddingStrategy[k];
            for (int i = 0; i < k; i++) {
                bsa[i] = bs;
            }
            return bsa;
        }
        public static BiddingStrategy[] getUniformSelection(int k) {
            BiddingStrategy[] bs = new BiddingStrategy[k];
            for (int i = 0; i < k; i++) {
                bs[i] = BiddingStrategy.values()[rng.nextInt(BiddingStrategy.values().length)];
            }
            return bs;
        }

        public abstract double bid(double[][] profitSellerInRoundR, double[][] profitBuyerInRoundR, int r, double startingPrice);
    }


}
