package droneRush;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Refinery extends Building {

	public Refinery(RobotController rc) {
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
