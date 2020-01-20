package droneRush;

import java.util.Collections;
import java.util.List;

import battlecode.common.*;

public abstract class Unit extends Robot {
	MapLocation target;
	Direction prevSpot;

	protected Unit(RobotController rc) {
		super(rc);
	}

	// Moving
	protected boolean moveCloser(MapLocation target, boolean avoid) throws GameActionException {
		if(!rc.isReady()) {
			return false;
		}

		int[] baseMove = {0,1,2,-1,-2,3,-3,4};
		int dir = directions.indexOf(rc.getLocation().directionTo(target));
		if(dir == -1) { // On spot already
			return false;
		}

		Direction moveDir;
		for(int dDir : baseMove) {
			moveDir = directions.get((dir + dDir + 8) % 8);
			if(canMoveComplete(moveDir, avoid, target)) {
				rc.move(moveDir);
				prevSpot = moveDir.opposite();
				loc = rc.getLocation();
				return true;
			}
		}
		return false;
	}

	protected boolean moveRandom() throws GameActionException {
		@SuppressWarnings("unchecked")
		List<Direction> rand = (List<Direction>) directions.clone();
		Collections.shuffle(rand);
		for(Direction dir : rand) {
			if(canMoveComplete(dir, true, null)) {
				rc.move(dir);
				loc = rc.getLocation();
				prevSpot = dir.opposite();
				return true;
			}
		}
		return false;
	}

	protected boolean canMoveComplete(Direction moveDir, boolean avoidWalls, MapLocation target) throws GameActionException {
		System.out.println(moveDir + ": " + moveAwayFromHQ(moveDir));
		if(ref != null && rc.getType() == RobotType.MINER && !moveAwayFromHQ(moveDir)) {
			return false;
		}
		if(rc.getType() != RobotType.DELIVERY_DRONE) {
			return prevSpot != moveDir && rc.canMove(moveDir) && !rc.senseFlooding(rc.adjacentLocation(moveDir))
				&& (!avoidWalls || avoidWalls(moveDir));
		} else {
			return prevSpot != moveDir && rc.canMove(moveDir) && (!avoidWalls || !rc.adjacentLocation(moveDir).isWithinDistanceSquared(target, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED));
		}
	}
	
	private boolean moveAwayFromHQ(Direction dir) throws GameActionException {
		MapLocation adj = loc.translate(dir.dx, dir.dy);
		if(rc.canSenseLocation(loc.translate(0, 1)) && rc.senseRobotAtLocation(loc.translate(0, 1)) != null && rc.senseRobotAtLocation(loc.translate(0, 1)).type == RobotType.VAPORATOR && dir == Direction.SOUTH) {
			return true;
		} else if(rc.canSenseLocation(loc.translate(0, 2)) && rc.senseRobotAtLocation(loc.translate(0, 2)) != null && rc.senseRobotAtLocation(loc.translate(0, 2)).type == RobotType.VAPORATOR && dir == Direction.SOUTH) {
			return true;
		}
		if(adj.x <= HQs[0].x + 2 && adj.x >= HQs[0].x - 3 && adj.y <= HQs[0].y + 2 && adj.y >= HQs[0].y - 2) {
			return false;
		}
		return true;
	}

	protected boolean potFlooding(MapLocation loc) throws GameActionException {
		int round = rc.getRoundNum() + 5;
		int waterLevel = (int) (Math.pow(Math.E, 0.0028*round - 1.38*Math.sin(0.00157*round - 1.73) + 1.38*Math.sin(-1.78)) - 1);
		return rc.senseElevation(loc) <= waterLevel;
	}

	protected boolean avoidWalls(Direction dir) {
		MapLocation adj = rc.adjacentLocation(dir);
		if(adj.x < Math.min(loc.x, Math.pow(rc.getType().sensorRadiusSquared, 0.5)) || adj.x > Math.max(loc.x, rc.getMapWidth() - Math.pow(rc.getType().sensorRadiusSquared, 0.5))) {
			return false;
		} else if (adj.y < Math.min(loc.y, Math.pow(rc.getType().sensorRadiusSquared, 0.5)) || adj.y > Math.max(loc.y, rc.getMapHeight() - Math.pow(rc.getType().sensorRadiusSquared, 0.5))) {
			return false;
		}
		return true;
	}
}
