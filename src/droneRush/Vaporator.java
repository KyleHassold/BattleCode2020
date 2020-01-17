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
				rc.isLocationOccupied(new MapLocation(-30,-30)); // Just to give the catch something
				Clock.yield();
			} catch(GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
			}
		}
	}
}
