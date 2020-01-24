package droneRush;

import battlecode.common.*;

public class Refinery extends Building {

	public Refinery(RobotController rc) {
		super(rc);
	}

	@Override
	protected void run() {

		while(true) {
			try {
				rc.isLocationOccupied(loc); // Just to give the catch something
				yield();
			} catch(GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
			}
		}
	}
}