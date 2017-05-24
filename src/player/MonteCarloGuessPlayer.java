package player;

import java.util.*;
import ship.Ship;
import world.World;
import world.World.ShipLocation;
import world.World.Coordinate;

/**
 * Monte Carlo guess player (task C).
 * Please implement this class.
 *
 * @author Youhan, Jeffrey
 */
public class MonteCarloGuessPlayer  implements Player{
    //Re implement the opponent ship status variables
    //Get substring from answer
    //Get if a == 1 statements remade
    World world;
    //Holds MonteCarloPlayer's shipTypes on coordinates
    private int[][] myShipBoard;//-1 = previous guesses, 0 = water, 1 = a, 2 = b, 3 = c, 4 = d, 5 = s (a-s represent ships)
    //Holds Current Guesses so far made by MonteCarlo
    private int[][] board;//0 = water, 1 = miss, 2 = hit, 3 = sunkenShip
    //Holds configurations for next guess. Highest number on board will be next guess
	private int[][] configurationsBoard;
    //holds x,y values so that the next guess will be at this coordinate since previous guess was a hit
    
   	//Represent the boards dimensions
    int numRow = -1;
	int numColumn = -1;
    //Mode activated for one guess, if previous guess was a hit
    boolean targetMode = false;
    int t1 = -1;
    int t2 = -1;
    //Is board hexagonal board?
	boolean isHex = false;
    //holds all ship objects
    ArrayList<Ship> allShips = new ArrayList<>();
    //lengths of each ship(max ship hit counter) [this players ships]
    int aircraftCarrier = 5;
    int battleship = 4;
    int submarine = 3;
    int cruiser = 3;
    int destroyer = 2;
    //These represent the Status of each ship owned by opponent, 1 = alive, 0 = sunk
    int a = 1;
    int b = 1;
    int c = 1;
    int d = 1;
    int s = 1;
	@Override
    public void initialisePlayer(World world) {
    	if(!world.isHex)
	    {
            //Assigning world and board sizes
            this.world = world;
		    board = new int[world.numColumn][world.numRow];
            configurationsBoard = new int[world.numColumn][world.numRow];
            myShipBoard = new int[world.numColumn][world.numRow];
            //Assign board dimensions
            if(board.length > 0)
            {
                numRow = board.length;
                numColumn = board[0].length;	
            }
            //initialising the three boards
            for(int i =0; i< numColumn; i++)
            {
                for(int j = 0; j<numRow; j++)
                {
                    board[i][j] = 0;
                    configurationsBoard[i][j] = 0;
                    myShipBoard[i][j] = 0;
                }
            }
            //Assigning shipType Numbers to coordinates specified by world.shiplocations
            ArrayList<ShipLocation> ships = world.shipLocations;
            for(ShipLocation ship: ships)
            {
                allShips.add(ship.ship);
               int shipTypeNum = getShipNum(ship.ship.name());
               ArrayList<Coordinate> coords = ship.coordinates;
               for(Coordinate coord: coords)
               { 
                    myShipBoard[coord.column][coord.row] = shipTypeNum;
               }
            }
            //Finds all the configurations for ALL ships in specified area.
            printBoard(configurationsBoard);
            updateConfigurationsInArea(0,numColumn,0,numRow);
            printBoard(configurationsBoard);
        }
        else
        {
            isHex = true;
            //board = 
        }
    } // end of initialisePlayer()

