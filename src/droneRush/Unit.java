package droneRush;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import battlecode.common.*;

public abstract class Unit extends Robot {
	List<MapLocation> path = new ArrayList<MapLocation>();
	List<MapLocation> landscaperSpots = new ArrayList<MapLocation>();
	MapLocation target;
	Random rand = new Random();

	protected Unit(RobotController rc) {
		super(rc);
		
        Direction prefDir = center.directionTo(HQs[0]);
		if(isCardinalDir(prefDir)) {
			prefDir = prefDir.rotateRight();
		}
		
		MapLocation spot;
		for(Direction dir : getPrefDir(prefDir)) {
			spot = HQs[0].translate(dir.dx, dir.dy);
			if(rc.onTheMap(spot)) {
				landscaperSpots.add(spot);
			}
		}
	}
	
	//----- Moving -----//
	
	/*
	 * Use the bread crumb path finding algorithm
	 * to move to the "target" within "moveLimit" number of moves
	 * but avoiding certain issues if "avoid" is true
	 * Only get as close to the target as "distance" specifies
	 * Returns true if the goal was achieved
	 */
	protected boolean pathFindTo(MapLocation target, int moveLimit, boolean avoid, String distance) {
        int giveUp = 0;

		while(giveUp < moveLimit && !pathFindToOne(target, avoid, distance)) {
			giveUp++;
			yield();
		}
		
		path.clear();
		
		if(giveUp >= moveLimit) {
			System.out.println("Failure: Unit.pathFindTo(" + target + ", " + moveLimit + ", " + avoid + ", " + distance + ")\nFailed to move to target");
		}
		return giveUp < moveLimit;
	}
	
	/*
	 * Selects a random spot using the getRandSpot() function
	 * Moves until on that spot or a it give up
	 * Keep moving to new random spots until soup is found
	 */
	protected void findSoup(boolean farSoup) {
		MapLocation randSpot = getRandSpot();
		rc.setIndicatorDot(randSpot, 255, 0, 0);
		int giveUp = 0;
		int limit = (int) Math.pow(loc.distanceSquaredTo(randSpot), 0.6);
		
		while((!farSoup && soup.isEmpty()) || (farSoup && bestSoup(30) == null)) {
			if(giveUp > limit || pathFindToOne(randSpot, true, "On")) {
				randSpot = getRandSpot();
				rc.setIndicatorDot(randSpot, 255, 0, 0);
				giveUp = 0;
				limit = (int) Math.pow(loc.distanceSquaredTo(randSpot), 0.6);
				path.clear();
			}
			
			giveUp++;
			yield();
		}
	}
	
	/*
	 * Conducts one movement using the bread crumb path finding algorithm
	 * Returns true if in the proper range of the target
	 */
	protected boolean pathFindToOne(MapLocation target, boolean avoid, String distance) {
		if(atGoal(target, distance)) {
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
				if(canMoveComplete(moveDir, avoid, target)) {
					break;
				}
			}
		}

		if(rc.canMove(moveDir)) {
			move(moveDir);
		}
		return atGoal(target, distance);
	}
	
	/*
	 * Moves the robot in the given direction
	 * Saves the previous location to the path
	 * Senses for soup if it's a miner
	 */
	private void move(Direction dir) {
		try {
			rc.move(dir);
		} catch (GameActionException e) {
			System.out.println("Error: Unit.move() Failed!\nrc.move(" + dir + ") Failed!");
			e.printStackTrace();
		}
		
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
					
					int soupAmount = 1;
					if(rc.canSenseLocation(s)) {
						try {
							soupAmount = rc.senseSoup(s);
						} catch (GameActionException e) {
							System.out.println("Error: Unit.move() Failed!\nrc.senseSoup(" + s + ") Failed!");
							e.printStackTrace();
						}
					}
					
					map.put(s, new int[] {0, 0, soupAmount, 0});
					
					submitTransaction(new int[] {teamCode, s.x, s.y, map.get(s)[2], -1, -1, 0}, 1, false);
				}
			}
		}
	}
	
	//----- Moving Auxiliary Functions -----//
	
	/*
	 * Calculates a random spot in a different quarter of the map
	 * Excludes locations within sensing distance of the wall as to not waste sensing
	 * Returns the MapLocation of the random spot
	 */
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
	
	/*
	 * Checks if a unit is currently at their movement goal
	 */
	protected boolean atGoal(MapLocation target, String distance) {
		return ((distance.equals("On") && loc.equals(target)) || (distance.equals("Adj") && loc.isAdjacentTo(target) && !loc.equals(target)) || (distance.equals("In Range") && rc.canSenseLocation(target)));
	}
	
	/*
	 * Checks if a move can be made based on various determinants
	 * Should be edited to better determine the value/ability to move
	 * Should include enemy drones for miners and landscapers and potential flooding
	 */
	private boolean canMoveComplete(Direction moveDir, boolean avoidWalls, MapLocation target) {
		if(ref != null && rc.getType() == RobotType.MINER && !moveAwayFromHQ(moveDir)) {
			return false;
		}
		if(rc.getType() != RobotType.DELIVERY_DRONE) {
			try {
				return rc.canMove(moveDir) && !rc.senseFlooding(rc.adjacentLocation(moveDir))
					&& (!avoidWalls || avoidWalls(moveDir));
			} catch (GameActionException e) {
				System.out.println("Error: Unit.canMoveComplete(" + moveDir + ", " + avoidWalls + ", " + target + ") Failed!\nrc.senseFlooding() Failed!");
				e.printStackTrace();
				return false;
			}
		} else {
			return rc.canMove(moveDir) && (!avoidWalls || !rc.adjacentLocation(moveDir).isWithinDistanceSquared(target, GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED));
		}
	}
	
	/*
	 * Checks if the movement will get in the way of things going on around the HQ
	 * or, if already near the HQ, if it moves the unit way
	 */
	protected boolean moveAwayFromHQ(Direction dir) {
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
	/*
	 * Checks if the give MapLocation will be flooded in 5 turns
	 */
	@SuppressWarnings("unused")
	private boolean potFlooding(MapLocation loc) {
		int round = rc.getRoundNum() + 5;
		int waterLevel = (int) (Math.pow(Math.E, 0.0028*round - 1.38*Math.sin(0.00157*round - 1.73) + 1.38*Math.sin(-1.78)) - 1);
		try {
			return rc.senseElevation(loc) <= waterLevel;
		} catch (GameActionException e) {
			System.out.println("Error: Unit.potFlooding(" + loc + ") Failed!\nrc.senseElevation() Failed!");
			e.printStackTrace();
			return false;
		}
	}
	
	/*
	 * Checks if movement is towards wall or moving out on wall zone
	 */
	private boolean avoidWalls(Direction dir) {
		MapLocation adj = rc.adjacentLocation(dir);
		if(adj.x < Math.min(loc.x, Math.pow(rc.getType().sensorRadiusSquared, 0.5)) || adj.x > Math.max(loc.x, rc.getMapWidth() - Math.pow(rc.getType().sensorRadiusSquared, 0.5))) {
			return false;
		} else if (adj.y < Math.min(loc.y, Math.pow(rc.getType().sensorRadiusSquared, 0.5)) || adj.y > Math.max(loc.y, rc.getMapHeight() - Math.pow(rc.getType().sensorRadiusSquared, 0.5))) {
			return false;
		}
		return true;
	}
}