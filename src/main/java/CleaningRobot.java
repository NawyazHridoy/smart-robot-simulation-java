public class CleaningRobot extends Robot {
    private int cleaningPower;

    public CleaningRobot(int id, String name, int x, int y, int speed, int cleaningPower) {
        super(id, name, x, y, speed);
        this.cleaningPower = cleaningPower;
    }

    public void cleanArea() {
        System.out.println(getName() + " is cleaning the area with power " + cleaningPower);
    }

    @Override
    public void display() {
        super.display();
        System.out.println("Specialty: Cleaning Robot | Cleaning Power: " + cleaningPower);
    }
}
