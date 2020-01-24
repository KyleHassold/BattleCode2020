package droneRush;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import battlecode.common.*;

public abstract class Unit extends Robot {
	MapLocation target;
	List<MapLocation> path = new ArrayList<MapLocation>();
	Direction prevSpot;
	List<MapLocation> landscaperSpots = new ArrayList<MapLocation>();
	Random rand = new Random();

	protected Unit(RobotController rc) {
		super(rc);
		
        int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4};
		int dir = directions.indexOf(center.directionTo(HQs[0]));
		if(isCardinalDir(directions.get(dir))) {
			dir = (dir + 1) % 8;
		}
		MapLocation spot;
		for(int dirOffset : offsets) {
			spot = HQs[0].translate(directions.get((dir + dirOffset + 8) % 8).dx, directions.get((dir + dirOffset + 8) % 8).dy);
			if(rc.onTheMap(spot)) {
				landscaperSpots.add(spot);
			}
		}
	}
	
	// Moving
	protected boolean pathFindTo(MapLocation target, int moveLimit, boolean avoid, String distance) {
        int giveUp = 0;

		while(giveUp < moveLimit && !pathFindToOne(target, avoid, distance)) {
			giveUp++;
			yield();
		}
		path.clear();
		return giveUp < moveLimit;
	}
	
	protected boolean findSoup(boolean farSoup) {
		MapLocation randSpot = getRandSpot();
		rc.setIndicatorDot(randSpot, 255, 0, 0);
		int giveUp = 0;
		int limit = (int) Math.pow(loc.distanceSquaredTo(randSpot), 0.6);
		System.out.println("Limit: " + limit);
		boolean found = false;
		
		while((!farSoup && soup.isEmpty()) || (farSoup && bestSoup(30) == null)) {
			found = pathFindToOne(randSpot, true, "On");
			giveUp++;
			if(giveUp > limit || found) {
				randSpot = getRandSpot();
				rc.setIndicatorDot(randSpot, 255, 0, 0);
				giveUp = 0;
				found = false;
				limit = (int) Math.pow(loc.distanceSquaredTo(randSpot), 0.6);
				System.out.println("Limit: " + limit);
				path.clear();
			}
			yield();
		}
		
		return false;
	}
	
	protected boolean pathFindToOne(MapLocation target, boolean avoid, String distance) {
		if(((distance.equals("On") && loc.equals(target)) || (distance.equals("Adj") && loc.isAdjacentTo(target) && !loc.equals(target)) || (distance.equals("In Range") && rc.canSenseLocation(target)))) {
			return true;
		}
		
        int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4};
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
				move(moveDir);
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
		return ((distance.equals("On") && loc.equals(target)) || (distance.equals("Adj") && loc.isAdjacentTo(target) && !loc.equals(target)) || (distance.equals("In Range") && rc.canSenseLocation(target)));
	}
	
	private void move(Direction dir) throws GameActionException {
		rc.move(dir);
		path.add(loc);
		if(path.size() > 15) {
			path.remove(0);
		}
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
	
	// Moving Aux
	private MapLocation getRandSpot() {
		int x = rand.nextInt(center.x - (int) Math.pow(rc.getType().sensorRadiusSquared, 0.5)) + (int) Math.pow(rc.getType().sensorRadiusSquared, 0.5);
		int y = rand.nextInt(center.y - (int) Math.pow(rc.getType().sensorRadiusSquared, 0.5)) + (int) Math.pow(rc.getType().sensorRadiusSquared, 0.5);
		double quarter = rand.nextDouble();
		
		if(quarter < 0.4) {
			if(loc.x < center.x) {
				x += center.x - (int) Math.pow(rc.getType().sensorRadiusSquared, 0.5);
			}
		} else if(quarter < 0.8) {
			if(loc.y < center.y) {
				y += center.y - (int) Math.pow(rc.getType().sensorRadiusSquared, 0.5);
			}
		} else {
			if(loc.x < center.x) {
				x += center.x - (int) Math.pow(rc.getType().sensorRadiusSquared, 0.5);
			}
			if(loc.y < center.y) {
				y += center.y - (int) Math.pow(rc.getType().sensorRadiusSquared, 0.5);
			}
		}
		
		return new MapLocation(x, y);
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
	
	protected boolean moveAwayFromHQ(Direction dir) throws GameActionException {
		MapLocation adj = loc.translate(dir.dx, dir.dy);
		if(adj.x <= HQs[0].x + 2 && adj.x >= HQs[0].x - 2 && adj.y <= HQs[0].y + 2 && adj.y >= HQs[0].y - 2) {
			Direction awayDir = HQs[0].directionTo(loc);
			if(dir.equals(awayDir) || dir.equals(awayDir.rotateRight()) || dir.equals(awayDir.rotateLeft())) {
				return true;
			}
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
