
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Niebisch Markus on 11.12.2019.
 */
public class OnceBrassCollusion {
    static final Logger logger = Logger.getLogger(OnceBrassCollusion.class.getName());
    public static Random rng;
    public static boolean print;

    public static void main(String[] args){
        print = true;
        OnceBrassCollusion.setRandomNumberGenerator(new Random(1));
        OnceBrassCollusion obc = new OnceBrassCollusion();

        obc.doAllExperiments();
    }

    private void doAllExperiments() {
        int minK = 6; //sellers
        int maxK = 6;
        int minN = 6; //buyer
        int maxN = 6;
        int threasholdNIncrement = 12;
        double incrementalFactorN = 1.5;
        int minR = 4;
        int maxR = 10;

        double sMax = 1000;
        double e = 1.;
        boolean pure = false;


        int count = 0;
        for (int k = minK; k <= maxK; k++) {
            for (int n = minN; n <= maxN; n = increase(n, threasholdNIncrement, incrementalFactorN)) {
                for (int r = minR; r < maxR; r = increase(r, 1, 1.5)) {
                    BiddingStrategy[] bs1 = BiddingStrategy.getAllOfOneType(BiddingStrategy.Version1,n);
                    BiddingStrategy[] bs2 = BiddingStrategy.getUniformSelection(k);
                    BiddingStrategy[] bs3 = BiddingStrategy.compareTwoStrategies(BiddingStrategy.Version1,BiddingStrategy.Version2,k);

                    Simulation s = new Simulation(bs2,n,k,r,sMax,e,pure,count);
                    s.printout();
                    count++;
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

    private double sumDoubleArray(double[] arrayToSum) {
        double sum = 0.;
        for (int r_i = 0; r_i < arrayToSum.length; r_i++) {
            sum += arrayToSum[r_i];
        }
        return sum;
    }

    private class Simulation {
        private final double sMax;
        private final BiddingStrategy[] bss;
        private final int k;
        private final Seller[] sellers;
        private final Buyer[] buyers;
        private double[][] biddingFactor;
        private final int r;
        private final int n;
        private final boolean pure;
        private final double e; //punishment factor




        public Simulation(BiddingStrategy[] bs, int n, int k, int r, double sMax, double e, boolean pure, int count) {
            this.sMax = sMax;
            this.bss = bs;
            this.n = n;
            this.k = k;

            this.sellers = createSellers(k,r,sMax);
            this.buyers = createBuyers(n,r,bs);
            this.r = r;
            this.pure = pure;
            this.e = e;

            simulate(pure,e);

        }



        public void simulate(boolean pure, double e) {
            if (print)System.out.println("Start simulation: NumberSellers: " + k + " NumberBuyers: " + n + " pure: " + pure);
            if (print)System.out.println("NumberRounds: " + r + " ErrorFactor: " + e);
            for (int i = 0; i < buyers.length; i++) {
                Buyer b = buyers[i];
                System.out.println("Buyer: " + i + " IncreaseFactor: " + b.biddingStrategy.getIncreaseFactor() + " DecreaseFactor: " + b.biddingStrategy.getDecreaseFactor());
            }
            if (print)System.out.println();
            for (int r_i = 0; r_i < r; r_i++) { //for each round
                if (print) {
                    System.out.println("This shall be round " + r_i);
                    System.out.println();
                }

                bettingRound( r_i, sellers,  buyers,  pure,  e);

            }



        }

        private void bettingRound(int r_i, Seller[] sellers, Buyer[] buyers, boolean pure, double e) {

            Iterator<Integer> it = createSellOrder(sellers.length);
            //determine winner for one round
            while(it.hasNext()){
                int i = it.next();
                Seller seller = sellers[i];
                Item item = seller.offerItem(r_i);
                if (print) System.out.println("Item item item from Seller " + seller.sellerIndex + " Starting price: " + item.startingPrice);

                Auction auction = new Auction(buyers,item,r_i,pure,e);
                for (Buyer b:buyers) {
                    b.adjustBiddingFactor(item,r_i);
                }
            }


        }


        private Iterator createSellOrder(int numberOfSellers) {
            ArrayList<Integer> a = new ArrayList<>(numberOfSellers);
            for (int i = 0; i < numberOfSellers; i++) a.add(i);
            Collections.shuffle(a);
            return a.iterator();
        }



        public void printout() {
            for (int k_i = 0; k_i < sellers.length; k_i++) {
                Seller s = sellers[k_i];
                System.out.println("Market Prices for seller: " + s.sellerIndex);
                for (int r_i = 0; r_i < r; r_i++) {
                    System.out.print(s.getItem(r_i).marketPrice + ",");
                }
                System.out.println();
                System.out.println();
            }

            System.out.println("Profit for sellers:");
            for (int n_i = 0; n_i < sellers.length; n_i++) {
                    System.out.print(sellers[n_i].getTotalProfit() + ",");
            }
            System.out.println();
            System.out.println("Profit for buyer:");
            for (int k_i = 0; k_i < buyers.length; k_i++) {
                System.out.print(buyers[k_i].getTotalProfit() + ",");
            }
            System.out.println();
            System.out.println();
        }


    }

    private Seller[] createSellers(int numerberSellers, int r, double maximumStartingPrice) {
        Seller[] b = new Seller[numerberSellers];
        for (int index = 0; index < numerberSellers; index++) {
            b[index] = new Seller(index,r,maximumStartingPrice);
        }
        return b;
    }

    private Buyer[] createBuyers(int n, int r, BiddingStrategy[] bs) {
        Buyer[] b = new Buyer[n];
        for (int index = 0; index < n; index++) {
            b[index] = new Buyer(index,r,n,bs[index]);
        }
        return b;
    }



    private enum BiddingStrategy {
        Version1(0.8,1.2){
            @Override
            public Bid bid(Buyer buyer, Item item, double e, int r_i) {
                Item i = buyer.checkInventory(r_i);
                Seller s = item.seller;
                int si = s.sellerIndex;

                double value = 0;
                value = buyer.biddingFactor[si]*item.getStartingPrice();
                if (i!=null){
                    value -= i.getFee(e)+buyer.getProfitInRoundR(r_i);
                }
                return new Bid(value, buyer);
            }


        },Version2(0.9,1.1){
            @Override
            public Bid bid(Buyer buyer, Item item, double e, int r_i) {
                return Version1.bid(buyer, item, e, r_i);
            }
        },Version3(0.7,1.1){
            @Override
            public Bid bid(Buyer buyer, Item item, double e, int r_i) {
                return Version1.bid(buyer, item, e, r_i);
            }
        };

        private final double decrease;
        private final double increse;

        BiddingStrategy(double decrease, double increase) {
            this.decrease = decrease;
            this.increse = increase;
        }

        public static BiddingStrategy[] compareTwoStrategies(BiddingStrategy b1, BiddingStrategy b2, int k){
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

        public abstract Bid bid(Buyer buyer, Item item, double e, int r_i);

        public double getDecreaseFactor(){
            return decrease;
        }

        public double getIncreaseFactor(){
            return increse;
        }
    }


    private class Seller {
        private final double[] feeInRoundR;
        private final double[] profitInRoundR;
        private final int sellerIndex;

        private final Item[] items;
        private final double maximumStartingPrice;
        ;

        private Seller(int sellerIndex, int r, double maximumStartingPrice){
            this.sellerIndex = sellerIndex;
            profitInRoundR = new double[r];
            feeInRoundR = new double[r];
            items = createItems(r);
            this.maximumStartingPrice = maximumStartingPrice;

        }

        private Item[] createItems(int r) {
            Item[] items = new Item[r];
            for (int i = 0; i < r; i++) {
                items[i] = new Item(this);
            }
            return items;
        }

        public double getProfitInRoundR(int r) {
            return profitInRoundR[r];
        }
        public double getTotalProfit(){
            return sumDoubleArray(profitInRoundR)+sumDoubleArray(feeInRoundR);
        }

        public Item offerItem(int r){
            Item item = items[r];
            item.setStartingPrice(rng.nextDouble()*maximumStartingPrice); //here we can adjust if for example last item was returned, increase price (but wouldnt that mean no reason to lower the price???)
            return item;
        }


        public Item getItem(int r_i) {
            return items[r_i];
        }
    }
    private class Buyer {
        private final double[] feeInRoundR;
        private final double[] profitInRoundR;
        private final int index;
        private final double[] biddingFactor;
        private final BiddingStrategy biddingStrategy;
        private final Item[] inventory;


        public Buyer(int index, int r, int numberSellers, BiddingStrategy bs) {
            this.index = index;
            this.biddingStrategy = bs;
            profitInRoundR = new double[r];
            feeInRoundR = new double[r];
            biddingFactor = createBiddingFactor(numberSellers); //for every buyer sellers combination a bidding factor


            inventory = new Item[r];
        }

        public double getProfitInRoundR(int r) {
            return profitInRoundR[r];
        }
        public double getTotalProfit(){
            return sumDoubleArray(profitInRoundR)-sumDoubleArray(feeInRoundR);
        }

        private double[] createBiddingFactor(int numberSellers) {
            double[] biddinFactor = new double[numberSellers];

                for (int j = 0; j < numberSellers; j++) {
                    biddinFactor[j] = rng.nextDouble()*4+1; //it doesnt make sense to bid less then the starting price
                }

            return biddinFactor;
        }

        public void makeBid(Item item, int r_i, double e) {
           Bid b = biddingStrategy.bid(this,item,e,r_i);
           item.recieveBid(b);

        }

        public Item checkInventory(int r_i) {
            return inventory[r_i];
        }


        public void adjustBiddingFactor(Item item, int r_i) {
            double bidDecreaseFactor = this.biddingStrategy.getDecreaseFactor();
            double bidIncreaseFactor = this.biddingStrategy.getIncreaseFactor();

            Bid bidOfBuyer = item.getBidOfBuyer(this);
            if (bidOfBuyer==null){
                return; //i dont think adjusting makes sense, if he didnt bet
            }
            Seller s = item.seller;
            Item itemOfBuyer = checkInventory(r_i);
            if (item.equals(itemOfBuyer)||(bidOfBuyer.value>item.marketPrice)){
                biddingFactor[s.sellerIndex] *= bidDecreaseFactor;
            }else{
                biddingFactor[s.sellerIndex] *= bidIncreaseFactor;
            }
        }
    }

    private class Item {
        final Seller seller;
        boolean sold = false;
        boolean returned = false;
        private double startingPrice;
        private double marketPrice;
        private double price;
        private Buyer buyer; //probably not needed

        private ArrayList<Bid> bids = new ArrayList<>();

        private Item(Seller seller) {
            this.seller = seller;
        }

        public void setStartingPrice(double startingPrice) {
            this.startingPrice =  startingPrice;
        }
        public double getStartingPrice() {
            return startingPrice;
        }

        public void determineMarketPrice(){
            double sum = 0;
            for (Bid b:bids) {
                sum+=b.value;
            }
            sum/=bids.size();
            this.marketPrice = sum;
        }

        public void buy(double price, Buyer buyer,int r_i){
            this.sold = true;

            this.price = price;
            this.buyer = buyer;
            seller.profitInRoundR[r_i] += price;
            buyer.profitInRoundR[r_i] += (this.marketPrice-price);
        }

        public void recieveBid(Bid b){
            bids.add(b);
        }

        public ArrayList<Bid> getBids() {
            return bids;
        }


        public double getMarketPrice() {
            determineMarketPrice();
            return marketPrice;
        }

        public void returnItem(double errorFactor,int r_i) {
            returned = true;
            buyer = null;

            double fee = getFee(errorFactor);
            seller.profitInRoundR[r_i] = 0; //as there can only be one item, setting profit to 0 should suffice
            seller.feeInRoundR[r_i] += fee;
            buyer.profitInRoundR[r_i] = 0;
            buyer.feeInRoundR[r_i] += fee;


        }

        private double getFee(double errorFactor) {
            return errorFactor * price;
        }

        public Bid getBidOfBuyer(Buyer buyer) {
            for (Bid b:bids) {
                if (b.buyer.equals(buyer))return b;
            }
            return null;
        }
    }

    private static class Auction {
        public Auction(Buyer[] buyers, Item item, int r_i, boolean pure, double e) {
            //recieve Bids
            List<Buyer> buyerList = Arrays.asList(buyers);
            Collections.shuffle(buyerList);
            for (Buyer b:buyerList) {
                if (pure){
                    Item i = b.checkInventory(r_i);
                    if (i!=null)continue; //can only bid if no buy in this round
                }
                b.makeBid(item,r_i,e);
            }

            ReturnObject pbb = determineWinnerAndPriceToPay(item);
            double payPrice = pbb.getPrice();
            Buyer buyer = pbb.getBuyer();
            Item oldItem = buyer.checkInventory(r_i);
            if (oldItem!=null){
                if (print) System.out.println("buyer returns old item");
                oldItem.returnItem(e,r_i);
            }
            item.buy(payPrice,buyer,r_i);
            if (print) System.out.println("Buyer " + buyer.index + " wins and pays " + payPrice + " Market: " + item.marketPrice);
            //we have a winner
        }

        private ReturnObject determineWinnerAndPriceToPay(Item item) {
            Buyer finalBuyer = null;
            Double finalPrice = null;
            double mp = item.getMarketPrice();
            ArrayList<Bid> bids = item.getBids();
            Collections.sort(bids);  //get a ranking of bids
            Iterator<Bid> it = bids.iterator();
            while(it.hasNext()){
                Bid currentBid = it.next();
                if (currentBid.value>mp)continue;
                //this is the winner
                finalBuyer = currentBid.buyer;
                Bid nextBid;
                if (it.hasNext()){
                    nextBid = it.next();
                    finalPrice = nextBid.value;
                }else {
                    System.out.println("the last haz won");
                    //From Task description: Note that in case of only one (winning) bid is below market price,
                    // second highest bid is calculated as the average between the winning bid
                    // and the auction’s starting price.
                    finalPrice = (currentBid.value+item.getStartingPrice())/2;
                }


            }
            if (finalBuyer==null||finalPrice==null){
                System.out.println("no winner could be determined");
            }
            return new ReturnObject(finalBuyer, finalPrice);
        }


        private class ReturnObject {
            private final Double finalPrice;
            private final Buyer finalBuyer;

            public ReturnObject(Buyer finalBuyer, Double finalPrice) {
                this.finalBuyer = finalBuyer;
                this.finalPrice = finalPrice;
            }

            public double getPrice() {
                return finalPrice;
            }

            public Buyer getBuyer() {
                return finalBuyer;
            }
        }
    }

    private static class Bid implements Comparable{

        public double value;
        public Buyer buyer;

        public Bid(double value, Buyer buyer){
            this.value = value;
            this.buyer = buyer;
        }
        @Override
        public int compareTo(Object o) {
            if (!(o instanceof Bid))return 0;
            Bid b2 = (Bid)o;
            if (this.value>b2.value)return -1;
            if (this.value<b2.value)return 1;
            return 0;
        }
    }
}
