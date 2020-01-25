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
	}

	@Override
	protected void run() {
		while(true) {
			if(job.equals("Scout")) {
				runScout();
			} else if(job.equals("Defense")) {
				runDefend();
			} else if(job.equals("Attacker")) {
				runAttack();
			} else if(job.equals("Mover")) {
				runMover();
			} else {
				System.out.println("Failure: Drone.run()\nFailed to assign job");
			}
		}
	}
	
	private void runScout() {
		MapLocation[] targets = new MapLocation[3];
		targets[0] = new MapLocation(mapW - HQs[0].x - 1, HQs[0].y);
		targets[1] = new MapLocation(mapW - HQs[0].x - 1, mapH - HQs[0].y - 1);
		targets[2] = new MapLocation(HQs[0].x, mapH - HQs[0].y - 1);
		int count = 0;
		
		while(count < targets.length) {
			// See if the drone can sense the HQ
			if(rc.canSenseLocation(targets[count])) {
				// Sense the location
				RobotInfo robo = null;
				try {
					robo = rc.senseRobotAtLocation(targets[count]);
				} catch (GameActionException e) {
					System.out.println("Error: Drone.runScout() Failed!\nrc.senseRobotAtLocation(" + targets[count] + ") Failed!");
					e.printStackTrace();
				}
				
				// Check if it the HQ
				if(robo != null && robo.type == RobotType.HQ && robo.team == rc.getTeam().opponent()) {
					HQs[1] = targets[count];
					map.put(targets[count], new int[] {0,0,0,-1});
					
					submitTransaction(new int[] {teamCode, targets[count].x, targets[count].y, -1, -1, -1, 1}, 10, true);
					
					job = "Mover";
					return;
				} else {
					// Move on to the next potential spot
					count++;
				}
			} else {
				// Move to the target
				pathFindTo(targets[count], 80, true, "In Range");
			}
		}
		
		System.out.println("Failure: Drone.runScout()\nFailed to find enemy HQ");
	}
	
	private void runDefend() {
		pathFindTo(HQs[0], 80, false, "In Range");
		while(true) {
			yield();
		}
	}
	
	private void runAttack() {
		
	}
	
	private void runMover() {
		Direction dir = HQs[0].directionTo(center);
		
		while(true) {
			// Wait for request
			while(moveReqs.size() == 0) {
				// Go to waiting spot
				pathFindTo(new MapLocation(HQs[0].x + 3 * dir.dx, HQs[0].y + 3 * dir.dy), 2, false, "On");
				RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam());
				// Check if any robots need forced removal
				for(RobotInfo robo : robots) {
					if((ref != null || rc.getRoundNum() > 300) && robo.location.x <= HQs[0].x + 2 && robo.location.x >= HQs[0].x - 3 && robo.location.y <= HQs[0].y + 2 && robo.location.y >= HQs[0].y - 2) {
						if(robo.type == RobotType.MINER) {
							moveReqs.add(0, new MapLocation[] {robo.location, ref != null ? ref.translate(0, 1) : bestSoup(0)});
						} else if(robo.type == RobotType.COW) {
							moveReqs.add(0, new MapLocation[] {robo.location, new MapLocation(HQs[0].x + 3 * dir.dx, HQs[0].y + 3 * dir.dy)});
						}
					}
				}
				
				yield();
			}
			
			// Move to target robot
			if(pathFindTo(moveReqs.get(0)[0], 100, false, "Adj") && rc.canSenseLocation(moveReqs.get(0)[0])) {
				rc.setIndicatorDot(moveReqs.get(0)[0], 120, 120, 120);
				
				RobotInfo robo = null;
				try {
					robo = rc.senseRobotAtLocation(moveReqs.get(0)[0]);
				} catch (GameActionException e) {
					System.out.println("Error: Drone.runMover() Failed!\nrc.senseRobotAtLocation(" + moveReqs.get(0)[0] + ") Failed!");
					e.printStackTrace();
				}
				
				// If the robot is still in the pickup spot
				if(robo != null) {
					// Pick up the unit
					while(!rc.canPickUpUnit(robo.ID)) {
						yield();
					}
					
					try {
						rc.pickUpUnit(robo.ID);
					} catch (GameActionException e) {
						System.out.println("Error: Drone.runMover() Failed!\nrc.pickUpUnit(" + robo.ID + ") Failed!");
						e.printStackTrace();
					}
					
					// Move to desired location
					MapLocation dropOff = moveReqs.get(0)[1] != null ? moveReqs.get(0)[1] : landscaperSpots.remove(0);
					rc.setIndicatorDot(dropOff, 120, 120, 120);
					try {
						while(moveReqs.get(0)[1] == null && pathFindTo(dropOff, 50, false, "In Range") && rc.canSenseLocation(dropOff) && !loc.equals(dropOff) && rc.senseRobotAtLocation(dropOff) != null) {
							if(rc.senseRobotAtLocation(dropOff).type == RobotType.LANDSCAPER) {
								dropOff = landscaperSpots.remove(0);
							} else {
								landscaperSpots.add(dropOff);
								dropOff = landscaperSpots.remove(0);
							}
							rc.setIndicatorDot(dropOff, 120, 120, 120);
						}
					} catch (GameActionException e1) {
						System.out.println("Error: Drone.runMover() Failed!\nrc.senseRobotAtLocation(" + dropOff + ") Failed!");
						e1.printStackTrace();
					}
					
					// Move to drop off location
					if(pathFindTo(dropOff, 50, false, "Adj")) {
						yield();
						
						while(loc.equals(dropOff)) {
							for(Direction awayDir : Direction.allDirections()) {
								if(rc.canMove(awayDir)) {
									try {
										rc.move(awayDir);
										loc = rc.getLocation();
										yield();
										break;
									} catch (GameActionException e) {
										System.out.println("Error: Drone.runMover() Failed!\nrc.move(" + awayDir + ") Failed!");
										e.printStackTrace();
									}
								}
							}
						}

						// Drop off
						while(!rc.canDropUnit(loc.directionTo(dropOff))) {
							yield();
						}
						
						try {
							rc.dropUnit(loc.directionTo(dropOff));
						} catch (GameActionException e) {
							System.out.println("Error: Drone.runMover() Failed!\nrc.dropUnit(" + loc.directionTo(dropOff) + ") Failed!");
							e.printStackTrace();
						}
					}
				}
			}
			
			// Task completed
			moveReqs.remove(0);
		}
	}
}