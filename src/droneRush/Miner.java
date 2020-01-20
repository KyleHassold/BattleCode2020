package droneRush;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import battlecode.common.*;

public class Miner extends Unit {
	boolean builder = false;
	RobotType[] buildings = new RobotType[] {RobotType.FULFILLMENT_CENTER, RobotType.DESIGN_SCHOOL, RobotType.REFINERY, RobotType.VAPORATOR};
	MapLocation[] buildSpots = new MapLocation[buildings.length];
	int buildCount = 0;
	boolean checked = false;

	public Miner(RobotController rc) {
		super(rc);

		if(rc.getRoundNum() == 3) {
			builder = true;
			System.out.println("Builder");
		}

		try {
			checkTransactions();
		} catch (GameActionException e) {
			e.printStackTrace();
		}
		
		Direction awayCenter = center.directionTo(HQs[0]);
		if(awayCenter == Direction.NORTH || awayCenter == Direction.EAST || awayCenter == Direction.SOUTH || awayCenter == Direction.WEST) {
			awayCenter = awayCenter.rotateRight();
		}
		buildSpots[0] = new MapLocation(HQs[0].x + 2 * awayCenter.dx, HQs[0].y + 2 * awayCenter.dy);
		if(awayCenter == Direction.NORTHEAST) {
			buildSpots[1] = new MapLocation(HQs[0].x + 2 * awayCenter.dx + 1, HQs[0].y + 2 * awayCenter.dy);
		} else if(awayCenter == Direction.SOUTHEAST) {
			buildSpots[1] = new MapLocation(HQs[0].x + 2 * awayCenter.dx, HQs[0].y + 2 * awayCenter.dy - 1);
		} else if(awayCenter == Direction.SOUTHWEST) {
			buildSpots[1] = new MapLocation(HQs[0].x + 2 * awayCenter.dx - 1, HQs[0].y + 2 * awayCenter.dy);
		} else {
			buildSpots[1] = new MapLocation(HQs[0].x + 2 * awayCenter.dx, HQs[0].y + 2 * awayCenter.dy + 1);
		}
		buildSpots[2] = null;
		buildSpots[3] = HQs[0].translate(-1, 0);
	}

	@Override
	protected void run() throws GameActionException {
		// Sense for initial soup
		MapLocation[] newSoup = rc.senseNearbySoup();
		Collections.addAll(soup, newSoup);
		for(MapLocation s : newSoup) {
			if(rc.canSenseLocation(s)) {
				map.put(s, new int[] {0, 0, rc.senseSoup(s), 0});
			}
		}

		// Get initial target if any soup was found
		if(!soup.isEmpty()) {
			target = bestSoup(0);
			rc.setIndicatorDot(target, 255, 0, 0);
		}
		
		// Find, mine, and refine soup forever or until can build
		while(true) {
			try {
				// Find and mine soup until full
				while(rc.getSoupCarrying() < RobotType.MINER.soupLimit) {
					getSoup();
					if(ref != null && checked == false) {
						Set<MapLocation> tempSoup = new HashSet<MapLocation>();
						for(MapLocation s : soup) {
							if(!(s.x <= HQs[0].x + 2 && s.x >= HQs[0].x - 3 && s.y <= HQs[0].y + 2 && s.y >= HQs[0].y - 2)) {
								tempSoup.add(s);
							}
						}
						soup = tempSoup;
						checked = true;
					}
					
					if(builder && rc.getTeamSoup() >= buildings[buildCount].cost + 10) {
						break;
					}
					yield();
				}
				// Return soup to HQ
				returnSoup();
			} catch(GameActionException e) {
				System.out.println(rc.getType() + " Exception");
				e.printStackTrace();
			}
			yield();
			
			while(builder && rc.getTeamSoup() >= buildings[buildCount].cost + 10) {
				System.out.println("Building!");
				build(buildings[buildCount], buildSpots[buildCount]);
				buildCount++;
				if(buildCount == 4) {
					builder = false;
				}
				yield();
			}
		}
	}

