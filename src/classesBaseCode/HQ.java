package classesBaseCode;

import battlecode.common.*;

public class HQ extends Building {

	public HQ(RobotController rc) {
		super(rc);
	}

	@Override
	protected void run() throws GameActionException {
		while(true) {
			try {
				rc.isLocationOccupied(new MapLocation(-30,-30)); // Just to give the catch something
			} catch(GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
			}

			yield();
		}
	}
}
