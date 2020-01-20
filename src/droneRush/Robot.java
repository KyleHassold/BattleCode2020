package droneRush;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
	Set<MapLocation> soup = new HashSet<MapLocation>();
	MapLocation[] HQs = new MapLocation[2];
	MapLocation ref;
	MapLocation vaporator;
	MapLocation fulCent;
	MapLocation desSch;
	MapLocation loc;
	MapLocation center;
	List<MapLocation[]> moveReqs = new ArrayList<MapLocation[]>();
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
		for(RobotInfo robo : rc.senseNearbyRobots()) {
			if(robo.type == RobotType.HQ && robo.team == rc.getTeam()) {
				HQs[0] = robo.location;
				break;
			}
		}
		if(HQs[0] == null) {
			System.out.println("Failed to locate HQ");
		}
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
				if(robo != null && (robo.type == type || (robo.type == RobotType.REFINERY && robo.type == RobotType.HQ)) && (team == null || robo.team == team)) {
					return robo.location;
				}
			}
		}
		return null;
	}
	
	protected MapLocation bestSoup() {
		MapLocation bestSoup = null;
		int bestScore = 0;
		for(MapLocation s : soup) {
			int temp = loc.distanceSquaredTo(s) + s.distanceSquaredTo(ref == null ? HQs[0] : ref);
			if(bestSoup == null || bestScore > temp) {
				bestSoup = s;
				bestScore = temp;
			}
		}
		
		return bestSoup;
	}
	
	protected void yield() throws GameActionException {
		Clock.yield();
		checkTransactions();
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
			System.out.println("Soup Message Recieved!");
		} else if(t.getMessage()[0] == 117291) { // HQs
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			HQs[1] = loc;
			map.put(loc, new int[] {0,0,0,-1});
			System.out.println("HQ Message Recieved!");
		} else if(t.getMessage()[0] == 117292) { // Refinery
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			ref = loc;
			map.put(loc, new int[] {0,0,0,2});
			System.out.println("Refinery Message Recieved!");
		} else if(t.getMessage()[0] == 117293) { // Vaporator
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			vaporator = loc;
			map.put(loc, new int[] {0,0,0,3});
			System.out.println("Vaporator Message Recieved!");
		} else if(t.getMessage()[0] == 117294) { // Fulfillment Center
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			vaporator = loc;
			map.put(loc, new int[] {0,0,0,4});
			System.out.println("Fulfillment Center Message Recieved!");
		} else if(t.getMessage()[0] == 117295) { // Design School
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			desSch = loc;
			map.put(loc, new int[] {0,0,0,5});
			System.out.println("Design School Message Recieved!");
		} else if(t.getMessage()[0] == 117299) { // Requires Movement
			MapLocation start = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			MapLocation end = null;
			if(t.getMessage()[3] != -1) {
				end = new MapLocation(t.getMessage()[3], t.getMessage()[4]);
			}
			moveReqs.add(new MapLocation[] {start, end});
		}
	}
}
