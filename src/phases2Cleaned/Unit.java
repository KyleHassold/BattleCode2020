package phases2Cleaned;

import java.util.HashMap;

import battlecode.common.*;

public abstract class Unit extends Robot {
	int[][][] minerNewSense = {{{3, -5},{5, 3},{4, -4},{5, 1},{5, -1},{3, 5},{5, -3},{5, 2},{4, 4},{5, 0},{5, -2}},
			{{-3, 5},{2, 5},{5, 2},{4, 4},{5, -2},{0, 5},{5, 1},{4, 3},{-2, 5},{3, 5},{5, -3},{1, 5},{5, 0},{3, 4},{5, 3},{-1, 5},{5, -1}}};
	Direction prevSpot;

	protected Unit(RobotController rc) {
		super(rc);
	}
	
	// Sensing
	protected HashMap<MapLocation, int[]> newSensor() throws GameActionException {
		HashMap<MapLocation, int[]> results = new HashMap<MapLocation, int[]>();
		Direction moveDir = directions.get((directions.indexOf(prevSpot) + 4) % 8);
		MapLocation loc = rc.getLocation();
		MapLocation curr;
		int genDir;

		if(moveDir == Direction.NORTH || moveDir == Direction.EAST || moveDir == Direction.SOUTH || moveDir == Direction.WEST) {
			genDir = 0;
		} else {
			genDir = 1;
		}
		for(int[] coords : minerNewSense[genDir]) {
			int dx = 0;
			int dy = 0;
			if(moveDir.dx == -1) {
				dx = -1 * coords[0];
			} else {
				dx = coords[0];
			}
			if(moveDir.dy == -1) {
				dy = -1 * coords[1];
			} else {
				dy = coords[1];
			}
			curr = new MapLocation(loc.x + dx, loc.y + dy);
			if(rc.canSenseLocation(curr)) {
				results.put(curr, new int[] {rc.senseFlooding(curr) ? 1 : 0, rc.senseElevation(curr), rc.senseSoup(curr)});
			}
		}

		return results;
	}

	// Moving
	protected boolean moveCloser(MapLocation target, boolean avoidWalls) throws GameActionException {
		if(!rc.isReady()) {
			return false;
		}

		int[] baseMove = {0,1,2,-1,-2,3,-3,4};
		int dir = directions.indexOf(getDirection(rc.getLocation(), target));
		if(dir == -1) { // On spot already
			return false;
		}

		Direction moveDir;
		for(int dDir : baseMove) {
			moveDir = directions.get((dir + dDir + 8) % 8);
			if(canMoveComplete(moveDir, avoidWalls)) {
				rc.move(moveDir);
				prevSpot = directions.get((dir + dDir + 4) % 8);
				return true;
			}
		}
		return false;
	}

	protected boolean moveRandom() throws GameActionException {
		Direction rand;
		int count = 0;
		while(rc.isReady() && count < 16) {
			rand = directions.get((int) (Math.random() * directions.size()));
			if(canMoveComplete(rand, true)) {
				rc.move(rand);
				prevSpot = directions.get((directions.indexOf(rand) + 4) % 8);
				return true;
			}
			count++;
		}
		return false;
	}

	protected boolean moveAwayFromHQ(Direction dir) {
		MapLocation loc = rc.getLocation();
		MapLocation adjLoc = rc.adjacentLocation(dir);
		if(loc.x <= HQs[0].x + 1 && loc.x >= HQs[0].x - 2 && loc.x <= HQs[0].y + 1 && loc.y >= HQs[0].y - 1) {
			return Math.abs(directions.indexOf(dir) - directions.indexOf(getDirection(HQs[0], loc)) - 4) <= 2;
		}
		return adjLoc.x <= HQs[0].x + 1 && adjLoc.x >= HQs[0].x - 2 && adjLoc.x <= HQs[0].y + 1 && adjLoc.y >= HQs[0].y - 1;
	}

	protected boolean canMoveComplete(Direction moveDir, boolean avoidWalls) throws GameActionException {
		return prevSpot != moveDir && rc.canMove(moveDir) && !rc.senseFlooding(rc.adjacentLocation(moveDir))
				&& (!avoidWalls || avoidWalls(moveDir)) /*&& (buildVap || desSch == null || rc.getType() != RobotType.MINER && moveAwayFromHQ(moveDir))*/;
	}

	protected boolean potFlooding(MapLocation loc) throws GameActionException {
		int round = rc.getRoundNum() + 5;
		int waterLevel = (int) (Math.pow(Math.E, 0.0028*round - 1.38*Math.sin(0.00157*round - 1.73) + 1.38*Math.sin(-1.78)) - 1);
		return rc.senseElevation(loc) <= waterLevel;
	}

	protected boolean avoidWalls(Direction dir) {
		MapLocation loc = rc.adjacentLocation(dir);
		if(loc.x < Math.pow(rc.getType().sensorRadiusSquared, 0.5) || loc.x > rc.getMapWidth() - Math.pow(rc.getType().sensorRadiusSquared, 0.5)) {
			return false;
		} else if (loc.y < Math.pow(rc.getType().sensorRadiusSquared, 0.5) || loc.y > rc.getMapHeight() - Math.pow(rc.getType().sensorRadiusSquared, 0.5)) {
			return false;
		}
		return true;
	}
}
