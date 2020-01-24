package phases2Cleaned;

import java.util.HashMap;

import battlecode.common.*;

public abstract class Unit extends Robot {
	Direction prevSpot;
	MapLocation prevLoc = new MapLocation(-30, -30);
	int prevRadius = 0;

	protected Unit(RobotController rc) {
		super(rc);
	}
	
	// Sensing
	protected HashMap<MapLocation, Integer> newSensor() throws GameActionException {
		System.out.println("Sense");
		HashMap<MapLocation, Integer> results = new HashMap<MapLocation, Integer>();
		int currRadius = rc.getCurrentSensorRadiusSquared();
		
		if(currRadius <= prevRadius && prevSpot != null) { // If you have moved and the sensor hasnt been changed
			Direction prevMove = directions.get((directions.indexOf(prevSpot) + 4) % 8);
			if(prevMove == Direction.EAST || prevMove == Direction.WEST) {
				results = senseHor(prevMove.dx, currRadius);
			} else if(prevMove == Direction.NORTH || prevMove == Direction.SOUTH) {
				results = senseVir(prevMove.dy, currRadius);
			} else {
				results = senseDia(prevMove.dx, prevMove.dy, currRadius);
			}
		} else { // Sense entire surroundings
			MapLocation loc = rc.getLocation();
			for(int x = Math.max(loc.x - (int) Math.pow(currRadius, 0.5), Math.max(0, senseStopLoc.x)); x <= Math.min(loc.x + (int) Math.pow(currRadius, 0.5), mapW); x++) {
				for(int y = Math.max(loc.y - (int) (Math.pow(currRadius - Math.pow(loc.x - x, 2), 0.5)), 0); y <= Math.min(loc.y + (int) (Math.pow(currRadius - Math.pow(loc.x - x, 2), 0.5)), mapH); y++) {
					if(x == senseStopLoc.x) {
						y = Math.max(y, senseStopLoc.y + 1);
					}
					rc.setIndicatorDot(new MapLocation(x, y), 0, 255, 255);
					if(prevRadius < Math.pow(prevLoc.x - x, 2) + Math.pow(prevLoc.y - y, 2)) {
						MapLocation senseSpot = new MapLocation(x, y);
						if(!map.containsKey(senseSpot) && rc.canSenseLocation(senseSpot)) {
							rc.setIndicatorDot(senseSpot, 255, 0, 0);
							int soupAmount = rc.senseSoup(senseSpot);
							map.put(senseSpot, new int[] {});
							if(soupAmount > 0 || (soupAmount == 0 && soup.get(senseSpot) != null)) {
								System.out.println("Found Soup");
								soup.put(senseSpot, soupAmount);
								results.put(senseSpot, soupAmount);
							}
						}
						if(stopProcessing(senseSpot)) {
							return results;
						}
					}
				}
			}
		}
		System.out.println("Finished Sensing");
		prevRadius = currRadius;
		doneProcessing();
		
		return results;
	}

	private HashMap<MapLocation, Integer> senseHor(int dx, int currRadius) throws GameActionException {
		HashMap<MapLocation, Integer> results = new HashMap<MapLocation, Integer>();
		MapLocation loc = rc.getLocation();
		for(int y = Math.max(loc.y - (int) Math.pow(currRadius, 0.5), 0); y <= Math.min(loc.y + (int) Math.pow(currRadius, 0.5), mapH); y++) {
			int x = Math.max(loc.x + ((int) (Math.pow(currRadius - Math.pow(loc.y - y, 2), 0.5)) * dx), 0);
			rc.setIndicatorDot(new MapLocation(x, y), 0, 255, 255);
			if(prevRadius < Math.pow(prevLoc.x - x, 2) + Math.pow(prevLoc.y - y, 2)) {
				MapLocation senseSpot = new MapLocation(x, y);
				if(!map.containsKey(senseSpot) && rc.canSenseLocation(senseSpot)) {
					rc.setIndicatorDot(senseSpot, 255, 0, 0);
					int soupAmount = rc.senseSoup(senseSpot);
					map.put(senseSpot, new int[] {});
					if(soupAmount > 0 || (soupAmount == 0 && soup.get(senseSpot) != null)) {
						System.out.println("Found Soup");
						soup.put(senseSpot, soupAmount);
						results.put(senseSpot, soupAmount);
					}
				}
			}
		}
		
		return results;
	}

