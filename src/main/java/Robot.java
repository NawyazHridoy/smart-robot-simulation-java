public class Robot extends SimulationObject {
    private int speed;
    private String direction;
    private Battery battery; // UML অনুযায়ী Battery অবজেক্ট যুক্ত করা হয়েছে

    public Robot(int id, String name, int x, int y, int speed) {
        super(id, name, x, y);
        this.speed = speed;
        this.direction = "NORTH";
        this.battery = new Battery(100); // 100% ব্যাটারি নিয়ে শুরু
    }

    public void moveForward() {
        if (direction.equals("NORTH")) {
            setPosition(getX(), getY() + speed);
        }
        battery.consumePower(); // মুভ করলে ব্যাটারি কমবে
        System.out.println(getName() + " moved forward to position: (" + getX() + "," + getY() + ")");
    }

    @Override
    public void update() {
        System.out.println(getName() + " status updated.");
    }

    @Override
    public void display() {
        System.out.println("--- Robot Status ---");
        System.out.println("ID: " + getId() + " | Name: " + getName());
        System.out.println("Position: (" + getX() + "," + getY() + ")");
        System.out.println("Direction: " + direction + " | Speed: " + speed);
        System.out.println("Battery Level: " + battery.getLevel() + "%");
    }
}
