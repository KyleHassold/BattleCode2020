package droneRush;

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
		while(true) {
			// Wait for request
			while(moveReqs.size() == 0) {
				Direction dir = HQs[0].directionTo(center);
				pathFindTo(new MapLocation(HQs[0].x + 3 * dir.dx, HQs[0].y + 3 * dir.dy), 2, false, "On");
				RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam());
				for(RobotInfo robo : robots) {
					if(ref != null && robo.location.x <= HQs[0].x + 2 && robo.location.x >= HQs[0].x - 3 && robo.location.y <= HQs[0].y + 2 && robo.location.y >= HQs[0].y - 2) {
						if(robo.type == RobotType.MINER) {
							moveReqs.add(0, new MapLocation[] {robo.location, ref.translate(0, 1)});
						} else if(robo.type == RobotType.COW) {
							moveReqs.add(0, new MapLocation[] {robo.location, new MapLocation(HQs[0].x + 3 * dir.dx, HQs[0].y + 3 * dir.dy)});
						}
					}
				}
				yield();
			}
			
			// Move to target robot
			System.out.println("Moving to target");
			if(pathFindTo(moveReqs.get(0)[0], 100, false, "Adj") && rc.canSenseLocation(moveReqs.get(0)[0])) {
				rc.setIndicatorDot(moveReqs.get(0)[0], 120, 120, 120);
				if(rc.senseRobotAtLocation(moveReqs.get(0)[0]) != null) {
					int roboId = rc.senseRobotAtLocation(moveReqs.get(0)[0]).ID;
					while(!rc.canPickUpUnit(roboId)) {
						yield();
					}
					rc.pickUpUnit(roboId);
					
					// Move to desired location
					MapLocation dropOff = moveReqs.get(0)[1] != null ? moveReqs.get(0)[1] : landscaperSpots.remove(0);
					System.out.println("Moving to drop off: " + dropOff);
					rc.setIndicatorDot(dropOff, 120, 120, 120);
					while(moveReqs.get(0)[1] == null && pathFindTo(dropOff, 50, false, "In Range") && rc.canSenseLocation(dropOff) && !loc.equals(dropOff) && rc.senseRobotAtLocation(dropOff) != null) {
						System.out.println("Yes");
						if(rc.senseRobotAtLocation(dropOff).type == RobotType.LANDSCAPER) {
							dropOff = landscaperSpots.remove(0);
						} else {
							landscaperSpots.add(dropOff);
							dropOff = landscaperSpots.remove(0);
						}
						rc.setIndicatorDot(dropOff, 120, 120, 120);
					}
					if(pathFindTo(dropOff, 50, false, "Adj")) {
						yield();
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
					}
				}
			}
			
			// Task completed
			System.out.println("Done");
			moveReqs.remove(0);
		}
	}
}