    @Override
    public Answer getAnswer(Guess guess) {
        //initialising
        Answer answer = new Answer();
        Ship ship = null;
        answer.shipSunk = ship;
        int i = guess.column;
        int j = guess.row;
        //if coordinate = a ship type number (bigger than 0)
        if(myShipBoard[i][j] > 0)
        {
            //asigning the type of ship hit via numType
            int shipType = myShipBoard[i][j];
            boolean sunk = shipHit(shipType);
            if(sunk)
            {
                 answer.shipSunk = getShip(shipType);
            }
            //Assigning coord as an invalid guess coord for future guesses
            myShipBoard[i][j] = -1;
            //Make answer a hit
            answer.isHit = true;
        }
        else
        {
            answer.isHit = false;
        }
       // System.out.println("\nDEBUG: OpponentGuess >> "+guess.toString());
       // System.out.println("DEBUG: Answer >> "+answer.toString());
       // printBoard(myShipBoard);
        return answer;
    } // end of getAnswer()


    @Override
    public Guess makeGuess() 
    {
        //initialising variables
        Guess guess = null;
        if(targetMode)
        {
            guess = new Guess();
            guess.row = t2;
            guess.column = t1;
            targetMode = false;
            System.out.println("\nDEBUG: myGuess. "+guess.toString());
            return guess;
        }
        int highestNum = 0;
        int x = -1;
        int y = -1;
        //Getting highest number of configurations over all coordinates
        for(int i = 0; i<numColumn; i++)
        {
            for(int j = 0; j <numRow; j++)
            {
                int configs = configurationsBoard[i][j];
                if(configs>highestNum)
                {
                    //Assigning highest number and coordinates of that number
                    highestNum = configs;
                    x = i;
                    y = j;
                }
            }
        }
        if(x >= 0 && y >= 0)
        {
            guess = new Guess();
            System.out.println("\nHighest Configurations = "+highestNum);
            guess.row = y;
            guess.column = x;
            //printBoard(configurationsBoard);
            configurationsBoard[x][y] = 0;
        }
        System.out.println("DEBUG: myGuess. "+guess.toString());

        return guess;
    } // end of makeGuess()


    @Override
    public void update(Guess guess, Answer answer) 
    {
        int i = guess.column;
        int j = guess.row;
        if(answer.isHit)
        {
           if(answer.shipSunk != null)
           {
               board[i][j] = 3;
               int type = getShipNum(answer.shipSunk.name());
               sinkShip(type);
           }
           else
           {
               board[i][j] = 2;
               targetMode = true;
               Coordinate coord = updateConfigs(i,j); 
               if(coord != null)
               {
                   t1 = coord.column;
                   t2 = coord.row;
               }
           }
        }
        else
        {
            printBoard(configurationsBoard);
            board[i][j] = 1;
            updateNeighboursOfCell(i,j);
            printBoard(configurationsBoard);
        }
    } // end of update()


    @Override
    public boolean noRemainingShips() {
        if(aircraftCarrier < 1 && battleship < 1 && cruiser < 1 && destroyer < 1 && submarine < 1)
        {
            return true;
        }
        else
        {
            return false;
        }
    } // end of noRemainingShips
    
    public void printBoard(int[][] board)
    {
        int length = board.length;
        int height = board[0].length;
        for(int j = 0; j< height; j++)
        {
            for(int i = 0; i < length; i++)
            {
                System.out.print(board[i][j]+"|");
            }
            System.out.println("");
        }
        System.out.println("\n");
    }

    //Getting highest configuration in neighbouring 4 tiles
    public Coordinate updateConfigs(int x, int y)
    {
        Coordinate coord = world.new Coordinate();

        updateConfigurationsForCell(x,y+1);
        updateConfigurationsForCell(x,y-1);
        updateConfigurationsForCell(x+1,y);
        updateConfigurationsForCell(x-1,y);

        int above = configurationsBoard[x][y+1];
        int below = configurationsBoard[x][y-1];
        int left = configurationsBoard[x+1][y];
        int right = configurationsBoard[x-1][y];
        
        if(above > below && above > left && above > right)
        {
            coord.column = x;
            coord.row = y+1;
        }
        else if(below > above && below > left && below > right)
        {
            coord.column = x;
            coord.row = y-1;
        }
        else if(left > above && left > right && left > below)
        {
            coord.column = x+1;
            coord.row = y;
        }
        else if(right > left && right > above && right > below)
        {
            coord.column = x-1;
            coord.row = y;
        }
        else
        {
            //if it gets to this stage then all 4 squares around are equal
           if(above == 0)
           {
               return null;
           }
            coord.column = x;
            coord.row = y+1;
        }
        return coord;
    }
    
