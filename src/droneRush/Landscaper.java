package droneRush;

import battlecode.common.*;

public class Landscaper extends Unit {

	public Landscaper(RobotController rc) {
		super(rc);
		if(HQs[0] == null) {
			try {
				desSch = findAdjacentRobot(RobotType.DESIGN_SCHOOL, null);
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
	}

	@Override
	protected void run() throws GameActionException {
		try {
			System.out.println("Landscaper");
			/*if(vaporator == null) {
				System.out.println("First 4");
				moveToSpot();
			} else {*/
				int[] message = new int[] {117299, loc.x, loc.y, -1, -1, -1, -1};
				while(!rc.canSubmitTransaction(message, 10)) {
					yield();
				}
				rc.submitTransaction(message, 10);
				while(!landscaperSpots.contains(rc.getLocation())) {
					yield();
				}
				loc = rc.getLocation();
			//}
			barricade();
		} catch(GameActionException e) {
			System.out.println(rc.getType() + " Exception");
			e.printStackTrace();
		}
	}
	
	private void moveToSpot() throws GameActionException {
		MapLocation[] spots = new MapLocation[4];
		spots[0] = new MapLocation(HQs[0].x + 1, HQs[0].y);
		spots[1] = new MapLocation(HQs[0].x - 1, HQs[0].y + 1);
		spots[2] = new MapLocation(HQs[0].x, HQs[0].y - 1);
		spots[3] = new MapLocation(HQs[0].x - 2, HQs[0].y);
		int currSpot = 0;
		while(!loc.equals(spots[currSpot])) {
			rc.setIndicatorDot(spots[currSpot], 255, 0, 0);
			if(rc.onTheMap(spots[currSpot]) && !(rc.canSenseLocation(spots[currSpot]) && rc.senseRobotAtLocation(spots[currSpot]) != null && rc.senseRobotAtLocation(spots[currSpot]).type == RobotType.LANDSCAPER)) {
				System.out.println("Move!" + rc.getCooldownTurns());
				if(!pathFindTo(spots[currSpot], 20, false, "On")) {
					int[] message = new int[] {117299, loc.x, loc.y, -1, -1, -1, -1};
					while(!rc.canSubmitTransaction(message, 10)) {
						yield();
					}
					rc.submitTransaction(message, 10);
					System.out.println("Move Requested");
					break;
				}
			} else {
				currSpot = (currSpot + 1) % 4;
			}
			yield();
		}
	}
	
	private void barricade() throws GameActionException {
		/*Direction[] placeSpots;
		if(loc.equals(new MapLocation(HQs[0].x - 1, HQs[0].y + 1)) || loc.equals(new MapLocation(HQs[0].x, HQs[0].y - 1))) {
			placeSpots = new Direction[3];
			placeSpots[0] = Direction.EAST;
			placeSpots[1] = Direction.CENTER;
			placeSpots[2] = Direction.WEST;
		} else if(loc.equals(new MapLocation(HQs[0].x + 1, HQs[0].y)) || loc.equals(new MapLocation(HQs[0].x - 2, HQs[0].y))) {
			placeSpots = new Direction[3];
			placeSpots[0] = Direction.NORTH;
			placeSpots[1] = Direction.CENTER;
			placeSpots[2] = Direction.SOUTH;
		} else {
			placeSpots = new Direction[1];
			placeSpots[0] = Direction.CENTER;
		}
		*/
		Direction placeSpots = Direction.CENTER;
		Direction mineDir = landscaperMining.get(landscaperSpots.indexOf(rc.getLocation()));
		
		Direction needDirt = placeSpots;
		while(true) {
			
			/*for(Direction dir : placeSpots) {
				if(rc.senseElevation(rc.adjacentLocation(dir)) < rc.senseElevation(rc.adjacentLocation(needDirt))) {
					needDirt = dir;
				}
			}*/
			
			if(rc.canDigDirt(mineDir)) {
				rc.digDirt(mineDir);
				yield();
			}
			
			while(rc.canDepositDirt(needDirt)) {
				rc.depositDirt(needDirt);
				yield();
			}
		}
	}
}
