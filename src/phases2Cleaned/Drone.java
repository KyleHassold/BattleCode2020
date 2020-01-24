package phases2Cleaned;

import battlecode.common.*;

public class Drone extends Unit {

	protected Drone(RobotController rc) {
		super(rc);
	}

	@Override
	protected void run() {
		while(true) {
			Clock.yield();
		}
	}

}
