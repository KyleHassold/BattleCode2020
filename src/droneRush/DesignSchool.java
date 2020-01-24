package droneRush;

import battlecode.common.*;

public class DesignSchool extends Building {

	public DesignSchool(RobotController rc) {
		super(rc);
		checkTransactions();
	}

	@Override
	protected void run() {
		if(terraformer) {
			while(rc.getTeamSoup() < RobotType.LANDSCAPER.cost + 10 || !buildRobot(RobotType.LANDSCAPER, loc.directionTo(HQs[0]))) {
				yield();
			}
			submitTransaction(new int[] {teamCode, HQs[0].x, HQs[0].y, -1, -1, -1, 8}, 10, true);
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
				System.out.println("Error: DesignSchool.run() Failed!\nrc.buildRobot(landscaper, " + dir + ") Failed!");
                e.printStackTrace();
			}
			yield();
		}
	}
}