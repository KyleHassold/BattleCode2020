package droneRush;

import battlecode.common.*;

public class DesignSchool extends Building {

	public DesignSchool(RobotController rc) {
		super(rc);
	}

	@Override
	protected void run() throws GameActionException {
		int landscapers = 0;
		Direction dir = loc.directionTo(HQs[0]).rotateLeft();
		while(true) {
			try {
				if((landscapers < 4 && ref != null) || (landscapers < 10 && vaporator != null) && rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
					rc.buildRobot(RobotType.LANDSCAPER, dir);
					landscapers++;
				}
			} catch(GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
			}
			yield();
		}
	}
}
