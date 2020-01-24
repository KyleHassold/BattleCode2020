package droneRush;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import battlecode.common.*;

public class Miner extends Unit {
	boolean builder = false;
	boolean terraformer = false;
	RobotType[] buildings = new RobotType[] {RobotType.FULFILLMENT_CENTER, RobotType.DESIGN_SCHOOL, RobotType.REFINERY, RobotType.VAPORATOR};
	MapLocation[] buildSpots = new MapLocation[buildings.length];
	int buildCount = 0;
	boolean checked = false;

	public Miner(RobotController rc) {
		super(rc);

		if(rc.getRoundNum() == 2) {
			builder = true;
			Direction toCenter = HQs[0].directionTo(center);
			if(isCardinalDir(toCenter)) {
				toCenter = toCenter.rotateRight();
			}
			buildSpots[0] = new MapLocation(HQs[0].x + 2 * toCenter.dx, HQs[0].y + toCenter.dy);
			buildSpots[1] = new MapLocation(HQs[0].x + toCenter.dx, HQs[0].y + 2 * toCenter.dy);
			buildSpots[2] = null;
			buildSpots[3] = new MapLocation(HQs[0].x - toCenter.dx, HQs[0].y + 2 * toCenter.dy);
			
			System.out.println("Builder");
		}

		checkTransactions();
	}

