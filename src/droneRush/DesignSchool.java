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
			
			while(rc.getTeamSoup() < RobotType.LANDSCAPER.cost + 10 || !buildRobot(RobotType.LANDSCAPER, loc.directionTo(HQs[0]))) {
				yield();
			}
			
			submitTransaction(new int[] {teamCode, HQs[0].x, HQs[0].y, -1, -1, -1, 8}, 10, true);
			while(true) {
				yield();
			}
		}
		
		int landscapers = 0;
		
		// Spawn direction is either South or North
		/*Direction dir = loc.directionTo(HQs[0]);
		if(dir == Direction.SOUTHEAST || dir == Direction.SOUTHWEST) {
			dir = Direction.SOUTH;
		} else {
			dir = Direction.NORTH;
		}*/
		
		Direction dir = HQs[0].y - loc.y == 2 ? Direction.NORTH : Direction.SOUTH;
		while(landscapers < 8) {
			if(rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
				try {
					rc.buildRobot(RobotType.LANDSCAPER, dir);
					landscapers++;
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		dir = HQs[0].x - loc.x == 1 ? Direction.WEST : Direction.EAST;
		while(landscapers < 28) {
			if((ref != null || rc.getRoundNum() > 300) && rc.canBuildRobot(RobotType.LANDSCAPER, dir)) {
				try {
					rc.buildRobot(RobotType.LANDSCAPER, dir);
					landscapers++;
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			yield();
		}
		
		try {
			while(rc.canSenseLocation(rc.adjacentLocation(dir)) && rc.senseRobotAtLocation(rc.adjacentLocation(dir)) != null && rc.senseRobotAtLocation(rc.adjacentLocation(dir)).type == RobotType.LANDSCAPER) {
				yield();
			}
			
			submitTransaction(new int[] {teamCode, 4, -1, -1, -1, -1, 7}, 10, true);
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		
		while(true) {
			yield();
		}
	}
}