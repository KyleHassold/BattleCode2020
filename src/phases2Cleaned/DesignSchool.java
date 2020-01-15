package phases2Cleaned;

import battlecode.common.*;

public class DesignSchool extends Robot {

	protected DesignSchool(RobotController rc) {
		super(rc);
	}

	@Override
	protected void run() throws GameActionException {
		int count = 0;
		MapLocation loc = rc.getLocation();
		Direction dir = getDirection(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2), loc);
		HQs[0] = new MapLocation(loc.x - 3 * dir.dx, loc.y - 3 * dir.dy);
		
		
		// Spawn in 5 Landscapers for phase 2
		while(count < 9) {
			checkTransactions();
			if(rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
				rc.buildRobot(RobotType.LANDSCAPER, dir);
				count++;
			}
			Clock.yield();
		}
		
		// Wait for phase 3
		while(vaporator == null) {
			checkTransactions();
			Clock.yield();
		}
		
		while(count < 10) {
			checkTransactions();
			if(rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
				rc.buildRobot(RobotType.LANDSCAPER, dir);
				count++;
			}
			Clock.yield();
		}
		
		// Prevent future code
		while(phase < 3) {
			checkTransactions();
			Clock.yield();
		}
		
		while(count < 33) {
			checkTransactions();
			if(rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
				rc.buildRobot(RobotType.LANDSCAPER, dir);
				count++;
			}
			Clock.yield();
		}
		while(true) {
			Clock.yield();
		}
	}

}
