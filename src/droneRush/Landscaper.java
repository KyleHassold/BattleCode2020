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
			barricade();
		} catch(GameActionException e) {
			System.out.println(rc.getType() + " Exception");
			e.printStackTrace();
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
}
