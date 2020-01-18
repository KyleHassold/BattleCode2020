package droneRush;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeSet;

import battlecode.common.*;

//-------------------------------------------------- INFO --------------------------------------
/*
BLOCKCHAIN CODES:
117290 = Soup
117291 = HQ Location
117292 = Refinery Location
117293 = Vaporator Location
117294 = Fulfillment Center Location
117295 = Design School Location
117296 = Net Gun Location
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
		Vaporator: 3
		Fulfillment Center: 4
		Design School: 5
		Net Gun: 6
 */

public abstract class Robot {
	// Variables for all Robots
	RobotController rc;
	ArrayList<Direction> directions = new ArrayList<Direction>();
	HashMap<MapLocation, int[]> map = new HashMap<MapLocation, int[]>();
	TreeSet<MapLocation> soup = new TreeSet<MapLocation>(new SoupComparator());
	MapLocation[] HQs = new MapLocation[2];
	MapLocation ref;
	MapLocation vaporator;
	MapLocation fulCent;
	MapLocation desSch;
	MapLocation loc;
	MapLocation center;
	int mapH;
	int mapW;


	// Super constructor
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
		loc = rc.getLocation();
		center = new MapLocation((mapW - 1) /2, (mapH - 1) / 2);
	}

	// Force all sub-classes to implement a run() method
	protected abstract void run() throws GameActionException;

	// Methods for all Robots
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

	protected MapLocation findAdjacentRobot(RobotType type, Team team) throws GameActionException {
		for(Direction dir : directions) {
			MapLocation adj = rc.adjacentLocation(dir);
			if(rc.canSenseLocation(adj)) {
				RobotInfo robo = rc.senseRobotAtLocation(adj);
				if(robo != null && (robo.getType() == type || robo.getType() == RobotType.HQ) && (team == null || robo.getTeam() == team)) {
					return robo.location;
				}
			}
		}
		return null;
	}
	
	protected void yield() throws GameActionException {
		while(rc.getCooldownTurns() >= 1) {
			Clock.yield();
			checkTransactions();
		}
	}

	// Blockchain
	protected void checkTransactions() throws GameActionException {
		Transaction[] trans = rc.getBlock(rc.getRoundNum()-1);
		for(Transaction t : trans) {
			analyzeTransaction(t);
		}
	}

	private void analyzeTransaction(Transaction t) {
		MapLocation loc;
		if(t.getMessage()[0] == 117290) { // Soup
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			if(!map.containsKey(loc)) {
				soup.add(loc);
				map.put(loc, new int[] {0,0,t.getMessage()[3],0});
			}
			System.out.println("Message Recieved!");
		} else if(t.getMessage()[0] == 117291) { // HQs
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			HQs[1] = loc;
			map.put(loc, new int[] {0,0,0,-1});
		} else if(t.getMessage()[0] == 117292) { // Refinery
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			ref = loc;
			map.put(loc, new int[] {0,0,0,2});
		} else if(t.getMessage()[0] == 117293) { // Vaporator
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			vaporator = loc;
			map.put(loc, new int[] {0,0,0,3});
		} else if(t.getMessage()[0] == 117294) { // Fulfillment Center
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			vaporator = loc;
			map.put(loc, new int[] {0,0,0,4});
		} else if(t.getMessage()[0] == 117295) { // Design School
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			desSch = loc;
			map.put(loc, new int[] {0,0,0,5});
		}
	}
	
	class SoupComparator implements Comparator<MapLocation>{

		@Override
		public int compare(MapLocation loc1, MapLocation loc2) {
			// Factors in robots distance to soup and soups distance to HQ
			if(HQs[0] != null) {
				return (loc1.distanceSquaredTo(loc) - loc2.distanceSquaredTo(loc)) + (loc1.distanceSquaredTo(HQs[0]) - loc2.distanceSquaredTo(HQs[0]));
			}
			return loc1.distanceSquaredTo(loc) - loc2.distanceSquaredTo(loc);
		}
	}
}
