package droneRush;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import battlecode.common.*;

//-------------------------------------------------- INFO --------------------------------------
/*
BLOCKCHAIN CODES:
0 = Soup
1 = HQ Location
2 = Refinery Location
3 = Vaporator Location
4 = Fulfillment Center Location
5 = Design School Location
6 = Net Gun Location
8 = Terraformer
9 = Move Request

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
	boolean terraformer = false;
	int mapH;
	int mapW;
	int teamCode;
	int phase;


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
		
		teamCode = 123789;
		
		loc = rc.getLocation();
		mapH = rc.getMapHeight();
		mapW = rc.getMapWidth();
		center = new MapLocation((mapW - 1) /2, (mapH - 1) / 2);
		
		//Find HQ
		for(RobotInfo robo : rc.senseNearbyRobots()) {
			if(robo.type == RobotType.HQ && robo.team == rc.getTeam()) {
				HQs[0] = robo.location;
				break;
			}
		}
		if(HQs[0] == null) {
			System.out.println("Failure: Robot.Robot()\nFailed to sense HQ");
		}
		checkTransactions();
	}

	//---- Methods for all Robots -----//

	// Force all sub-classes to implement a run() method
	protected abstract void run();
	
	/*
	 * Attempts to build a robot of the given type
	 * Prioritizes the give Direction but will check all
	 * Returns true if successful and false otherwise
	 */
	protected boolean buildRobot(RobotType rType, Direction prefDir) {
		if(rc.getTeamSoup() < rType.cost) {
			return false;
		}
		
		// Check all directions for building
		Direction[] prefDirs = getPrefDir(prefDir);
		for(Direction dir : prefDirs) {
			if(rc.canBuildRobot(rType, dir)) {
				try {
					rc.buildRobot(rType, dir);
					return true;
				} catch (GameActionException e) {
					System.out.println("Error: Robot.buildRobot(" + rType + ", " + dir + ") Failed!");
					e.printStackTrace();
					return false;
				}
			}
		}
		
		System.out.println("Failure: Robot.buildRobot(" + rType + ", " + prefDir + ")\nFailed to build robot");
		return false;

	}
	
	/*
	 * Searches adjacent tiles for the give robot type and team
	 * If team is null, either team works
	 * Return the found location or null if not found
	 */
	protected MapLocation findAdjRobot(RobotType type, Team team) {
		// Check each direction
		for(Direction dir : directions) {
			MapLocation adj = rc.adjacentLocation(dir);
			
			// Sense the adjacent location
			if(rc.canSenseLocation(adj)) {
				RobotInfo robo = null;
				
				try {
					robo = rc.senseRobotAtLocation(adj);
				} catch (GameActionException e) {
					System.out.println("Error: Robot.findAdjRobot(" + adj + ") Failed!");
					e.printStackTrace();
				}
				
				// If it is the robot being searched for
				if(robo != null && robo.type == type && (team == null || robo.team == team)) {
					return robo.location;
				}
			}
		}
		
		System.out.println("Failure: Robot.findAdjRobot(" + type + ", " + team + ")\nFailed to find robot");
		return null;
	}
	
	/*
	 * Calculates the best soup
	 * Best soup is determined by distance to current location and the refinery/HQ
	 * Return the MapLocation of the lowest score
	 */
	protected MapLocation bestSoup(int rSq) {
		MapLocation bestSoup = null;
		int bestScore = 0;
		
		// Check each soup to see if it is the best
		for(MapLocation s : soup) {
			int temp = loc.distanceSquaredTo(s) + s.distanceSquaredTo(ref == null ? HQs[0] : ref);
			if(!s.isWithinDistanceSquared(HQs[0], rSq) && (bestSoup == null || bestScore > temp)) {
				bestSoup = s;
				bestScore = temp;
			}
		}
		
		return bestSoup;
	}
	
	/*
	 * Returns true if the give direction is a cardinal Direction and false otherwise
	 */
	protected boolean isCardinalDir(Direction dir) {
		return dir.equals(Direction.NORTH) || dir.equals(Direction.EAST) || dir.equals(Direction.SOUTH) || dir.equals(Direction.WEST);
	}
	
	/*
	 * Senses a given location to check if a building exists there
	 * Does not consider units in this
	 * Returns true if a building exists there and false otherwise
	 */
	protected boolean senseForBuilding(MapLocation potBuilding) {
		if(rc.canSenseLocation(potBuilding)) {
			try {
				RobotInfo robo = rc.senseRobotAtLocation(potBuilding);
				return robo != null && (robo.type == RobotType.DESIGN_SCHOOL || robo.type == RobotType.FULFILLMENT_CENTER || robo.type == RobotType.REFINERY || robo.type == RobotType.HQ);
			} catch (GameActionException e) {
				System.out.println("Error: Robot.senseForBuilding(" + potBuilding + ") Failed!");
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
	/*
	 * Senses the surroundings for soup and saves any new soup
	 * Stops if there is not enough processing power left
	 * Based on HQ's processing
	 * Return true is all soup has been processed and false if otherwise
	 */
	protected boolean senseNewSoup(boolean first, boolean report, int spareByteCode) {
		MapLocation[] newSoup = rc.senseNearbySoup();
		
		for(MapLocation s : newSoup) {
			// If the soup is new, record it
			if(first || !map.containsKey(s)) {
				soup.add(s);
				map.put(s, new int[] {0,0,1,0});
				if(report) {
					submitTransaction(new int[] {teamCode, s.x, s.y, 1, -1, -1, 0}, 1, false);
				}
			}
			
			if(Clock.getBytecodesLeft() < 5000) {
				return false;
			}
		}
		
		return true;
	}
	
	protected Direction[] getPrefDir(Direction initDir) {
		Direction[] results = new Direction[8];
        int[] offsets = {1, 7, 2, 6, 3, 5, 4};
        int baseDir = directions.indexOf(initDir);
        
        results[0] = initDir;
        for(int i = 0; i < offsets.length; i++) {
        	results[i + 1] = directions.get((baseDir + offsets[i]) % 8);
        }
        
        return results;
	}
	
	/*
	 * Finishes the current turn, check transactions, and repeats
	 * Repeats until cooldown < 1
	 */
	protected void yield() {
		Clock.yield();
		checkTransactions();
		checkPhase();
		while(rc.getCooldownTurns() >= 1) {
			Clock.yield();
			checkTransactions();
			checkPhase();
		}
	}

	private void checkPhase() {
		if(phase == 1 && fulCent != null) {
			phase = 2;
		}
		if(phase == 2 && (ref != null || rc.getRoundNum() > 300)) {
			phase = 3;
		}
		if(phase == 3 && rc.getRoundNum() > 500) {
			phase = 4;
		}
	}

	// Blockchain
	
	/*
	 * Checks the previous turn's BlockChain message
	 * Analyzes each message there
	 */
	protected void checkTransactions() {
		Transaction[] trans = new Transaction[] {};
		
		try {
			trans = rc.getBlock(rc.getRoundNum()-1);
		} catch (GameActionException e) {
			System.out.println("Error: Robot.checkTransactions() Failed!");
			e.printStackTrace();
		}
		
		for(Transaction t : trans) {
			analyzeTransaction(t);
		}
	}
	
	/*
	 * Analyzes a single message checking that it is our team's
	 * Based on the last number, save the information respectively
	 */
	private void analyzeTransaction(Transaction t) {
		MapLocation loc;
		if(t.getMessage()[0] != teamCode) {
			return;
		}
		
		if(t.getMessage()[6] == 0) { // Soup
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			if(!map.containsKey(loc)) {
				soup.add(loc);
				map.put(loc, new int[] {0,0,t.getMessage()[3],0});
			}
			System.out.println("Soup Message Recieved!");
			
		} else if(t.getMessage()[6] == 1) { // HQs
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			HQs[1] = loc;
			map.put(loc, new int[] {0,0,0,-1});
			System.out.println("HQ Message Recieved!");
			
		} else if(t.getMessage()[6] == 2) { // Refinery
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			ref = loc;
			map.put(loc, new int[] {0,0,0,2});
			System.out.println("Refinery Message Recieved!");
			
		} else if(t.getMessage()[6] == 3) { // Vaporator
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			vaporator = loc;
			map.put(loc, new int[] {0,0,0,3});
			System.out.println("Vaporator Message Recieved!");
			
		} else if(t.getMessage()[6] == 4) { // Fulfillment Center
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			vaporator = loc;
			map.put(loc, new int[] {0,0,0,4});
			System.out.println("Fulfillment Center Message Recieved!");
			
		} else if(t.getMessage()[6] == 5) { // Design School
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			if(desSch == null) {
				desSch = loc;
			}
			map.put(loc, new int[] {0,0,0,5});
			System.out.println("Design School Message Recieved!");
			
		} else if(t.getMessage()[6] == 7) { // Phase change
			phase = t.getMessage()[1];
			System.out.println("Phase Message Recieved!");
			
		} else if(t.getMessage()[6] == 8) { // Terraformer
			loc = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			HQs[0] = loc;
			terraformer = true;
			map.put(loc, new int[] {0,0,0,1});
			System.out.println("Terraformer Message Recieved!");
			
		} else if(t.getMessage()[6] == 9) { // Requires Movement
			MapLocation start = new MapLocation(t.getMessage()[1], t.getMessage()[2]);
			MapLocation end = null;
			if(t.getMessage()[3] != -1) {
				end = new MapLocation(t.getMessage()[3], t.getMessage()[4]);
			}
			moveReqs.add(new MapLocation[] {start, end});
			System.out.println("Move Request Message Recieved!");
		}
	}
	
	protected boolean submitTransaction(int[] message, int cost, boolean wait) {
		while(wait && !rc.canSubmitTransaction(message, cost)) {
			yield();
		}
		
		if(rc.canSubmitTransaction(message, cost)) {
			try {
				rc.submitTransaction(message, cost);
			} catch (GameActionException e) {
				System.out.println("Error: Robot.submitTransaction(" + message + ", " + cost + ", " + wait + ") Failed!");
				e.printStackTrace();
			}
			return true;
		}
		
		System.out.println("Failure: Robot.submitTransaction(" + message + ", " + cost + ", " + wait + ")\nFailed to submit transaction");
		return false;
	}
}