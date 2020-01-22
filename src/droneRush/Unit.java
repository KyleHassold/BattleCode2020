package droneRush;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import battlecode.common.*;

public abstract class Unit extends Robot {
	MapLocation target;
	Direction prevSpot;
	List<MapLocation> landscaperSpots = new ArrayList<MapLocation>();
	List<Direction> landscaperMining = new ArrayList<Direction>();

	protected Unit(RobotController rc) {
		super(rc);
		landscaperSpots.add(new MapLocation(HQs[0].x + 1, HQs[0].y));
		landscaperSpots.add(new MapLocation(HQs[0].x - 1, HQs[0].y + 1));
		landscaperSpots.add(new MapLocation(HQs[0].x, HQs[0].y - 1));
		landscaperSpots.add(new MapLocation(HQs[0].x - 2, HQs[0].y));
		landscaperSpots.add(new MapLocation(HQs[0].x - 1, HQs[0].y - 1));
		landscaperSpots.add(new MapLocation(HQs[0].x, HQs[0].y + 1));
		landscaperSpots.add(new MapLocation(HQs[0].x + 1, HQs[0].y + 1));
		landscaperSpots.add(new MapLocation(HQs[0].x + 1, HQs[0].y - 1));
		landscaperSpots.add(new MapLocation(HQs[0].x - 2, HQs[0].y - 1));
		landscaperSpots.add(new MapLocation(HQs[0].x - 2, HQs[0].y + 1));
		landscaperMining.add(Direction.EAST);
		landscaperMining.add(Direction.NORTH);
		landscaperMining.add(Direction.SOUTH);
		landscaperMining.add(Direction.WEST);
		landscaperMining.add(Direction.SOUTH);
		landscaperMining.add(Direction.NORTH);
		landscaperMining.add(Direction.NORTH);
		landscaperMining.add(Direction.SOUTH);
		landscaperMining.add(Direction.WEST);
		landscaperMining.add(Direction.WEST);
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
				prevSpot = dir.opposite();
				move(dir);
				return true;
			}
		}
		return false;
	}
	
	protected boolean pathFindTo(MapLocation target, int moveLimit, boolean avoid, String distance) {
		List<MapLocation> path = new ArrayList<MapLocation>();
        int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4};
        int giveUp = 0;

		while(giveUp < moveLimit && ((distance.equals("On") && !loc.equals(target)) || (distance.equals("Adj") && !loc.isAdjacentTo(target)) || (distance.equals("In Range") && !rc.canSenseLocation(target)))) {
			int baseDir = directions.indexOf(loc.directionTo(target));
			Direction moveDir = Direction.CENTER;
			MapLocation moveLoc;
			for(int offset : offsets) {
				moveDir = directions.get((baseDir + offset + 8) % 8);
				moveLoc = loc.translate(moveDir.dx, moveDir.dy);
				if(Collections.frequency(path, moveLoc) < 2) {
					try {
						if(canMoveComplete(moveDir, avoid, target)) {
							break;
						}
					} catch (GameActionException e) {
						e.printStackTrace();
					}
				}
			}
			
			if(rc.canMove(moveDir)) {
				try {
					path.add(loc);
					move(moveDir);
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				yield();
			} catch (GameActionException e) {
				e.printStackTrace();
			}
			giveUp++;
		}
		return giveUp < moveLimit;
	}
	
	private void move(Direction dir) throws GameActionException {
		rc.move(dir);
		loc = rc.getLocation();
		if(rc.getType() == RobotType.MINER) {
			MapLocation[] newSoup = rc.senseNearbySoup();
			for(MapLocation s : newSoup) {
				if(!map.containsKey(s)) {
					soup.add(s);
					int soupAmount = rc.canSenseLocation(s) ? rc.senseSoup(s) : 1;
					map.put(s, new int[] {0, 0, soupAmount, 0});
					
					int[] message = new int[7];
					message[0] = 117290;
					message[1] = s.x;
					message[2] = s.y;
					message[3] = map.get(s)[2];
					if(rc.canSubmitTransaction(message, 1)) {
						rc.submitTransaction(message, 1);
					}
				}
			}
		}
	}

	private boolean canMoveComplete(Direction moveDir, boolean avoidWalls, MapLocation target) throws GameActionException {
		if(ref != null && rc.getType() == RobotType.MINER && !moveAwayFromHQ(moveDir)) {
			return false;
		}
		if(rc.getType() != RobotType.DELIVERY_DRONE) {
			return rc.canMove(moveDir) && !rc.senseFlooding(rc.adjacentLocation(moveDir))
				&& (!avoidWalls || avoidWalls(moveDir));
		} else {
			return rc.canMove(moveDir) && (!avoidWalls || !rc.adjacentLocation(moveDir).isWithinDistanceSquared(target, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED));
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
