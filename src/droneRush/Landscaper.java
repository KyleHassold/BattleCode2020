package droneRush;

import java.util.ArrayList;
import java.util.List;

import battlecode.common.*;

public class Landscaper extends Unit {

	public Landscaper(RobotController rc) {
		super(rc);
		try {
			desSch = findAdjacentRobot(RobotType.DESIGN_SCHOOL, null);
		} catch (GameActionException e1) {
			e1.printStackTrace();
		}
		if(HQs[0] == null) {
			try {
				fulCent = findAdjacentRobot(RobotType.FULFILLMENT_CENTER, null);
				Direction dir = desSch.directionTo(fulCent).rotateLeft();
				HQs[0] = new MapLocation(fulCent.x + 2 * dir.dx, fulCent.y + 2 * dir.dy);
			} catch (GameActionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			for(RobotInfo robo : rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam())) {
				if(robo.type == RobotType.VAPORATOR) {
					vaporator = robo.location;
				}
			}
		}
		try {
			checkTransactions();
		} catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void run() throws GameActionException {
		try {
			if(terraformer) {
				equalizeLandscape();
				barricadeOuter();
			} else {
				System.out.println("Landscaper");
				getToSpot();
				barricade();
			}
		} catch(GameActionException e) {
			System.out.println(rc.getType() + " Exception");
			e.printStackTrace();
		}
	}

	private void getToSpot() throws GameActionException {
		rc.setIndicatorDot(landscaperSpots.get(0), 255, 0, 0);
		target = landscaperSpots.remove(0);
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

		yield();
		while(!loc.equals(target)) {
			if(rc.canSenseLocation(target) && rc.senseRobotAtLocation(target) != null && rc.senseRobotAtLocation(target).type == RobotType.LANDSCAPER) {
				target = landscaperSpots.remove(0);
			}
			MapLocation prevLoc = loc;
			pathFindToOne(target, false, "On");
			if(loc.equals(prevLoc)) {
				int[] message = new int[] {117299, loc.x, loc.y, target.x, target.y, -1, -1};
				while(!rc.canSubmitTransaction(message, 10)) {
					yield();
				}
				rc.submitTransaction(message, 10);
				while(!landscaperSpots.get(0).equals(rc.getLocation())) {
					yield();
				}
				loc = rc.getLocation();
			}
			yield();
		}
	}
	
	private void barricade() throws GameActionException {
		Direction placeDirt = Direction.CENTER;
		Direction mineDir = HQs[0].directionTo(rc.getLocation());
		if(!rc.onTheMap(rc.adjacentLocation(mineDir)) && !isCardinalDir(mineDir)) {
			mineDir = rc.onTheMap(rc.adjacentLocation(mineDir.rotateRight().rotateRight())) ? mineDir.rotateRight().rotateRight() : mineDir.rotateLeft().rotateLeft();
		}
		
		while(true) {
			if(rc.canDigDirt(mineDir)) {
				rc.digDirt(mineDir);
				yield();
			}
			
			while(rc.canDepositDirt(placeDirt)) {
				rc.depositDirt(placeDirt);
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
		System.out.println(tooHigh);
		System.out.println(tooLow);
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
		new MapLocation(HQs[0].x + 2 * toCenter.dx, HQs[0].y + toCenter.dy);
		new MapLocation(HQs[0].x + toCenter.dx, HQs[0].y + 2 * toCenter.dy);
		MapLocation[] toBeFormed = new MapLocation[] {
				HQs[0].translate(2 * toCenter.dx, toCenter.dy), HQs[0].translate(toCenter.dx, 2 * toCenter.dy),
				HQs[0].translate(1, 1), HQs[0].translate(1, 0),
				HQs[0].translate(1, -1), HQs[0].translate(0, -1),
				HQs[0].translate(-1, -1), HQs[0].translate(-1, 0),
				HQs[0].translate(-1, 1), HQs[0].translate(1, 0)
		};
		for(MapLocation terraform : toBeFormed) {
			if(pathFindTo(terraform, 20, false, "In Range")) {
				try {
					if(rc.senseElevation(terraform) > 5) {
						tooHigh.add(terraform);
					} else if(rc.senseElevation(terraform) < 2) {
						tooLow.add(terraform);
					}
				} catch (GameActionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				System.out.println("Failed to sense elevation of: " + terraform);
			}
		}
	}

	private void getDirt(List<MapLocation> tooHigh) {
		Direction dir;
		while(rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit && !tooHigh.isEmpty()) {
			dir = loc.directionTo(tooHigh.get(0));
			if(pathFindTo(tooHigh.get(0), 20, false, "Adj")) {
				try {
					while(rc.canDigDirt(dir) && rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit && rc.senseElevation(tooHigh.get(0)) > 5) {
						rc.digDirt(dir);
						yield();
					}
					if(rc.senseElevation(tooHigh.get(0)) <= 5 || senseForBuilding(rc.adjacentLocation(dir))) {
						tooHigh.remove(0);
					}
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
		}

		if(rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit) {
			MapLocation getDirtFrom = desSch.translate(1, 0);
			if(pathFindTo(getDirtFrom, 20, false, "Adj")) {
				dir = loc.directionTo(getDirtFrom);
				while(rc.getDirtCarrying() < RobotType.LANDSCAPER.dirtLimit && rc.canDigDirt(dir)) {
					try {
						rc.digDirt(dir);
						yield();
					} catch (GameActionException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void putDirt(List<MapLocation> tooLow) {
		Direction dir;
		while(rc.getDirtCarrying() > 0 && !tooLow.isEmpty()) {
			dir = loc.directionTo(tooLow.get(0));
			if(pathFindTo(tooLow.get(0), 20, false, "Adj")) {
				try {
					while(rc.canDepositDirt(dir) && !senseForBuilding(rc.adjacentLocation(dir)) && rc.getDirtCarrying() > 0 && rc.senseElevation(tooLow.get(0)) < 2) {
						rc.depositDirt(dir);
						yield();
					}
					if(rc.senseElevation(tooLow.get(0)) >= 2 || senseForBuilding(rc.adjacentLocation(dir))) {
						tooLow.remove(0);
					}
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
		}
		
		if(rc.getDirtCarrying() > 0) {
			MapLocation putDirt = desSch.translate(1, 0);
			if(pathFindTo(putDirt, 20, false, "Adj")) {
				dir = loc.directionTo(putDirt);
				while(rc.getDirtCarrying() > 0 && rc.canDepositDirt(dir)) {
					try {
						rc.depositDirt(dir);
						yield();
					} catch (GameActionException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
