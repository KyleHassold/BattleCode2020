package soupbot;
import battlecode.common.*;
import java.util.ArrayList;
public strictfp class RobotPlayer {
    static RobotController rc;
    static MapLocation spawn;

    static Direction heading;
    static int miners = 0;

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
        
        for (Direction dir : directions) 
            if (miners < 8 && tryBuild(RobotType.MINER, dir))
               System.out.println("Built Miner " + ++miners + " in " + dir);
    }

    static void runMiner() throws GameActionException {
        System.out.println("Hello World From Miner\nOrigin:" + spawn + "\nHeading " + heading + "\nSoup: " + rc.getSoupCarrying());
        
        boolean mining = false;
        for (Direction dir : directions)
           if (tryMine(dir)) {
              System.out.println("I mined soup! " + rc.getSoupCarrying());
              mining = true;
           }
       
		boolean refining = false; 
        for (Direction dir : directions)
 			if (tryRefine(dir)) {
				System.out.println("I refined soup! " + rc.getTeamSoup());
				refining = true;
        	}

        if (mining) return; //Don't move if mining soup

        int soup = rc.getSoupCarrying();
        if (safeMove())
            System.out.println("I moved to " + rc.getLocation() + " to find safety!"); 
		if (soup == 0 && tryMove(findPath(findSoup()))) //Move Towards Soup
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

        System.out.println("Goodbye From Miner");
    }

    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {

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

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
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

    /**
     * Returns a random RobotType spawned by miners.
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
        // MapLocation loc = rc.getLocation();
        // if (loc.x < 10 && loc.x < loc.y)
        //     return tryMove(Direction.EAST);
        // else if (loc.x < 10)
        //     return tryMove(Direction.SOUTH);
        // else if (loc.x > loc.y)
        //     return tryMove(Direction.WEST);
        // else
        //     return tryMove(Direction.NORTH);
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
            rc.move(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
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


    static Direction findPath(MapLocation dest) throws GameActionException {
    	return rc.getLocation().directionTo(dest);     
	}

	static MapLocation findSoup() throws GameActionException {
		int rs = rc.getCurrentSensorRadiusSquared();
        int r = (int) Math.sqrt(rs);
        MapLocation loc = rc.getLocation();
        for (int i = -r; i < r; i++)
        	for (int j = -r; j < r; j++) {
				MapLocation search = loc.translate(i,j);
                if (rc.canSenseLocation(search) && rc.senseSoup(search) > 0) { 
					System.out.println("Soup Found At " + search + "!");
					return search;
				}				
			}
		return loc.add(heading);         
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
}
