package classesBaseCode;

import battlecode.common.*;

public class Refinery extends Building {

	public Refinery(RobotController rc) {
		super(rc);
	}

	@Override
	protected void run() throws GameActionException {

		while(true) {
			try {
				rc.isLocationOccupied(rc.getLocation()); // Just to give the catch something
			} catch(GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
			}

			yield();
		}
	}
}
