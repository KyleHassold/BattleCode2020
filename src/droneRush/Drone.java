package droneRush;

import java.util.List;
import java.util.ArrayList;

import battlecode.common.*;

public class Drone extends Unit {
	String job;

	public Drone(RobotController rc) {
		super(rc);
		RobotInfo[] robots = rc.senseNearbyRobots(2, rc.getTeam().opponent());
		boolean nearHq = false;
		boolean nearDs = false;
		for(RobotInfo robo : robots) {
			if(robo.type == RobotType.HQ) {
				nearHq = true;
			} else if(robo.type == RobotType.DESIGN_SCHOOL) {
				nearDs = true;
			}
		}
		if(nearHq && nearDs) {
			job = "Scout";
		} else if(nearHq) {
			job = "Defense";
		} else if(nearDs) {
			job = "Attacker";
		} else if(nearDs) {
			job = "Mover";
		} else {
			job = "Scout";
		}
		System.out.println(job);
	}

	@Override
	protected void run() throws GameActionException {

		while(true) {
			try {
				if(job.equals("Scout")) {
					runScout();
				} else if(job.equals("Defense")) {
					runDefend();
				} else if(job.equals("Attacker")) {
					runAttack();
				} else if(job.equals("Mover")) {
					runMover();
				} else {
					System.out.println("Failed");
				}
			} catch(GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
			}
		}
	}
	
	private void runScout() throws GameActionException {
		MapLocation[] targets = new MapLocation[3];
		targets[0] = new MapLocation(mapW - HQs[0].x - 1, HQs[0].y);
		targets[1] = new MapLocation(mapW - HQs[0].x - 1, mapH - HQs[0].y - 1);
		targets[2] = new MapLocation(HQs[0].x, mapH - HQs[0].y - 1);
		int count = 0;
		
		while(count < targets.length) {
			if(rc.canSenseLocation(targets[count])) {
				RobotInfo robo = rc.senseRobotAtLocation(targets[count]);
				if(robo != null && robo.type == RobotType.HQ && robo.team == rc.getTeam().opponent()) {
					HQs[1] = targets[count];
					map.put(targets[count], new int[] {0,0,0,-1});
					int[] message = new int[] {117291, targets[count].x, targets[count].y, -1, -1, -1, -1};
					while(!rc.canSubmitTransaction(message, 10)) {
						yield();
					}
					rc.submitTransaction(message, 10);
					job = "Mover";
					break;
				} else {
					System.out.println("Next");
					count++;
				}
			} else {
				moveCloser(targets[count], true);
				yield();
			}
		}
	}
	
	private void runDefend() throws GameActionException {
		while(!loc.isWithinDistanceSquared(HQs[0], 48)) {
			moveCloser(HQs[0], false);
			yield();
		}
		while(true) {
			yield();
		}
	}
	
	private void runAttack() {
		
	}
	
	private void runMover() throws GameActionException {
		List<MapLocation> landscaperSpots = new ArrayList<MapLocation>();
		landscaperSpots.add(new MapLocation(HQs[0].x + 1, HQs[0].y + 1));
		landscaperSpots.add(new MapLocation(HQs[0].x + 1, HQs[0].y - 1));
		landscaperSpots.add(new MapLocation(HQs[0].x - 1, HQs[0].y - 1));
		landscaperSpots.add(new MapLocation(HQs[0].x - 2, HQs[0].y - 1));
		landscaperSpots.add(new MapLocation(HQs[0].x - 2, HQs[0].y + 1));
		landscaperSpots.add(new MapLocation(HQs[0].x, HQs[0].y + 1));
		
		while(true) {
			// Wait for request
			while(moveReqs.size() == 0) {
				moveCloser(new MapLocation(HQs[0].x + 3 * HQs[0].directionTo(center).dx, HQs[0].y + 3 * HQs[0].directionTo(center).dy), false);
				RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam());
				for(RobotInfo robo : robots) {
					if(robo.type == RobotType.MINER && robo.location.x <= HQs[0].x + 2 && robo.location.x >= HQs[0].x - 3 && robo.location.y <= HQs[0].y + 2 && robo.location.y <= HQs[0].y - 2) {
						Direction dir = HQs[0].directionTo(center);
						moveReqs.add(0, new MapLocation[] {robo.location, HQs[0].translate(3*dir.dx, 3*dir.dy)});
					}
				}
				yield();
			}
			
			// Move to target robot
			System.out.println("Moving to target");
			while(!loc.isAdjacentTo(moveReqs.get(0)[0])) {
				moveCloser(moveReqs.get(0)[0], false);
			}
			
			// Sense the robot to pick up
			System.out.println("Picking up target");
			while(!rc.canSenseLocation(moveReqs.get(0)[0])) {
				yield();
			}
			int roboId = rc.senseRobotAtLocation(moveReqs.get(0)[0]).ID;
			while(!rc.canPickUpUnit(roboId)) {
				yield();
			}
			rc.pickUpUnit(roboId);
			
			// Move to desired location
			System.out.println("Moving to drop off");
			MapLocation dropOff = moveReqs.get(0)[1] != null ? moveReqs.get(0)[1] : landscaperSpots.remove(0);
			rc.setIndicatorDot(dropOff, 120, 120, 120);
			while(!loc.isAdjacentTo(dropOff)) {
				moveCloser(dropOff, false);
				yield();
			}
			
			while(loc.equals(dropOff)) {
				System.out.println("Need to move");
				for(Direction dir : Direction.allDirections()) {
					if(rc.canMove(dir)) {
						rc.move(dir);
						yield();
						loc = rc.getLocation();
						break;
					}
				}
			}
			
			// Drop off
			System.out.println("Dropping off");
			while(!rc.canDropUnit(loc.directionTo(dropOff))) {
				yield();
			}
			rc.dropUnit(loc.directionTo(dropOff));
			
			// Task completed
			System.out.println("Done");
			moveReqs.remove(0);
		}
	}
}
