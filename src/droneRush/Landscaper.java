package droneRush;

import java.util.ArrayList;
import java.util.List;

import battlecode.common.*;

public class Landscaper extends Unit {

	public Landscaper(RobotController rc) {
		super(rc);
		
		desSch = findAdjRobot(RobotType.DESIGN_SCHOOL, null);
		if(HQs[0] == null) {
			fulCent = findAdjRobot(RobotType.FULFILLMENT_CENTER, null);
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
				while(!landscaperSpots.get(0).equals(rc.getLocation())) {
					yield();
				}
				loc = rc.getLocation();
			}
			
			yield();
		}
	}
	
	private void barricade() {
		Direction[] putDirt = new Direction[2];
		if(loc.x == HQs[0].x) {
			putDirt[0] = Direction.EAST;
			putDirt[1] = Direction.WEST;
		} else if(loc.y == HQs[0].y) {
			putDirt[0] = Direction.NORTH;
			putDirt[1] = Direction.SOUTH;
		} else {
			putDirt[0] = loc.directionTo(HQs[0]).rotateLeft();
			putDirt[1] = loc.directionTo(HQs[0]).rotateRight();
		}
		
		Direction placeDirt = Direction.CENTER;
		Direction mineDir = HQs[0].directionTo(rc.getLocation());
		
		// Check that the mining location is valid
		if(!rc.onTheMap(rc.adjacentLocation(mineDir)) && !isCardinalDir(mineDir)) {
			mineDir = rc.onTheMap(rc.adjacentLocation(mineDir.rotateRight().rotateRight())) ? mineDir.rotateRight().rotateRight() : mineDir.rotateLeft().rotateLeft();
		}
		
		// Forever, mine and place dirt
		while(true) {
			// Dig dirt
			if(rc.canDigDirt(mineDir)) {
				try {
					rc.digDirt(mineDir);
				} catch (GameActionException e) {
					System.out.println("Error: Landscaper.barricade() Failed!\nrc.digDirt(" + mineDir + ") Failed!");
					e.printStackTrace();
				}
				
				yield();
			}

			placeDirt = Direction.CENTER;
			if(rc.getRoundNum() > 1000) {
				for(Direction putDirection : putDirt) {
					rc.setIndicatorDot(rc.adjacentLocation(putDirection), 255, 0, 255);
					try {
						if(rc.canSenseLocation(rc.adjacentLocation(putDirection)) && rc.senseRobotAtLocation(rc.adjacentLocation(putDirection)) != null && rc.senseRobotAtLocation(rc.adjacentLocation(putDirection)).type == RobotType.LANDSCAPER && rc.senseElevation(rc.adjacentLocation(placeDirt)) > rc.senseElevation(rc.adjacentLocation(putDirection))) {
							placeDirt = putDirection;
						}
					} catch (GameActionException e) {
						e.printStackTrace();
					}
				}
			}
			
			// Place dirt
			while(rc.canDepositDirt(placeDirt)) {
				try {
					rc.depositDirt(placeDirt);
				} catch (GameActionException e) {
					System.out.println("Error: Landscaper.barricade() Failed!\nrc.depositDirt(" + placeDirt + ") Failed!");
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