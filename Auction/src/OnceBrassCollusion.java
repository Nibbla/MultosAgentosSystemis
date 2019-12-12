import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Niebisch Markus on 11.12.2019.
 */
public class OnceBrassCollusion {
    static final Logger logger = Logger.getLogger(OnceBrassCollusion.class.getName());
    public static Random rng;

    public static void main(String[] args){
        OnceBrassCollusion.setRandomNumberGenerator(new Random(1));
        OnceBrassCollusion obc = new OnceBrassCollusion();

        obc.doAllExperiments();
    }

    private void doAllExperiments() {
        int minK = 6;
        int maxK = 6;
        int minN = 6;
        int maxN = 6;
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
                    BiddingStrategy[] bs1 = BiddingStrategy.getAllOfOneType(BiddingStrategy.Version1,k);
                    BiddingStrategy[] bs2 = BiddingStrategy.getUniformSelection(k);
                    BiddingStrategy[] bs3 = BiddingStrategy.compareTwoStrategies(BiddingStrategy.Version1,BiddingStrategy.Version2,k);

                    Simulation s = new Simulation(bs1,n,r,sMax,e,pure,true);
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
        private double[][] biddingFactor;
        private final int r;
        private final int n;
        private final boolean pure;
        private final double e; //punishment factor
        private final double[] totalProfitBuyer;
        private final double[] totalProfitSeller;
        private final boolean print;
        double[][] profitSellerInRoundR;
        double[][] profitBuyerInRoundR;
        private final double[][] marketPriceInRoundR;

        public Simulation(BiddingStrategy[] bs, int n, int r, double sMax, double e, boolean pure,boolean print) {
            this.sMax = sMax;
            this.bss = bs;
            this.n = n;
            this.print = print;
            profitSellerInRoundR = new double[r][n];
            profitBuyerInRoundR = new double[r][bs.length];
            totalProfitSeller = new double[n];
            totalProfitBuyer = new double[bs.length];
            biddingFactor = createBiddingFactor(bs.length,n); //for every buyer seller combination a bidding factor

            marketPriceInRoundR = new double[r][];

            this.r = r;
            this.pure = pure;
            this.e = e;


        }

        private double[][] createBiddingFactor(int k, int n) {
            double[][] biddinFactor = new double[k][n];
            for (int i = 0; i < k; i++) {
                for (int j = 0; j < n; j++) {
                    biddinFactor[i][j] = rng.nextDouble()*4+1; //it doesnt make sense to bid less then the starting price
                }
            }
            return biddinFactor;
        }

        public void simulate() {
            for (int r_i = 0; r_i < r; r_i++) { //for each round
                if (print) {
                    System.out.println("This shall be round " + r_i);
                    System.out.println();
                }
                double[] stuffToBuyStartingPrice = createStartingPrices(n,sMax);
                double[][] bidsPerItem = recieveBidsPerItem(stuffToBuyStartingPrice,bss,biddingFactor,r_i);
                double[] markedPrices = calcMarketPrices(bidsPerItem);
                marketPriceInRoundR[r_i] = markedPrices;
                double[] profitSellerInCurrentRound = profitSellerInRoundR[r_i];
                double[] profitBuyerInCurrentRound = profitBuyerInRoundR[r_i];
                determineWinnerAndCalculateProfit(stuffToBuyStartingPrice,markedPrices,bidsPerItem,profitBuyerInCurrentRound,profitSellerInCurrentRound,pure,e);
                updateBiddingFactor();
            }
            //calc total profit
            for (int n_i = 0; n_i < n; n_i++) {
                for (int r_i = 0; r_i < r; r_i++) {
                   totalProfitSeller[n_i] += profitSellerInRoundR[r_i][n_i];
                }
            }
            for (int k_i = 0; k_i < bss.length; k_i++) {
                for (int r_i = 0; r_i < r; r_i++) {
                    totalProfitBuyer[k_i] += profitBuyerInRoundR[r_i][k_i];
                }
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

        private void determineWinnerAndCalculateProfit(double[] stuffToBuyStartingPrice, double[] markedPrices, double[][] bidsPerItem, double[] profitBuyerInCurrentRound, double[] profitSellerInCurrentRound, boolean pure, double e) {
            int numberBuyers = bidsPerItem[0].length;
            boolean[] hasPlayerWonInThisRound = new boolean[numberBuyers];
            int[] fromWichSellerDidBuyerBuy = new int[numberBuyers];
            for (int i = 0; i < numberBuyers; i++) fromWichSellerDidBuyerBuy[i]=-1;

            int numberOfSellers = markedPrices.length;
            Iterator<Integer> it = createSellOrder(numberOfSellers);
            //determine winner for one round
            while(it.hasNext()){
                int i = it.next();
                if (print) {
                    System.out.println("Item item item from Seller " + i + " Starting price: " + stuffToBuyStartingPrice[i]);
                    System.out.println();
                }
                double[] bidsOfParticularItem = bidsPerItem[i];
                double marketPriceOfParticularItem = markedPrices[i];

                //get a ranking of buyers determined by bid
                Integer[] ranking = getBiddingsOrder(bidsOfParticularItem);

                //iterate through the ranking until winner is reached
                int indexOfWinnerInRanking = 0;
                int n_i = ranking[indexOfWinnerInRanking];
                double payPrice = stuffToBuyStartingPrice[i];
                while (bidsOfParticularItem[n_i] > marketPriceOfParticularItem ||
                    !winnerWantsItem(pure,hasPlayerWonInThisRound[n_i],profitBuyerInCurrentRound[n_i],bidsOfParticularItem[ranking[indexOfWinnerInRanking+1]],e)){
                    indexOfWinnerInRanking++;
                    if(indexOfWinnerInRanking==ranking.length-1){
                        //apperently all prices are the same or sombody doesnt want it, lets give it to the second to last
                        indexOfWinnerInRanking=ranking.length-2;
                        payPrice = bidsOfParticularItem[ranking[indexOfWinnerInRanking+1]];
                        break;
                    }


                    n_i = ranking[indexOfWinnerInRanking];
                    payPrice = bidsOfParticularItem[ranking[indexOfWinnerInRanking+1]];

                }

                //we have a winner
                assert (payPrice>0);
                assert (profitSellerInCurrentRound[i]<0.000001);
                if (pure){
                    profitSellerInCurrentRound[i]=payPrice;
                    double profitBuyer = marketPriceOfParticularItem-payPrice;
                    assert (profitBuyerInCurrentRound[n_i]<0.0000001);
                    profitBuyerInCurrentRound[n_i]=profitBuyer;

                }else {
                    if (hasPlayerWonInThisRound[n_i]){

                        double punish = profitBuyerInCurrentRound[n_i]*e;

                        assert (punish>0);
                        profitBuyerInCurrentRound[n_i] = payPrice-punish;
                        profitSellerInCurrentRound[i] = payPrice;
                        profitSellerInCurrentRound[fromWichSellerDidBuyerBuy[n_i]] = punish;
                    }else{
                        profitBuyerInCurrentRound[n_i] = payPrice;
                        profitSellerInCurrentRound[i] = payPrice;
                    }
                    fromWichSellerDidBuyerBuy[n_i] = i;
                }

                hasPlayerWonInThisRound[n_i] = true;

            }


        }

        private boolean winnerWantsItem(boolean pure, boolean hasPlayerWonInThisRound, double oldProfit, double newProfit, double e) {
            if (pure) return !hasPlayerWonInThisRound;

            //slightly different then description, but my profit includes my punishment, otherwise its stupid
            if (oldProfit>(newProfit-e*oldProfit))return false;
            return true;
        }

        private Iterator createSellOrder(int numberOfSellers) {
            ArrayList<Integer> a = new ArrayList<>(numberOfSellers);
            for (int i = 0; i < numberOfSellers; i++) a.add(i);
            Collections.shuffle(a);
            return a.iterator();
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
         * @param biddingFactor
         * @param r curent round
         * @return an array which contains for every item a bid of every buyer
         */
        private double[][] recieveBidsPerItem(double[] stuffToBuyStartingPrice, BiddingStrategy[] bss, double[][] biddingFactor, int r) {
            int k = stuffToBuyStartingPrice.length;
            int n = bss.length;

            double[][] bidsPerItem = new double[k][n];
            for (int k_i = 0; k_i < k; k_i++) {
                double startingPrice = stuffToBuyStartingPrice[k_i];
                double[] bidsForParticularItem = bidsPerItem[k_i];
                for (int n_i = 0; n_i < n; n_i++) {

                    if (k_i == 6){
                        System.out.println(k_i + " " + n_i);
                    }
                    bidsForParticularItem[n_i] = bss[n_i].bid(profitSellerInRoundR,profitBuyerInRoundR,r,startingPrice,biddingFactor[n_i][k_i]);
                }

            }
            return bidsPerItem;
        }

        private double[] createStartingPrices(int n, double sMax) {
            //its been said, that sellers want to maximise their profits, but i dont see it mentioned further
            double[] prizeOfEachSeller = new double[n];
            for (int i = 0; i < n; i++) {
                prizeOfEachSeller[i] = rng.nextDouble()*sMax;
            }
            return prizeOfEachSeller;
        }

        public void printout() {
            for (int n_i = 0; n_i < marketPriceInRoundR[0].length; n_i++) {
                System.out.println("Market Prices for seller " + n_i);
                for (int r_i = 0; r_i < r; r_i++) {
                    System.out.print(marketPriceInRoundR[r_i][n_i] + ",");
                }
                System.out.println();
                System.out.println();
            }

            System.out.println("Profit for seller:");
            for (int n_i = 0; n_i < totalProfitSeller.length; n_i++) {
                    System.out.print(totalProfitSeller[n_i] + ",");
            }
            System.out.println();
            System.out.println("Profit for buyer:");
            for (int k_i = 0; k_i < totalProfitBuyer.length; k_i++) {
                System.out.print(totalProfitBuyer[k_i] + ",");
            }
            System.out.println();
            System.out.println();
        }

        private void updateBiddingFactor()
        {
            double bidDecreaseFactor = 0.8;
            double bidIncreaseFactor = 1.2;

            for (int i = 0; i < bss.length; i++)
            {
                for (int j = 0; j < n; j++)
                {
                    // if buyer i won from seller n   or   the bid was higher than the avg price
                    if(false)
                    {
                        biddingFactor[i][j] *= bidDecreaseFactor;
                    }
                    else
                    {
                        biddingFactor[i][j] *= bidIncreaseFactor;
                    }
                }
            }
        }
    }

    private enum BiddingStrategy {
        Version1{
            @Override
            public double bid(double[][] profitSellerInRoundR, double[][] profitBuyerInRoundR, int r, double startingPrice, double biddingFactor) {
                return startingPrice*biddingFactor;
            }
        },Version2{
            @Override
            public double bid(double[][] profitSellerInRoundR, double[][] profitBuyerInRoundR, int r, double startingPrice, double biddingFactor) {
                return 0;
            }
        },Version3{
            @Override
            public double bid(double[][] profitSellerInRoundR, double[][] profitBuyerInRoundR, int r, double startingPrice, double biddingFactor) {
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

        public abstract double bid(double[][] profitSellerInRoundR, double[][] profitBuyerInRoundR, int r, double startingPrice, double biddingFactor);
    }


}
