package classesBaseCode;

import battlecode.common.*;

public class Miner extends Unit {

	public Miner(RobotController rc) {
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
