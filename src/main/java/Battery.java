public class Battery {
    private int capacity;
    private int currentLevel;

    public Battery(int capacity) {
        this.capacity = capacity;
        this.currentLevel = capacity;
    }

    public void consumePower() {
        if (currentLevel > 0) {
            currentLevel -= 10;
        }
    }

    public int getLevel() {
        return currentLevel;
    }
}