   public void sinkShip(int shipNum)
    {
        switch(shipNum)
        {
            case 1: a = 0;break;
            case 2: b = 0;break;
            case 3: c = 0;break;
            case 4: d = 0;break;
            case 5: s = 0;break;
        }
    }

    //minuses 1 from the ship and checks if it has sunk
    public boolean shipHit(int shipType)
    {
        boolean sunk = false;
        if(shipType == 1){
           aircraftCarrier--;
           if(aircraftCarrier < 1)
           {
                sunk = true;
           }
        }
        else if(shipType == 2){
            battleship--;
            if(battleship < 1)
            {
                sunk = true;
            }
        }
        else if(shipType == 3){
           cruiser--;
           if(cruiser < 1)
           {
               sunk = true;
           }
        }
        else if(shipType == 4){
            destroyer--;
            if(destroyer < 1)
            {
                sunk = true;
            } 
        }
        else if(shipType == 5){
            submarine--;
            if(submarine < 1)
            {
                sunk = true;
            }
        }
        return sunk;
    }

    //Returns ship object matching shipType number
    public Ship getShip(int shipType)
    {
        Ship ship = null;
        for(Ship ship2: allShips)
        {
            int shipNum = getShipNum(ship2.name());
            if(shipNum == shipType)
            {
                ship = ship2;
            }
        }
       return ship;
    }


    //returns number matching shipType name
    public int getShipNum(String shipName)
    {
        int shipNum = 0;
        switch(shipName)
        {
            case "AircraftCarrier":   shipNum = 1;break;
            case "Battleship":   shipNum = 2;break;
            case "Cruiser":   shipNum = 3;break;
            case "Destroyer":   shipNum = 4;break;
            case "Submarine":   shipNum = 5;break;
            //default: shipNum = 0;break;
        }
        return shipNum;
    }

    public void updateConfigurationsForCell(int i, int j)
    {
        int total = 0;
        if(a == 1)
        {
            total = total + configurations(aircraftCarrier,i,j,board);
        }
        if(b == 1)
        {
            total = total + configurations(battleship,i,j,board);
        }
        if(c == 1)
        {
            total = total +  configurations(cruiser,i,j,board);
        }
        if(d == 1)
        {
            total = total + configurations(destroyer,i,j,board);
        }
        if(s == 1)
        {
            total = total + configurations(submarine,i,j,board);
        }
        configurationsBoard[i][j] = total;
    }

    public void updateNeighboursOfCell(int x, int y)
    {
        if(a == 1)
        {
            updateLineOfNeighboursOfCell(5,x,y);
        }
        else if(b == 1)
        {
            updateLineOfNeighboursOfCell(4,x,y);
        }
        else if(c == 1)
        {
            updateLineOfNeighboursOfCell(3,x,y);
        }
        else if(s == 1)
        {
            updateLineOfNeighboursOfCell(3,x,y);
        }
        else if(d == 1)
        {
             updateLineOfNeighboursOfCell(2,x,y);
        }
    }
    
    public void updateLineOfNeighboursOfCell(int spanningLength,int x, int y)
    {
        for(int i = 1; i<= spanningLength; i++)
        {
            updateConfigurationsForCell(x+i,y);
            updateConfigurationsForCell(x-i,y);
        }
        for(int i = 1; i<= spanningLength; i++)
        {
            updateConfigurationsForCell(x,y+i);
            updateConfigurationsForCell(x,y-i);
        }
    }

