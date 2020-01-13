package sprintbot;

import battlecode.common.*;
import battlecode.world.GameWorld;
import java.util.*;

//-------------------------------------------------- INFO --------------------------------------

/*
BLOCKCHAIN CODES:
    117290 = Soup
    117291 = HQ Location
    117292 = Mine Location
    117293 = Design School Location
    117294 = Vaporator Location

BLOCKCHAIN PROTOCOL:
    for soup:
        [code, x, y, amountOfSoup, -1, -1, -1]
    for other stuff:
        [code, x, y, -1, -1, -1, -1]

 */



public strictfp class RobotPlayer {

    // --------------------------------------- PRIVATE DATA ---------------------------------------
    static RobotController rc;
    static Random rand = new Random();

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
    static Map<Direction, Direction[]> searchdirdict = new HashMap<Direction, Direction[]>();

    static List<MapLocation> soupLocs = new ArrayList<MapLocation>();
    static List<MapLocation> hqLocs = new ArrayList<MapLocation>();
    static List<MapLocation> mineLocs = new ArrayList<MapLocation>();
    static List<MapLocation> designSchoolLocs = new ArrayList<MapLocation>();
    static List<MapLocation> vaporatorLocs = new ArrayList<MapLocation>();

    static int turnCount;
    static Direction startDir = null;

    static MapLocation currentObjective = null;
    static String currentPurpose = "";



    // ------------------------------ RUN METHOD -----------------------------------------
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        RobotPlayer.rc = rc;
        turnCount = 0;
        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        precompute();

        while (true) {
            turnCount += 1;
            try {

                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case HQ:
                        runHQ();
                        break;
                    case MINER:
                        runMiner();
                        break;
                    case REFINERY:
                        runRefinery();
                        break;
                    case VAPORATOR:
                        runVaporator();
                        break;
                    case DESIGN_SCHOOL:
                        runDesignSchool();
                        break;
                    case FULFILLMENT_CENTER:
                        runFulfillmentCenter();
                        break;
                    case LANDSCAPER:
                        runLandscaper();
                        break;
                    case DELIVERY_DRONE:
                        runDeliveryDrone();
                        break;
                    case NET_GUN:
                        runNetGun();
                        break;
                }

                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    //----------------------------- RUN METHODS --------------------------------------
    static void precompute(){
        searchdirdict.put(Direction.NORTH, new Direction[]{Direction.WEST, Direction.EAST});
        searchdirdict.put(Direction.NORTHWEST, new Direction[]{Direction.SOUTHWEST, Direction.NORTHEAST});
        searchdirdict.put(Direction.WEST, new Direction[]{Direction.SOUTH, Direction.NORTH});
        searchdirdict.put(Direction.SOUTHWEST, new Direction[]{Direction.SOUTHEAST, Direction.NORTHWEST});
        searchdirdict.put(Direction.SOUTH, new Direction[]{Direction.EAST, Direction.WEST});
        searchdirdict.put(Direction.SOUTHEAST, new Direction[]{Direction.NORTHEAST, Direction.SOUTHWEST});
        searchdirdict.put(Direction.EAST, new Direction[]{Direction.NORTH, Direction.SOUTH});
        searchdirdict.put(Direction.NORTHEAST, new Direction[]{Direction.NORTHWEST, Direction.SOUTHEAST});

    }

    static void runHQ() throws GameActionException {
        System.out.println("By Rasputin's beard, may the USSR reign supreme.");

        List<Direction> threedirs = getExplorerDirections(rc.getMapWidth()/2, rc.getMapHeight()/2);
        if (rc.getRobotCount() < 4)
        {
            if (rc.isReady() && rc.canBuildRobot(RobotType.MINER, threedirs.get(rc.getRobotCount()-1)))
            {
                rc.buildRobot(RobotType.MINER, threedirs.get(rc.getRobotCount()-1));
            }
        }
        else if (rc.getRobotCount() < 8)
        {
            Random rand = new Random();
            for (Direction d : threedirs)
            {
                if (rc.isReady() && rc.canBuildRobot(RobotType.MINER, d))
                {
                    rc.buildRobot(RobotType.MINER, d);
                }
            }
        }
    }

    static void runMiner() throws GameActionException {
        if (startDir == null)
            startDir = directionFromSpawned();

        updateBlockchainInfo();

        if (soupLocs.size() <= 0) {

            System.out.println("Searching for soup, to feed all of Mother Russia");
            // Look for soup within a 5x5 box of current location, send to blockchain if possible

            for (int i = -2; i < 3; i++)
            {
                for (int j = -2; j < 3; j++)
                {
                    MapLocation senseloc = new MapLocation(rc.getLocation().x + i, rc.getLocation().y + j);
                    if (rc.canSenseLocation(senseloc)) {
                        int soupcount = rc.senseSoup(senseloc);
                        if (soupcount > 0) {

                            System.out.println("Soup spotted comrade! We shall alert the Kremlin at once");
                            int[] message = {117290, senseloc.x, senseloc.y, soupcount, -1, -1, -1};

                            if (rc.canSubmitTransaction(message, 15))
                                rc.submitTransaction(message, 15);
                            currentPurpose = "soup";
                            currentObjective = senseloc;

                        }
                    }
                }
            }

            System.out.println("Onwards, fellow soviets! We shall seize the means of production");

            Direction nextDir = startDir;
            double chance = rand.nextDouble();
            if (chance > 0.3) {
                Direction[] lrdirs = searchdirdict.get(startDir);
                nextDir = lrdirs[rand.nextInt(2)];
            }

            if (rc.isReady() && rc.canMove(nextDir)) {
                rc.move(nextDir);
            }

        }
        else
        {
            System.out.println("There is soup to be found, comrade!");
            if (currentPurpose.equals(""))
            {
                currentPurpose = "soup";
                currentObjective = getClosestSoup();
            }

            if (currentPurpose.equals("soup")) {
                if (soupLocs.contains(currentObjective))
                    soupLocs.remove(currentObjective);

                if (rc.getLocation() == currentObjective)
                {
                    if (rc.canMineSoup(Direction.CENTER))
                    {
                        rc.mineSoup(Direction.CENTER);
                    }
                    currentPurpose = "";
                    currentObjective = null;
                }


                System.out.println("We will scope out the best direction to proceed, sir");
                int cx = rc.getLocation().x;
                int cy = rc.getLocation().y;

                int[][] sptm = {{cx, cy}, {cx + 1, cy}, {cx - 1, cy}, {cx, cy + 1}, {cx, cy - 1}, {cx + 1, cy + 1}, {cx - 1, cy - 1}, {cx + 1, cy - 1}, {cx - 1, cy + 1}};
                List<int[]> spotsToMove = Arrays.asList(sptm);

                Direction[] dtm = {Direction.CENTER, Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.SOUTHWEST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.NORTHWEST};
                List<Direction> dirsToMove = Arrays.asList(dtm);

                List<Integer> spotscores = new ArrayList<Integer>();

                for (int[] sp : spotsToMove) {
                    spotscores.add(Math.abs(currentObjective.x - sp[0]) + Math.abs(currentObjective.y - sp[0]));
                }
                int scoretobeat = spotscores.get(0);


                dirsToMove.sort(new Comparator<Direction>() {
                    @Override
                    public int compare(Direction arg0, Direction arg1) {
                        return spotscores.get(dirsToMove.indexOf(arg1)) - spotscores.get(dirsToMove.indexOf(arg0));
                    }
                });
                Collections.sort(spotscores);

                int ct=0;
                boolean movedToBetterPlace = false;
                for (Direction dir : dirsToMove)
                {
                    boolean tttr = rc.isReady();
                    boolean tttv = spotscores.get(ct) < scoretobeat;
                    boolean ttty = rc.canMove(dir);
                    if (rc.isReady() && spotscores.get(ct) < scoretobeat && rc.canMove(dir)) {
                        rc.move(dir);
                        movedToBetterPlace = true;
                        break;
                    }
                    ct++;
                }
                if (!movedToBetterPlace)
                {
                    Direction randDir = directions[rand.nextInt(directions.length)];
                    if (rc.isReady() && rc.canMove(randDir))
                    {
                        rc.move(randDir);
                    }
                }

            }


        }

    }

    static void runRefinery() throws GameActionException {

    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {

    }

    static void runFulfillmentCenter() throws GameActionException {

    }

    static void runLandscaper() throws GameActionException {

    }

    static void runDeliveryDrone() throws GameActionException {

    }

    static void runNetGun() throws GameActionException {

    }

    //--------------------------------- HELPER METHODS --------------------------------------------
    static List<Direction> getExplorerDirections(int centerx, int centery) {

        Direction zeroD = Direction.NORTH;
        int px = rc.getLocation().x;
        int py = rc.getLocation().y;

        double theta = Math.atan2((double)(centery-py), (double)(centerx-px));
        if (theta < 0)
            theta = theta + 2*Math.PI;

        if (theta <= Math.PI/8 || theta > 15*Math.PI/8)
            zeroD = Direction.EAST;
        else if (theta <= 3*Math.PI/8)
            zeroD = Direction.NORTHEAST;
        else if (theta <= 5*Math.PI/8)
            zeroD = Direction.NORTH;
        else if (theta <= 7*Math.PI/8)
            zeroD = Direction.NORTHWEST;
        else if (theta <= 9*Math.PI/8)
            zeroD = Direction.WEST;
        else if (theta <= 11*Math.PI/8)
            zeroD = Direction.SOUTHWEST;
        else if (theta <= 13*Math.PI/8)
            zeroD = Direction.SOUTH;
        else if (theta <= 15*Math.PI/8)
            zeroD = Direction.SOUTHEAST;

        Direction[] out = {zeroD, searchdirdict.get(zeroD)[0], searchdirdict.get(zeroD)[1]};
        return Arrays.asList(out);

    }

    static Direction directionFromSpawned() {
        if (rc.getType() == RobotType.MINER)
        {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
            for (RobotInfo nr : nearbyRobots)
            {
                int rx = rc.getLocation().x;
                int ry = rc.getLocation().y;
                int hx = nr.location.x;
                int hy = nr.location.y;

                if (nr.type == RobotType.HQ)
                {
                    if (rx - hx == 0 && ry - hy == 1)
                        return Direction.NORTH;
                    else if (rx - hx == 1 && ry - hy == 0)
                        return Direction.EAST;
                    else if (rx - hx == 0 && ry - hy == -1)
                        return Direction.SOUTH;
                    else if (rx - hx == -1 && ry - hy == 0)
                        return Direction.WEST;
                    else if (rx - hx == 1 && ry - hy == 1)
                        return Direction.NORTHEAST;
                    else if (rx - hx == -1 && ry - hy == -1)
                        return Direction.SOUTHWEST;
                    else if (rx - hx == -1 && ry - hy == 1)
                        return Direction.NORTHWEST;
                    else if (rx - hx == 1 && ry - hy == -1)
                        return Direction.SOUTHEAST;
                }
            }
        }
        return Direction.NORTH;
    }

    static void updateBlockchainInfo() throws GameActionException {
        Transaction[] lastTransactions = rc.getBlock(rc.getRoundNum()-1);
        for (Transaction tr : lastTransactions)
        {
            int[] trmessage = tr.getMessage();
            if (trmessage[0] == 117290)
            {
                soupLocs.add(new MapLocation(trmessage[1], trmessage[2]));
            }
            if (trmessage[0] == 117291)
            {
                hqLocs.add(new MapLocation(trmessage[1], trmessage[2]));
            }
            if (trmessage[0] == 117292)
            {
                mineLocs.add(new MapLocation(trmessage[1], trmessage[2]));
            }
            if (trmessage[0] == 117293)
            {
                designSchoolLocs.add(new MapLocation(trmessage[1], trmessage[2]));
            }
            if (trmessage[0] == 117290)
            {
                vaporatorLocs.add(new MapLocation(trmessage[1], trmessage[2]));
            }
        }
    }

    static MapLocation getClosestSoup() {
        soupLocs.sort(new Comparator<MapLocation>() {
            @Override
            public int compare(MapLocation arg0, MapLocation arg1) {
                return (arg1.x+arg1.y) - (arg0.x+arg0.y);
            }
        });
        return soupLocs.get(0);
    }
}