	private HashMap<MapLocation, Integer> senseVir(int dy, int currRadius) throws GameActionException {
		HashMap<MapLocation, Integer> results = new HashMap<MapLocation, Integer>();
		MapLocation loc = rc.getLocation();
		for(int x = Math.max(loc.x - (int) Math.pow(currRadius, 0.5), 0); x <= Math.min(loc.x + (int) Math.pow(currRadius, 0.5), mapW); x++) {
			int y = Math.max(loc.y + ((int) (Math.pow(currRadius - Math.pow(loc.x - x, 2), 0.5)) * dy), 0);
			rc.setIndicatorDot(new MapLocation(x, y), 0, 255, 255);
			if(prevRadius < Math.pow(prevLoc.x - x, 2) + Math.pow(prevLoc.y - y, 2)) {
				MapLocation senseSpot = new MapLocation(x, y);
				if(!map.containsKey(senseSpot) && rc.canSenseLocation(senseSpot)) {
					rc.setIndicatorDot(senseSpot, 255, 0, 0);
					int soupAmount = rc.senseSoup(senseSpot);
					map.put(senseSpot, new int[] {});
					if(soupAmount > 0 || (soupAmount == 0 && soup.get(senseSpot) != null)) {
						System.out.println("Found Soup");
						soup.put(senseSpot, soupAmount);
						results.put(senseSpot, soupAmount);
					}
				}
			}
		}

		return results;
	}

	private HashMap<MapLocation, Integer> senseDia(int dx, int dy, int currRadius) throws GameActionException {
		HashMap<MapLocation, Integer> results = new HashMap<MapLocation, Integer>();
		MapLocation loc = rc.getLocation();
		
		for(int x = Math.max(loc.x - (int) Math.pow(currRadius, 0.5), 0); x <= Math.min(loc.x + (int) Math.pow(currRadius, 0.5), mapW); x++) {
			int y = Math.max(loc.y + ((int) (Math.pow(currRadius - Math.pow(loc.x - x, 2), 0.5)) * dy), 0);
			rc.setIndicatorDot(new MapLocation(x, y), 0, 255, 255);
			if(prevRadius < Math.pow(prevLoc.x - x, 2) + Math.pow(prevLoc.y - y, 2)) {
				MapLocation senseSpot = new MapLocation(x, y);
				if(!map.containsKey(senseSpot) && rc.canSenseLocation(senseSpot)) {
					rc.setIndicatorDot(senseSpot, 255, 0, 0);
					int soupAmount = rc.senseSoup(senseSpot);
					map.put(senseSpot, new int[] {});
					if(soupAmount > 0 || (soupAmount == 0 && soup.get(senseSpot) != null)) {
						System.out.println("Found Soup");
						soup.put(senseSpot, soupAmount);
						results.put(senseSpot, soupAmount);
					}
				}
			}
		}
		
		int x = loc.x + ((int) (Math.pow(currRadius, 0.5)) * dx);
		if(x < 0 || x > mapW - 1) {
			return results;
		}
		int dyMax = (int) (Math.pow(currRadius - Math.pow(loc.x - x, 2), 0.5));
		for(int y = loc.y - dyMax; y <= loc.y + dyMax; y++) {
			rc.setIndicatorDot(new MapLocation(x, y), 0, 255, 255);
			if(prevRadius < Math.pow(prevLoc.x - x, 2) + Math.pow(prevLoc.y - y, 2)) {
				MapLocation senseSpot = new MapLocation(x, y);
				if(!map.containsKey(senseSpot) && rc.canSenseLocation(senseSpot)) {
					rc.setIndicatorDot(senseSpot, 255, 0, 0);
					int soupAmount = rc.senseSoup(senseSpot);
					map.put(senseSpot, new int[] {});
					if(soupAmount > 0 || (soupAmount == 0 && soup.get(senseSpot) != null)) {
						System.out.println("Found Soup");
						soup.put(senseSpot, soupAmount);
						results.put(senseSpot, soupAmount);
					}
				}
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
				prevLoc = rc.getLocation();
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
				prevLoc = rc.getLocation();
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
