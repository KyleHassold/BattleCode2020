package phases2Cleaned;

import java.util.ArrayList;
import java.util.HashMap;

import battlecode.common.*;

//-------------------------------------------------- INFO --------------------------------------
/*
BLOCKCHAIN CODES:
117290 = Soup
117291 = HQ Location
117292 = Mine Location
117293 = Design School Location
117294 = Vaporator Location
BLOCKCHAIN PROTOCOL:
for soup:
    [code, x, y, amountOfSoup, -1, -1, -1]
for other stuff:
    [code, x, y, -1, -1, -1, -1]
MAP INT[]:
	Water, Dirt, Soup, Base
	Building:
		Positive: Friendly
		Negative: Enemy
		HQ: 1
		Refinery: 2
		Net Gun: 3
		Vaporator: 4
		Design School: 5
		Fulfillment Center: 6
*/

public abstract class Robot {
	RobotController rc;
	ArrayList<Direction> directions = new ArrayList<Direction>();
	HashMap<MapLocation, int[]> map = new HashMap<MapLocation, int[]>();
	HashMap<MapLocation, Integer> soup = new HashMap<MapLocation, Integer>();
	MapLocation[] HQs = new MapLocation[2];
	MapLocation[] refs = new MapLocation[2];
	MapLocation desSch;
	MapLocation vaporator;
	MapLocation senseStopLoc = new MapLocation(-2,-2);
	int mapH;
	int mapW;
	int phase = 1;

	protected Robot(RobotController rc) {
		this.rc = rc;
		directions.add(Direction.NORTH);
		directions.add(Direction.NORTHEAST);
		directions.add(Direction.EAST);
		directions.add(Direction.SOUTHEAST);
		directions.add(Direction.SOUTH);
		directions.add(Direction.SOUTHWEST);
		directions.add(Direction.WEST);
		directions.add(Direction.NORTHWEST);
		mapH = rc.getMapHeight();
		mapW = rc.getMapWidth();
	}

	protected abstract void run() throws GameActionException;

	// Building robots
	protected boolean buildRobot(RobotType rType, Direction prefDir) throws GameActionException {
		if(rc.getTeamSoup() < rType.cost) {
			return false;
		}

		int[] baseDir = {0,1,2,-1,-2,3,-3,4};
		int dirI = directions.indexOf(prefDir);

		Direction dir;
		for(int dDir : baseDir) {
			dir = directions.get((dirI + dDir + 8) % 8);
			if(rc.canBuildRobot(rType, dir)) {
				rc.buildRobot(rType, dir);
				return true;
			}
		}
		return false;

	}
	
	protected Direction getDirection(MapLocation start, MapLocation end) {
		if(start.equals(end)) {
			return Direction.CENTER;
		}
		
		double angle = (Math.atan2(end.y - start.y, end.x - start.x))/Math.PI + 1;

		if(angle > 15.0/8 || angle <= 1.0/8) {
			return Direction.WEST;
		} else if(angle > 13.0/8) {
			return Direction.NORTHWEST;
		} else if(angle > 11.0/8) {
			return Direction.NORTH;
		} else if(angle > 9.0/8) {
			return Direction.NORTHEAST;
		} else if(angle > 7.0/8) {
			return Direction.EAST;
		} else if(angle > 5.0/8) {
			return Direction.SOUTHEAST;
		} else if(angle > 3.0/8) {
			return Direction.SOUTH;
		} else {
			return Direction.SOUTHWEST;
		}
	}
	
	protected int getRSquared(MapLocation start, MapLocation end) {
		return (int) (Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2));
	}
	
	protected MapLocation findAdjacentRobot(RobotType type, Team team) throws GameActionException {
		MapLocation currLoc = rc.getLocation();
		for(Direction dir : directions) {
			if(rc.canSenseLocation(new MapLocation(currLoc.x + dir.dx, currLoc.y + dir.dy))) {
				RobotInfo robo = rc.senseRobotAtLocation(new MapLocation(currLoc.x + dir.dx, currLoc.y + dir.dy));
				if(robo != null && (robo.getType() == type || robo.getType() == RobotType.HQ) && (team == null || robo.getTeam() == team)) {
					return robo.location;
				}
			}
		}
		return null;
	}
	
	protected boolean stopProcessing(MapLocation senseLoc) {
		if(Clock.getBytecodesLeft() < 500) {
			senseStopLoc = new MapLocation(senseLoc.x, senseLoc.y);
			return true;
		}
		return false;
	}
	
	protected void doneProcessing() {
		senseStopLoc = new MapLocation(-1,-1);
	}
	
	// Blockchain
	protected void checkTransactions() throws GameActionException {
		Transaction[] trans = rc.getBlock(rc.getRoundNum()-1);
		for(Transaction t : trans) {
			analyzeTransaction(t);
		}
	}

	protected void analyzeTransaction(Transaction t) {
		MapLocation loc;
		if(t.getMessage()[0] == 21) { // HQs
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			HQs[1] = loc;
			map.put(loc, new int[] {0,0,0,-1});
		} else if(t.getMessage()[0] == 22) { // Refinery
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			if(refs[0] == null) {
				refs[0] = loc;
			} else {
				refs[1] = loc;
			}
			map.put(loc, new int[] {0,0,0,2});
		} else if(t.getMessage()[0] == 117290) { // Soup
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			soup.put(loc, t.getMessage()[3]);
			map.put(loc, new int[] {0,0,t.getMessage()[3],0});
		} else if(t.getMessage()[0] == 117293) { // Design School
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			desSch = loc;
			map.put(loc, new int[] {0,0,0,5});
		} else if(t.getMessage()[0] == 117294) { // Vaporator
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			vaporator = loc;
			map.put(loc, new int[] {0,0,0,4});
		}
	}
}
