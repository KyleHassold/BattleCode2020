package droneRush;

import battlecode.common.*;

public class Vaporator extends Building {

	public Vaporator(RobotController rc) {
		super(rc);
	}

	@Override
	protected void run() throws GameActionException {

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
