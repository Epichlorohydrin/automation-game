package edu.brandeis.cosi103a.groupa;

/**
 * Represents a purchasable card with a name and a cost in coins.
 */
interface Card {

    /**
     * @return the card name (e.g., "Bitcoin", "Method")
     */
    String name();

    /**
     * @return cost in coins to buy this card from the supply
     */
    int cost();
}

/**
 * An automation card that contributes Automation Points (AP) toward winning.
 */
final class AutomationCard implements Card {
    private String name;
    private int cost;
    private int apValue;

    /**
     * Constructs an automation card.
     *
     * @param name card name
     * @param cost cost in coins
     * @param apValue automation points provided by this card
     */
    public AutomationCard(String name, int cost, int apValue) {
        this.name = name;
        this.cost = cost;
        this.apValue = apValue;
    }

    /**
     * @return automation points provided by this card
     */
    public int apValue() {
        return apValue;
    }

    /** @return card name */
    @Override
    public String name() {
        return name;
    }

    /** @return cost in coins */
    @Override
    public int cost() {
        return cost;
    }

    /**
     * Compares this automation card with another object for value equality.
     *
     * @param obj object to compare against
     * @return true if the other object is an equivalent automation card
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AutomationCard)) {
            return false;
        }
        AutomationCard other = (AutomationCard) obj;
        return this.name.equals(other.name) && this.cost == other.cost && this.apValue == other.apValue;
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + cost * 17 + apValue * 7;
    }
}

/**
 * A cryptocurrency card that produces coins during the buy phase.
 */
final class CryptoCard implements Card {
    private String name;
    private int cost;
    private int coinValue;

    /**
     * Constructs a cryptocurrency card.
     *
     * @param name card name
     * @param cost cost in coins
     * @param coinValue coins produced when played
     */
    public CryptoCard(String name, int cost, int coinValue) {
        this.name = name;
        this.cost = cost;
        this.coinValue = coinValue;
    }

    /**
     * @return coins produced when this card is played
     */
    public int coinValue() {
        return coinValue;
    }

    /** @return card name */
    @Override
    public String name() {
        return name;
    }

    /** @return cost in coins */
    @Override
    public int cost() {
        return cost;
    }

    /**
     * Compares this crypto card with another object for value equality.
     *
     * @param obj object to compare against
     * @return true if the other object is an equivalent crypto card
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CryptoCard)) {
            return false;
        }
        CryptoCard other = (CryptoCard) obj;
        return this.name.equals(other.name) && this.cost == other.cost && this.coinValue == other.coinValue;
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + cost * 17 + coinValue * 7;
    }
}
