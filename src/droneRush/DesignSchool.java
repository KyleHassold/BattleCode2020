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
		
		// Spawn direction is either South or North
		Direction dir = loc.directionTo(HQs[0]);
		if(dir == Direction.SOUTHEAST || dir == Direction.SOUTHWEST) {
			dir = Direction.SOUTH;
		} else {
			dir = Direction.NORTH;
		}
		
		while(true) {
			if(landscapers < 20 && (ref != null || rc.getRoundNum() > 300)) {
				if(buildRobot(RobotType.LANDSCAPER, dir)) {
					landscapers++;
				} else {
					System.out.println("Failure: DesignSchool.run()\nFailed to build Landscaper");
				}
			}
			yield();
		}
	}
}