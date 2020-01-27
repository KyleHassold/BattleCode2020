package droneRush;

import java.util.ArrayList;
import java.util.List;

import battlecode.common.*;

public class Landscaper extends Unit {
	Direction getDirt;
	Direction[] putDirt;

	public Landscaper(RobotController rc) {
		super(rc);
		
		desSch = findAdjRobot(RobotType.DESIGN_SCHOOL, rc.getTeam());
		if(HQs[0] == null) {
			fulCent = findAdjRobot(RobotType.FULFILLMENT_CENTER, rc.getTeam());
			Direction dir = desSch.directionTo(fulCent).rotateLeft();
			HQs[0] = new MapLocation(fulCent.x + 2 * dir.dx, fulCent.y + 2 * dir.dy);
			
			for(RobotInfo robo : rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam())) {
				if(robo.type == RobotType.VAPORATOR) {
					vaporator = robo.location;
				}
			}
		}
		checkTransactions();
	}

	@Override
	protected void run() {
		if(terraformer) {
			equalizeLandscape();
			barricadeOuter();
		} else {
			getToSpot();
			getSpotInfo();
			barricade();
		}
	}

	private void getToSpot() {
		rc.setIndicatorDot(landscaperSpots.get(0), 255, 0, 0);
		target = landscaperSpots.remove(0);
		
		// Keep trying to go to spots on the wall to fill in the wall
		try {
			while(landscaperSpots.size() > 0 && !loc.equals(target) && pathFindTo(target, 20, false, "In Range") && rc.senseRobotAtLocation(target) != null && rc.senseRobotAtLocation(target).type == RobotType.LANDSCAPER) {
				target = landscaperSpots.remove(0);
				rc.setIndicatorDot(target, 255, 0, 0);
			}
			
			if(landscaperSpots.size() == 0 && !loc.equals(target) && pathFindTo(target, 20, false, "In Range") && rc.senseRobotAtLocation(target) != null && rc.senseRobotAtLocation(target).type == RobotType.LANDSCAPER) {
				while(true) {
					System.out.println("Failed to get to any spot");
					yield();
				}
			}
		} catch (GameActionException e) {
			System.out.println("Error: Landscaper.getToSpot() Failed!\nrc.senseRobotAtLocation(" + target + ") Failed!");
			e.printStackTrace();
		}

		yield();
		
		// Move to selected target
		while(!loc.equals(target)) {
			// If the target is now filled, move on
			try {
				if(rc.canSenseLocation(target) && rc.senseRobotAtLocation(target) != null && rc.senseRobotAtLocation(target).type == RobotType.LANDSCAPER) {
					target = landscaperSpots.remove(0);
				}
			} catch (GameActionException e) {
				System.out.println("Error: Landscaper.getToSpot() Failed!\nrc.senseRobotAtLocation(" + target + ") Failed!");
				e.printStackTrace();
			}
			
			MapLocation prevLoc = loc;
			pathFindToOne(target, false, "On");
			
			// If movement failed
			if(loc.equals(prevLoc)) {
				submitTransaction(new int[] {teamCode, loc.x, loc.y, target.x, target.y, -1, 9}, 10, true);
				yield();
				int prevTurn = rc.getRoundNum() - 1;
				while(rc.getRoundNum() == prevTurn + 1) {
					prevTurn = rc.getRoundNum() - 1;
					yield();
				}
				loc = rc.getLocation();
				break;
			}
			
			yield();
		}
	}
	
	private void getSpotInfo() {
		int dist = Math.max(Math.abs(loc.x - HQs[0].x), Math.abs(loc.y - HQs[0].y));
		if(dist == 1) {
			getDirt = HQs[0].directionTo(loc);
			putDirt = new Direction[] {Direction.CENTER};
		} else if(dist == 2) {
			getDirt = Direction.CENTER;
			putDirt = new Direction[] {loc.directionTo(HQs[0])};
		} else if(dist == 3) {
			getDirt = Direction.CENTER;
			putDirt = new Direction[] {HQs[0].directionTo(loc), HQs[0].directionTo(loc).rotateLeft(), HQs[0].directionTo(loc).rotateRight()};
		} else if(dist == 4) {
			if(Math.abs(loc.x - HQs[0].x) == Math.abs(loc.y - HQs[0].y)) {
				getDirt = loc.directionTo(HQs[0]);
				putDirt = new Direction[] {Direction.CENTER, getDirt.rotateLeft(), getDirt.rotateRight()};
			} else if(Math.abs(loc.x - HQs[0].x) == 4) {
				if(Math.abs(loc.y - HQs[0].y) % 2 == 1) {
					getDirt = Math.abs(loc.x - HQs[0].x) < 0 ? Direction.EAST : Direction.WEST;
				} else {
					getDirt = Math.abs(loc.x - HQs[0].x) < 0 ? Direction.SOUTHEAST : Direction.SOUTHWEST;
				}
				putDirt = new Direction[] {Direction.CENTER, Direction.NORTH, Direction.SOUTH};
			} else {
				if(Math.abs(loc.x - HQs[0].x) % 2 == 1) {
					getDirt = Math.abs(loc.y - HQs[0].y) < 0 ? Direction.NORTH : Direction.SOUTH;
				} else {
					getDirt = Math.abs(loc.y - HQs[0].y) < 0 ? Direction.NORTHEAST : Direction.SOUTHEAST;
				}
				putDirt = new Direction[] {Direction.CENTER, Direction.EAST, Direction.WEST};
			}
		} else {
			System.out.println("Failure: Landscaper.getSpotInfo()\nFailed to get info for: " + dist);
		}
	}
	
	private void barricade() {
		// Check that the mining location is valid
		if(!rc.onTheMap(rc.adjacentLocation(getDirt)) && !isCardinalDir(getDirt)) {
			getDirt = rc.onTheMap(rc.adjacentLocation(getDirt.rotateRight().rotateRight())) ? getDirt.rotateRight().rotateRight() : getDirt.rotateLeft().rotateLeft();
		}
		
		// Forever, mine and place dirt
		while(true) {
			// Dig dirt
			if(rc.canDigDirt(getDirt)) {
				try {
					rc.digDirt(getDirt);
				} catch (GameActionException e) {
					System.out.println("Error: Landscaper.barricade() Failed!\nrc.digDirt(" + getDirt + ") Failed!");
					e.printStackTrace();
				}
				
				yield();
			}
			
			// Place dirt
			Direction low = putDirt[0];
			for(Direction dir : putDirt) {
				try {
					if(rc.senseElevation(rc.adjacentLocation(dir)) < rc.senseElevation(rc.adjacentLocation(low))) {
						low = dir;
					}
				} catch (GameActionException e) {
					System.out.println("Error: Landscaper.barricade() Failed!\nrc.senseElevation(" + dir + " or " + low + ") Failed!");
					e.printStackTrace();
				}
			}
			
			while(rc.canDepositDirt(low)) {
				try {
					rc.depositDirt(low);
				} catch (GameActionException e) {
					System.out.println("Error: Landscaper.barricade() Failed!\nrc.depositDirt(" + low + ") Failed!");
					e.printStackTrace();
				}
				
				yield();
			}
		}
	}

	private void barricadeOuter() {
		while(true) {
			yield();
		}
	}

	private void equalizeLandscape() {
		List<MapLocation> tooHigh = new ArrayList<MapLocation>();
		List<MapLocation> tooLow = new ArrayList<MapLocation>();
		
		analyzeTerrain(tooHigh, tooLow);
		
		while(!tooHigh.isEmpty() || !tooLow.isEmpty()) {
			getDirt(tooHigh);
			putDirt(tooLow);
		}
	}
	
	private void analyzeTerrain(List<MapLocation> tooHigh, List<MapLocation> tooLow) {
		Direction toCenter = HQs[0].directionTo(center);
		
		if(isCardinalDir(toCenter)) {
			toCenter = toCenter.rotateRight();
		}
		
		MapLocation[] toBeFormed = new MapLocation[] {
				HQs[0].translate(2 * toCenter.dx, toCenter.dy), HQs[0].translate(toCenter.dx, 2 * toCenter.dy),
				HQs[0].translate(1, 1), HQs[0].translate(1, 0),
				HQs[0].translate(1, -1), HQs[0].translate(0, -1),
				HQs[0].translate(-1, -1), HQs[0].translate(-1, 0),
				HQs[0].translate(-1, 1), HQs[0].translate(1, 0)
		};
		
		// Check all locations to see if they need to be terraformed
		for(MapLocation terraform : toBeFormed) {
			if(pathFindTo(terraform, 20, false, "In Range")) {
				try {
					if(rc.senseElevation(terraform) > 5) {
						tooHigh.add(terraform);
					} else if(rc.senseElevation(terraform) < 2 || rc.senseFlooding(terraform)) {
						tooLow.add(terraform);
					}
				} catch (GameActionException e) {
					System.out.println("Error: Landscaper.analyzeTerrain() Failed!\nrc.senseElevation(" + terraform + ") Failed!");
					e.printStackTrace();
				}
				
			} else {
				System.out.println("Failure: Landscaper.analyzeTerrain()\nFailed to analyze: " + terraform);
			}
		}
	}

	private void getDirt(List<MapLocation> tooHigh) {
		Direction dir;
		
		// Mine until all high spots have been terraformed or full
		while(rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit && !tooHigh.isEmpty()) {
			dir = loc.directionTo(tooHigh.get(0));
			
			// Go to a mountain
			if(pathFindTo(tooHigh.get(0), 20, false, "Adj")) {
				try {
					// Tear down the mountain
					while(rc.canDigDirt(dir) && rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit && rc.senseElevation(tooHigh.get(0)) > 5) {
						rc.digDirt(dir);
						yield();
					}
					
					// If successful, remove it
					if(rc.senseElevation(tooHigh.get(0)) <= 5 || senseForBuilding(rc.adjacentLocation(dir))) {
						tooHigh.remove(0);
					}
				} catch (GameActionException e) {
					System.out.println("Error: Landscaper.getDirt() Failed!\nrc.senseElevation(" + tooHigh.get(0) + ") or rc.digDirt(" + dir + ") Failed!");
					e.printStackTrace();
				}
			}
		}
		
		// If not full
		if(rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
			MapLocation getDirtFrom = desSch.translate(1, 0);

			// Go to generic spot
			if(pathFindTo(getDirtFrom, 20, false, "Adj")) {
				dir = loc.directionTo(getDirtFrom);
				
				// Get dirt
				while(rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit && rc.canDigDirt(dir)) {
					try {
						rc.digDirt(dir);
						yield();
					} catch (GameActionException e) {
						System.out.println("Error: Landscaper.getDirt() Failed!\nrc.digDirt(" + dir + ") Failed!");
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void putDirt(List<MapLocation> tooLow) {
		Direction dir;
		
		// Put dirt until all low spots filled in or empty
		while(rc.getDirtCarrying() > 0 && !tooLow.isEmpty()) {
			dir = loc.directionTo(tooLow.get(0));
			
			// Go to first low spot
			if(pathFindTo(tooLow.get(0), 20, false, "Adj")) {
				try {
					// Fill in hole or flooded tile
					while(rc.canDepositDirt(dir) && !senseForBuilding(rc.adjacentLocation(dir)) && rc.getDirtCarrying() > 0 && (rc.senseElevation(tooLow.get(0)) < 2 || rc.senseFlooding(tooLow.get(0)))) {
						rc.depositDirt(dir);
						yield();
					}

					// If successful, remove it
					if(rc.senseElevation(tooLow.get(0)) >= 2 || senseForBuilding(rc.adjacentLocation(dir))) {
						tooLow.remove(0);
					}
				} catch (GameActionException e) {
					System.out.println("Error: Landscaper.putDirt() Failed!\nrc.senseElevation(" + tooLow.get(0) + ") or rc.depositDirt(" + dir + ") Failed!");
					e.printStackTrace();
				}
			}
		}
		
		// If not empty
		if(rc.getDirtCarrying() > 0) {
			MapLocation putDirt = desSch.translate(1, 0);
			
			// Go to generic spot
			if(pathFindTo(putDirt, 20, false, "Adj")) {
				dir = loc.directionTo(putDirt);
				
				// Place dirt
				while(rc.getDirtCarrying() > 0 && rc.canDepositDirt(dir)) {
					try {
						rc.depositDirt(dir);
						yield();
					} catch (GameActionException e) {
						System.out.println("Error: Landscaper.putDirt() Failed!\nrc.depositDirt(" + dir + ") Failed!");
						e.printStackTrace();
					}
				}
			}
		}
	}
}