	private void getSoup() throws GameActionException {
		System.out.println("Getting Soup");
		while(soup.isEmpty()) {
			moveRandom();
			yield();
			System.out.println("Moving Randomly");
		}
		target = bestSoup(0);
		System.out.println("Target: " + target);
		
		if(pathFindTo(target, 50, false, "In Range") && rc.canSenseLocation(target) && rc.senseSoup(target) != 0) {
			System.out.println("In Range!");
			if(pathFindTo(target, 15, false, "Adj")) {
				System.out.println("Adjacent!");
				Direction dir = loc.directionTo(target);
				while(rc.canMineSoup(dir)) {
					System.out.println("Mining!");
					rc.mineSoup(dir);
					yield();
				}
			}
		}
		System.out.println("Done");
		
		// If the soup is gone (also check if soup is reachable preferably)
		rc.setIndicatorDot(target, 255, 255, 0);
		if(rc.canSenseLocation(target) && rc.senseSoup(target) == 0) {
			System.out.println("No More Soup");
			rc.setIndicatorDot(target, 255, 255, 255);
			soup.remove(target);
			target = null;
		}
	}

	private void returnSoup() throws GameActionException {
		MapLocation returnTo;
		if(ref == null) {
			returnTo = HQs[0];
		} else {
			returnTo = ref;
		}
		
		// Move to HQ
		while(!loc.isAdjacentTo(returnTo)) {
			moveCloser(returnTo, false);
			yield();
		}

		// Deposit soup to be refined
		Direction dir = loc.directionTo(returnTo);
		if(rc.canDepositSoup(dir)) {
			rc.depositSoup(dir, RobotType.MINER.soupLimit);
		} else {
			System.out.println("Failed to deposit soup!");
		}
	}
	
	private void build(RobotType robo, MapLocation buildSpot) throws GameActionException {
		while(buildSpot == null && bestSoup(15) == null) {
			moveRandom();
			yield();
		}
		if(buildSpot == null) {
			buildSpot = bestSoup(15);
		}
		
		int count = 0;
		while(!loc.isAdjacentTo(buildSpot)) {
			moveCloser(buildSpot, false);
			count++;
			if(count > 20 && robo == RobotType.VAPORATOR) {
				int[] message = new int[] {117299, loc.x, loc.y, buildSpot.x, buildSpot.y - 1, -1, -1};
				while(!rc.canSubmitTransaction(message, 10)) {
					yield();
				}
				rc.submitTransaction(message, 10);
				break;
			}
			yield();
		}
		
		while(!loc.isAdjacentTo(buildSpot)) { // In case needing help from drone
			loc = rc.getLocation();
			System.out.println("Waiting");
			yield();
		}
		
		Direction dir = loc.directionTo(buildSpot);
		while(!rc.canBuildRobot(robo, dir)) {
			yield();
		}
		rc.buildRobot(robo, dir);
		int code;
		if(robo == RobotType.FULFILLMENT_CENTER) {
			code = 117294;
		} else if(robo == RobotType.DESIGN_SCHOOL) {
			code = 117295;
		} else if(robo == RobotType.REFINERY) {
			code = 117292;
		} else if(robo == RobotType.VAPORATOR) {
			code = 117293;
			int[] message = new int[] {117299, loc.x, loc.y, loc.x - 2, loc.y - 2, -1, -1};
			while(!rc.canSubmitTransaction(message, 10)) {
				yield();
			}
			rc.submitTransaction(message, 10);
		} else {
			code = 0;
			System.out.println("Failed to code");
		}
		int[] message = new int[] {code, buildSpot.x, buildSpot.y, -1, -1, -1, -1};
		if(rc.canSubmitTransaction(message, 10)) {
			rc.submitTransaction(message, 10);
		}
	}
}