	@Override
	protected void run() {
		// Sense for initial soup
		MapLocation[] newSoup = rc.senseNearbySoup();
		Collections.addAll(soup, newSoup);
		for(MapLocation s : newSoup) {
			if(rc.canSenseLocation(s)) {
				try {
					map.put(s, new int[] {0, 0, rc.senseSoup(s), 0});
				} catch (GameActionException e) {
					System.out.println("Error: Miner.run() Failed!\nrc.senseSoup(" + s + ") Failed!");
					e.printStackTrace();
				}
			}
		}

		// Get initial target if any soup was found
		if(!soup.isEmpty()) {
			target = bestSoup(0);
			rc.setIndicatorDot(target, 255, 0, 0);
		}
		
		// Find, mine, and refine soup forever or until can build
		while(true) {
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

	private void getSoup() {
		if(soup.isEmpty()) {
			findSoup(false);
		}
		target = bestSoup(0);
		
		try {
			if(pathFindTo(target, 50, false, "In Range") && rc.canSenseLocation(target) && rc.senseSoup(target) != 0) {
				if(pathFindTo(target, 15, false, "Adj")) {
					Direction dir = loc.directionTo(target);
					while(rc.canMineSoup(dir)) {
						try {
							rc.mineSoup(dir);
						} catch (GameActionException e) {
							System.out.println("Error: Miner.getSoup() Failed!\nrc.mineSoup(" + dir + ") Failed!");
							e.printStackTrace();
						}
						yield();
					}
				}
			}
		
			// If the soup is gone (also check if soup is reachable preferably)
			rc.setIndicatorDot(target, 255, 255, 0);
			if(rc.canSenseLocation(target) && rc.senseSoup(target) == 0) {
				rc.setIndicatorDot(target, 255, 255, 255);
				soup.remove(target);
				target = null;
			}
		} catch (GameActionException e) {
			System.out.println("Error: Miner.getSoup() Failed!\nrc.senseSoup(" + target + ") Failed!");
			e.printStackTrace();
		}
	}

	private void returnSoup() {
		MapLocation returnTo;
		if(ref == null) {
			returnTo = HQs[0];
		} else {
			returnTo = ref;
		}
		
		// Move to HQ
		if(!pathFindTo(returnTo, 40, false, "Adj")) {
			System.out.println("Failure: Miner.returnSoup()\nFailed to return to Refinery/HQ");
		}

		// Deposit soup to be refined
		Direction dir = loc.directionTo(returnTo);
		if(rc.canDepositSoup(dir)) {
			try {
				rc.depositSoup(dir, RobotType.MINER.soupLimit);
			} catch (GameActionException e) {
				System.out.println("Error: Miner.returnSoup() Failed!\nrc.depositSoup(dir, RobotType.MINER.soupLimit) Failed!");
				e.printStackTrace();
			}
		} else {
			System.out.println("Failure: Miner.returnSoup()\nFailed to deposit soup");
		}
	}
	
	private void build(RobotType robo, MapLocation buildSpot) {
		if(buildSpot == null) {
			findSoup(true);
			buildSpot = bestSoup(30);
		}
		
		if(!pathFindTo(buildSpot, 50, false, "Adj")) {
			submitTransaction(new int[] {teamCode, loc.x, loc.y, buildSpot.x, buildSpot.y - 1, -1, 9}, 10, true);
		}
		yield();
		if(robo != RobotType.REFINERY) {
			while(!loc.isAdjacentTo(buildSpot)) { // In case needing help from drone
				loc = rc.getLocation();
				yield();
			}
			
			Direction dir = loc.directionTo(buildSpot);
			while(!rc.canBuildRobot(robo, dir)) {
				yield();
				try {
					if(!terraformer && (rc.senseFlooding(buildSpot) || Math.abs(rc.senseElevation(buildSpot) - rc.senseElevation(loc)) > GameConstants.MAX_DIRT_DIFFERENCE)) {
						buildSpot = buildTerraformer();
						robo = RobotType.DESIGN_SCHOOL;
						dir = loc.directionTo(buildSpot);
						submitTransaction(new int[] {teamCode, HQs[0].x, HQs[0].y, -1, -1, -1, 8}, 10, true);
						buildCount--;
					}
				} catch (GameActionException e) {
					System.out.println("Error: Miner.build(" + robo + ", " + buildSpot + ") Failed!\nrc.sense...(" + buildSpot + ") Failed!");
					e.printStackTrace();
				}
			}
			try {
				rc.buildRobot(robo, dir);
			} catch (GameActionException e) {
				System.out.println("Error: Miner.build(" + robo + ", " + buildSpot + ") Failed!\nrc.buildRobot(" + robo + ", " + dir + ") Failed!");
				e.printStackTrace();
			}
		} else if(buildRobot(robo, loc.directionTo(buildSpot))) {
			buildSpot = findAdjRobot(robo, rc.getTeam());
		} else {
			System.out.println("Failure: Miner.build()\nFailed to build robot");
			return;
		}
		int code;
		if(robo == RobotType.FULFILLMENT_CENTER) {
			code = 4;
		} else if(robo == RobotType.DESIGN_SCHOOL) {
			code = 5;
		} else if(robo == RobotType.REFINERY) {
			code = 2;
		} else if(robo == RobotType.VAPORATOR) {
			code = 3;
		} else {
			code = -1;
			System.out.println("Failure: Miner.build()\nFailed to code message");
			return;
		}
		submitTransaction(new int[] {teamCode, buildSpot.x, buildSpot.y, -1, -1, -1, code}, 10, false);
	}

	@SuppressWarnings("unchecked")
	private MapLocation buildTerraformer() {
		while(true) {
			if(loc.x > HQs[0].x + 2 || loc.x < HQs[0].x - 2 || loc.y > HQs[0].y + 2 || loc.y < HQs[0].y - 2) {
				for(Direction dir : Direction.allDirections()) {
					if(rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir)) {
						terraformer = true;
						return loc.translate(dir.dx, dir.dy);
					}
				}
			}
			List<Direction> randDirs = (List<Direction>) directions.clone();
			Collections.shuffle(randDirs);
			Direction backup = null;
			for(Direction dir : randDirs) {
				rc.setIndicatorDot(rc.adjacentLocation(dir), 0, 120, 120);
					if(rc.canMove(dir)) {
						if(moveAwayFromHQ(dir)) {
							try {
								rc.move(dir);
								loc = rc.getLocation();
								break;
							} catch (GameActionException e) {
								System.out.println("Error: Miner.buildTerraformer() Failed!\nrc.move(" + dir + ") Failed!");
								e.printStackTrace();
							}
						} else {
							backup = dir;
						}
					}
			}
			if(backup != null && rc.canMove(backup)) {
				try {
					rc.move(backup);
				} catch (GameActionException e) {
					System.out.println("Error: Miner.buildTerraformer() Failed!\nrc.move(" + backup + ") Failed!");
					e.printStackTrace();
				}
			}
			yield();
		}
	}
}