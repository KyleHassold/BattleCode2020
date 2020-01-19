package soupbot;
import battlecode.common.*;
import java.util.ArrayList;
public strictfp class RobotPlayer {
    static RobotController rc;
    static MapLocation spawn;
	static int teamid;

    static Direction heading;
    static int miners = 0;
	static int center = 0;
	static int school = 0;

	static MapLocation enemy;

	static boolean build_center = false;
	static boolean build_school = false;

    static Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST
    };
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        RobotPlayer.spawn = rc.getLocation();
        heading = randomValidDirection();
        turnCount = 0;
		teamid = rc.getTeam().hashCode();
        
        System.out.println("I'm a " + rc.getType() + " and I just got created! Heading " + heading);
        while (true) {
            turnCount += 1;
            float cdt = rc.getCooldownTurns();
            if (cdt >= 1) {
				System.out.println("Cooling down for " + cdt + " turns");
            	Clock.yield();
			}
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                //System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
				readBlock();
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    case REFINERY:           runRefinery();          break;
                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    case NET_GUN:            runNetGun();            break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
        System.out.println("Hello World From HQ\nOrigin:" + spawn + "\nTeam Soup: " + rc.getTeamSoup());

		RobotInfo[] info = rc.senseNearbyRobots();
		
		shootDrone(info);

		if (miners > 5 && center < 1 && rc.getTeamSoup() >= 160) {
			Team myTeam = rc.getTeam();
			for (RobotInfo rob : info)
				if (rob.getTeam() == myTeam && rob.getType() == RobotType.MINER)
					if (trySendMessageToRobot(rob.getID(), 2, 10))
						break;

		 } else if (miners > 5 && school < 1 && rc.getTeamSoup() >= 160) {
			Team myTeam = rc.getTeam();
			for (RobotInfo rob : info)
				if (rob.getTeam() == myTeam && rob.getType() == RobotType.MINER)
					if (trySendMessageToRobot(rob.getID(), 3, 10))
						break;

		} else if (((center > 0 && school > 0) || miners < 6) &&  miners < 8)
        	for (Direction dir : directions) 
            	if (tryBuild(RobotType.MINER, dir))
               		System.out.println("Built Miner " + ++miners + " in " + dir);
		
		System.out.println("Goodnight From HQ");
    }

    static void runMiner() throws GameActionException {
        System.out.println("Hello World From Miner\nOrigin:" + spawn + "\nHeading " + heading + "\nSoup: " + rc.getSoupCarrying());

		if (safeMove())
            System.out.println("I moved to " + rc.getLocation() + " to find safety!"); 

		if (build_center && rc.getTeamSoup() >= 160) 
			for (Direction dir : directions) 
				if (tryBuild(RobotType.FULFILLMENT_CENTER, dir)) {
					System.out.println("Built Fulfillment Center in " + dir); 			
		        	build_center = false;
 					if (!trySendSignal(2, 10)) 
						System.out.println("ERROR: Could Not Notify Team");	
				}


		if (build_school && rc.getTeamSoup() >= 160) 
			for (Direction dir : directions) 
				if (tryBuild(RobotType.DESIGN_SCHOOL, dir)) {
					System.out.println("Built Design School in " + dir); 			
		        	build_school = false;
 					if (!trySendSignal(3, 10)) 
						System.out.println("ERROR: Could Not Notify Team");	
				}

        for (Direction dir : directions)
           if (tryMine(dir))
              System.out.println("I mined soup! " + rc.getSoupCarrying());
       
        for (Direction dir : directions)
 			if (tryRefine(dir))
				System.out.println("I refined soup! " + rc.getTeamSoup());

        int soup = rc.getSoupCarrying();
		
		if (!rc.isReady()); //Don't Move

		else if (soup == 0 && tryMove(findPath(findSoup()))) //Move Towards Soup
            System.out.println("I moved to " + rc.getLocation() + " to find soup!");
        else if (soup > 0 && tryMove(findPath(spawn))) //Move Towards Refinery
			System.out.println("I moved to " + rc.getLocation() + " to refine soup!"); 
		else if (tryMove(heading)) //Move in random heading
        	System.out.println("I moved " + heading + "!");	
       	else { //Change Heading
			heading = randomValidDirection();
            System.out.println("Heading changed to " + heading);
        	if (tryMove(heading)) 
        		System.out.println("I moved " + heading + "!");	
		}

        System.out.println("Goodnight From Miner");
    }

    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
        for (Direction dir : directions)
            tryBuild(RobotType.LANDSCAPER, dir);
    }

    static void runFulfillmentCenter() throws GameActionException {
        for (Direction dir : directions)
            tryBuild(RobotType.DELIVERY_DRONE, dir);
    }

    static void runLandscaper() throws GameActionException {

    }

    static void runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
        } else {
            // No close robots, so search for robots within sight radius
            tryMove(randomDirection());
        }
    }

    static void runNetGun() throws GameActionException {

    }

	static void shootDrone(RobotInfo[] info) throws GameActionException {
		Team opp = rc.getTeam().opponent();
		for (RobotInfo rob : info)
			if (rob.getTeam() == opp && rob.getType() == RobotType.DELIVERY_DRONE && tryShoot(rob.getID()))
				System.out.println("Shot Enemy Drone " + rob.getID() + "!");
	}

    static Direction randomValidDirection() {
    	ArrayList<Direction> list = new ArrayList<Direction>();
        for (Direction dir : directions) 
			if (rc.canMove(dir)) list.add(dir);  
		
 		if (list.isEmpty()) {
			System.out.println("Oh no I am stuck"); 
			return randomDirection();
		} else
			return list.get((int) (Math.random() * list.size()));	
    }
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
            rc.move(dir);
            return true;
        } else return false;
    }

    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }

    static void tryBlockchain() throws GameActionException {
        if (turnCount < 3) {
            int[] message = new int[7];
            for (int i = 0; i < 7; i++) {
                message[i] = 123;
            }
            if (rc.canSubmitTransaction(message, 10))
                rc.submitTransaction(message, 10);
        }
        // System.out.println(rc.getRoundMessages(turnCount-1));
    }

	static boolean tryShoot(int id) throws GameActionException {
		if (rc.isReady() && rc.canShootUnit(id)) {
			rc.shootUnit(id);
			return true;
		} return false;
	}

    static Direction findPath(MapLocation dest) throws GameActionException {	
    	Direction opt = rc.getLocation().directionTo(dest);
		if (rc.canMove(opt)) return opt;
		if (rc.canMove(opt.rotateLeft())) return opt.rotateLeft();
		if (rc.canMove(opt.rotateRight())) return opt.rotateRight(); 
		return opt;     
	}

	static MapLocation findSoup() throws GameActionException {
        MapLocation loc = rc.getLocation();
		MapLocation[] locs = rc.senseNearbySoup();
		if (locs.length == 0)
			return loc.add(heading);         
		else 
			return locs[(int) Math.random() * locs.length];
		
		/*Closest Soup Method Not Ideal: Clusters Miners
		MapLocation close = locs[0];
		int dist = loc.distanceSquaredTo(close);
		for (int i = 1; i < locs.length; i++) {
			int temp = loc.distanceSquaredTo(locs[i]); 
			if (temp < dist) {
				close = locs[i];
				dist = temp;
			}
		}
		return close;*/
	}
	
	static boolean safeMove() throws GameActionException {
		MapLocation loc = rc.getLocation();
		for (Direction dir : directions) { 
			MapLocation search = loc.add(dir);
			if (rc.canSenseLocation(search) && rc.senseFlooding(search)) {
				Direction mov = dir.opposite();
				heading = mov;
				if (tryMove(mov)) return true;
				else if (tryMove(mov.rotateLeft())) return true;
				else if (tryMove(mov.rotateRight())) return true;
			} 
		}
		return false;
	}

	static void readBlock() throws GameActionException {
		int round = rc.getRoundNum();
		if (round == 1) return;

		Transaction[] rec = rc.getBlock(round - 1);
		for (Transaction t: rec) {

			int[] mess = t.getMessage();
			if (mess[0] != teamid) continue; //Wrong Team Message
			switch (mess[1]) {
				case 0: //Message Is For A Specific Robot
					if (mess[2] == rc.getID()) {
						System.out.println("Received Message " + mess[3]);
						if (mess[3] == 2) build_center = true;
						if (mess[3] == 3) build_school = true;
					}
					break; 
				case 1: //Found HQ
					enemy = new MapLocation(mess[2], mess[3]); 
					break;
				case 2: //Fulfillment Center Built
					center++;
					break;
				case 3: //Design School Built
					school++;
					break;
				default:
					System.out.println("Unknown Team Message Code " + mess[1]);
					break;
			}
		}				
	}
	
	static boolean trySendMessage(int[] message, int cost) throws GameActionException {
		if (rc.canSubmitTransaction(message, cost)) {
			System.out.println("Sending Message" + message + " For " + cost + "Soup");
			rc.submitTransaction(message, cost);
			return true;
		}
		return false;
		
	}

	static boolean trySendMessageToRobot(int id, int m, int cost) throws GameActionException {
		int[] message = {teamid, 0, id, m, 0, 0, 0};
		return trySendMessage(message, cost); 
	}

	static boolean trySendSignal(int sig, int cost) throws GameActionException {
		int[] message = {teamid, sig, 0, 0, 0, 0, 0};
		return trySendMessage(message, cost);
	}	
}
