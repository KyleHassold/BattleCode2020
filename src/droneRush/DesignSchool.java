package droneRush;

import battlecode.common.*;

public class DesignSchool extends Building {

	public DesignSchool(RobotController rc) {
		super(rc);
		try {
			checkTransactions();
		} catch (GameActionException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void run() throws GameActionException {
		if(terraformer) {
			System.out.println("I'm a terraformer");
			while(rc.getTeamSoup() < RobotType.LANDSCAPER.cost + 10 || !buildRobot(RobotType.LANDSCAPER, loc.directionTo(HQs[0]))) {
				yield();
			}
			int[] message = new int[] {117298, HQs[0].x, HQs[0].y, -1, -1, -1, -1};
			while(!rc.canSubmitTransaction(message, 10)) {
				yield();
			}
			rc.submitTransaction(message, 10);
		}
		
		int landscapers = 0;
		Direction dir = loc.directionTo(HQs[0]);
		if(dir == Direction.SOUTHEAST || dir == Direction.SOUTHWEST) {
			dir = Direction.SOUTH;
		} else {
			dir = Direction.NORTH;
		}
		while(true) {
			try {
				if(landscapers < 8 && (ref != null || rc.getRoundNum() > 300) && rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
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
