package phases2Cleaned;

import battlecode.common.*;

public class Landscaper extends Unit {

	protected Landscaper(RobotController rc) {
		super(rc);
	}

	@Override
	protected void run() throws GameActionException {
		MapLocation loc = rc.getLocation();
		Direction dir = getDirection(new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2), loc);
		HQs[0] = new MapLocation(loc.x - 4 * dir.dx, loc.y - 4 * dir.dy);
		// Phase 3 landscaper locations
		MapLocation[] wallBuilding = new MapLocation[] {
				new MapLocation(HQs[0].x - 2, HQs[0].y - 1), new MapLocation(HQs[0].x - 2, HQs[0].y - 0),
				new MapLocation(HQs[0].x - 2, HQs[0].y + 1), new MapLocation(HQs[0].x - 1, HQs[0].y + 1),
				new MapLocation(HQs[0].x - 0, HQs[0].y + 1), new MapLocation(HQs[0].x + 1, HQs[0].y + 1),
				new MapLocation(HQs[0].x + 1, HQs[0].y + 0), new MapLocation(HQs[0].x + 1, HQs[0].y - 1),
				new MapLocation(HQs[0].x - 0, HQs[0].y - 1), new MapLocation(HQs[0].x - 1, HQs[0].y - 1),
		};
				/*
				new MapLocation(HQs[0].x - 1, HQs[0].y - 2), new MapLocation(HQs[0].x - 2, HQs[0].y - 2),
				new MapLocation(HQs[0].x - 3, HQs[0].y - 2), new MapLocation(HQs[0].x - 3, HQs[0].y - 1),
				new MapLocation(HQs[0].x - 3, HQs[0].y - 0), new MapLocation(HQs[0].x - 3, HQs[0].y + 1),
				new MapLocation(HQs[0].x - 3, HQs[0].y + 2), new MapLocation(HQs[0].x - 2, HQs[0].y + 2),
				new MapLocation(HQs[0].x - 1, HQs[0].y + 2), new MapLocation(HQs[0].x - 0, HQs[0].y + 2),
				new MapLocation(HQs[0].x + 1, HQs[0].y + 2), new MapLocation(HQs[0].x + 2, HQs[0].y + 2),
				new MapLocation(HQs[0].x + 2, HQs[0].y + 1), new MapLocation(HQs[0].x + 2, HQs[0].y + 0),
				new MapLocation(HQs[0].x + 2, HQs[0].y - 1), new MapLocation(HQs[0].x + 2, HQs[0].y - 2),
				new MapLocation(HQs[0].x + 1, HQs[0].y - 2), new MapLocation(HQs[0].x - 2, HQs[0].y + 1),
				new MapLocation(HQs[0].x - 1, HQs[0].y + 1), new MapLocation(HQs[0].x - 0, HQs[0].y + 1),
				new MapLocation(HQs[0].x + 1, HQs[0].y + 1), new MapLocation(HQs[0].x + 1, HQs[0].y + 0),
				new MapLocation(HQs[0].x - 2, HQs[0].y + 0), new MapLocation(HQs[0].x - 2, HQs[0].y - 1),
				new MapLocation(HQs[0].x - 1, HQs[0].y - 1), new MapLocation(HQs[0].x + 1, HQs[0].y - 1),
				new MapLocation(HQs[0].x + 0, HQs[0].y - 1), new MapLocation(HQs[0].x + 0, HQs[0].y - 2)
		};
				*/
		int currPos = 0;
		while(!rc.getLocation().equals(wallBuilding[currPos])) {
			if(rc.canSenseLocation(wallBuilding[currPos]) && rc.senseRobotAtLocation(wallBuilding[currPos]) != null) {
				currPos = (currPos + 1) % wallBuilding.length;
			} else {
				moveCloser(wallBuilding[currPos], false);
				Clock.yield();
			}
		}
		Direction dig = getDirection(HQs[0], rc.getLocation());
		Direction deposit = Direction.CENTER;
		/*
		if(findAdjacentRobot(RobotType.HQ, rc.getTeam()) != null) {
			deposit = dig;
			dig = Direction.CENTER;
		}
		*/
		
		while(true) {
			if(rc.canDigDirt(dig)) {
				rc.digDirt(dig);
			} else if(rc.canDepositDirt(deposit)){
				rc.depositDirt(deposit);
			}
			Clock.yield();
		}
	}

}