    //Updates the configurations in left, right, top and bottom boundaries. a,b,c,d,s = Sunk Status of ships a-s
	public void updateConfigurationsInArea(int x1,int x2, int y1, int y2)
    {
        for(int i = x1; i < x2; i++)
        {
            for(int j = y1; j < y2; j++)
            {
               updateConfigurationsForCell(i,j);
            }
        }
    }


	// Gets number of configurations for the length of ship in specified coordinate Over the given board	
	public int configurations(int shipLength, int x, int y,int[][] board)
	{
        int lXBoundary = x-shipLength+1;
        int lYBoundary = y-shipLength+1;
        int hXBoundary = x+shipLength-1;
        int hYBoundary = y+shipLength-1;
        printBoard(board);
        System.out.println("\nShip Length >> "+shipLength+" Lx = "+lXBoundary+" Hx = "+hXBoundary+" Ly = "+lYBoundary+" Hy = "+hYBoundary);
		//Returns 0 configurations since coord has already been shot at before
		if(board[x][y] != 0)
		{
			return 0;
		}
		int numConfigs = 0;
        int numXConfigs = 0;
        int numYConfigs = 0;
		//configuring distance from coordinate to all board edges
		if(!isHex)
		{	
            if(lXBoundary < 0)
            {
                lXBoundary = 0;
            }
            if(lYBoundary < 0)
            {
                lYBoundary =0;
            }
            if(hXBoundary > numColumn-1)
            {
                hXBoundary = numColumn-1;
            }
            if(hYBoundary > numRow-1)
            {
                hYBoundary = numRow-1;
            }
           System.out.println("yTopRow = "+lYBoundary+" xLeftColumn = "+lXBoundary+" yBottomRow = "+hYBoundary+" xRightColumn = "+hXBoundary); 
			for(int i = lXBoundary; i <= hXBoundary; i++)
			{

                //x-1 is so that the first part of ship length fits into the current square, not joining on the end.
                int endCoord = lXBoundary+x-1+shipLength - i;
                int beginningCoord = lXBoundary + x-i;
                System.out.println("BackEndX = "+beginningCoord+" FrontEndX = "+endCoord);
                if(endCoord <= hXBoundary && beginningCoord >= lXBoundary)
                {
                    boolean clean = true;
                    //checking for any missedShots or sunken ship parts along the desired horizontal ship configuration
                    for(int k = 0; k<=(endCoord - beginningCoord); k++)
                    {
                        if(board[endCoord-k][y]==1 || board[endCoord-k][y]==3)
                        {
                            clean = false;
                            System.out.println("No Config Added");
                            break;
                        }
                    }
                    if(clean)
                    {
                        System.out.println("Config Added");
                        ++numXConfigs;
                    }
                } 
            }//endFor
            for(int j = lYBoundary; j <= hYBoundary; j++)
            {
                int topCoord = lYBoundary+y-1+shipLength - j;
                int bottomCoord = lYBoundary+y - j;
                if(topCoord <= hYBoundary && bottomCoord >=lYBoundary)
                {
                    boolean clean = true;
                    //Checking for any missedShots or sunken ship parts along the desired vertical ship configuration
                    for(int k = 0; k<=(topCoord - bottomCoord); k++)
                    {
                        if(board[x][topCoord-k]==1 || board[x][topCoord-k]==3)
                        {	
                            clean = false;	
                            k = topCoord-bottomCoord;//break
                        }	
                    }
                    if(clean)
                    {
                        ++numYConfigs;
                    }
                }           
            }//endFor
		}//endIf
		else
		{
			//NOT IMPLEMENTED YET
		}
        numConfigs = numXConfigs + numYConfigs;
        System.out.println("Num Of X Configurations >> "+numXConfigs);
        System.out.println("Num Of Y Configurations >> "+numYConfigs);
        System.out.println("TOTAL Num Of Configurations >> "+numConfigs);
		return numConfigs;
	} //endConfigurations()
} // end of class MonteCarloGuessPlayer
