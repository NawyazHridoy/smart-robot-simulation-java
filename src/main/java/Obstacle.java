public class Obstacle extends SimulationObject {
    private String type;

    public Obstacle(int id, String name, int x, int y, String type) {
        super(id, name, x, y);
        this.type = type;
    }

    public boolean isCollision(int robotX, int robotY) {
        return (getX() == robotX && getY() == robotY);
    }

    @Override
    public void update() {
        System.out.println(getName() + " (Obstacle) status updated.");
    }

    @Override
    public void display() {
        System.out.println("--- Obstacle Status ---");
        System.out.println("ID: " + getId() + " | Name: " + getName());
        System.out.println("Type: " + type + " | Position: (" + getX() + "," + getY() + ")");
    }
}
