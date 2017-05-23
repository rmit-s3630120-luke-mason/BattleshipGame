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
    World world;
    //Holds MonteCarloPlayer's shipTypes on coordinates
    private int[][] myShipBoard;//-1 = previous guesses, 0 = water, 1 = a, 2 = b, 3 = c, 4 = d, 5 = s (a-s represent ships)
    //Holds Current Guesses so far made by MonteCarlo
    private int[][] board;
    //Holds configurations for next guess. Highest number on board will be next guess
	private int[][] configurationsBoard;
   	//Represent the boards dimensions
    int numRow = -1;
	int numColumn = -1;
    //Is board hexagonal board?
	boolean isHex = false;
    //holds all ship objects
    ArrayList<Ship> allShips = new ArrayList<>();
    //lenths of each ship(max ship hit counter)
    int aircraftCarrier = 5;
    int battleship = 4;
    int submarine = 3;
    int cruiser = 3;
    int destroyer = 2;
    //These represent the Status of each ship, 1 = alive, 0 = sunk
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
            updateConfigurationsInArea(0,numColumn,0,numRow,1,1,1,1,1); 
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
        System.out.println("\nDEBUG: OpponentGuess >> "+guess.toString());
        System.out.println("DEBUG: Answer >> "+answer.toString());
        return answer;
    } // end of getAnswer()


    @Override
    public Guess makeGuess() 
    {
        //initialising variables
        Guess guess = null;
        int highestNum = 0;
        int x = -1;
        int y = -1;
        //Getting highest number of configurations over all coordinates
        for(int i = 0; i<numColumn; i++)
        {
            for(int j = 0; j <numRow; j++)
            {
                if(configurationsBoard[i][j]>highestNum)
                {
                    //Assigning highest number and coordinates of that number
                    highestNum = configurationsBoard[i][j];
                    x = i;
                    y = j;
                }
            }
        }

        if(x >= 0 && y >= 0)
        {
            guess.row = y;
            guess.column = x;
        }
        System.out.println("\nDEBUG: myGuess. "+guess.toString());
        return guess;
    } // end of makeGuess()


    @Override
    public void update(Guess guess, Answer answer) 
    {
        if(answer.isHit)
        {
           
        }       
    } // end of update()


    @Override
    public boolean noRemainingShips() {
        // To be implemented.

        // dummy return
        return true;
    } // end of noRemainingShips


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


    //Updates the configurations in left, right, top and bottom boundaries. a,b,c,d,s = Sunk Status of ships a-s
	public void updateConfigurationsInArea(int x1,int x2, int y1, int y2,int a,int b,int c, int d, int s)
    {
        for(int i = x1; i < x2; i++)
        {
            for(int j = y1; j < y2; j++)
            {
                if(a == 1)
                {
                    a = configurations(aircraftCarrier,i,j,board);
                }
                if(b == 1)
                {
                    b = configurations(battleship,i,j,board);
                }
                if(c == 1)
                {
                    c = configurations(cruiser,i,j,board);
                }
                if(d == 1)
                {
                    d = configurations(destroyer,i,j,board);
                }
                if(s == 1)
                {
                    s = configurations(submarine,i,j,board);
                }
                configurationsBoard[i][j] = a + b + c + d + s;
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
		//Returns 0 configurations since coord has already been shot at before
		if(board[x][y] != 0)
		{
			return 0;
		}
		int numConfigs = 0;
		//configuring distance from coordinate to all board edges
		
        int leftSpace = 0;
        int rightSpace = 0;
		if(!isHex)
		{
	        int aboveSpace = 0;
            int belowSpace = 0;
			if((numRow - x)>0)
			{
				rightSpace = numRow - x;
			}
			if(x - 1 > 0)
			{
				leftSpace = x - 1; //Everything left of selected Square
			}
			if((numColumn - y)>0)
			{
				aboveSpace = numColumn - y;
			}
			if(y - 1>0)
			{
				belowSpace = y-1;
			}
            if(lXBoundary < 0)
            {
                lXBoundary = 0;
            }
            if(lYBoundary < 0)
            {
                lYBoundary =0;
            }
            if(hXBoundary > numColumn)
            {
                hXBoundary = numColumn;
            }
            if(hYBoundary > numRow)
            {
                hYBoundary = numRow;
            } 
			for(int i = lXBoundary; i < hXBoundary; i++)
			{
                //x-1 is so that the first part of ship length fits into the current square, not joining on the end.
                int endCoord = x-1+shipLength - i;
                int beginningCoord = x-i;
                if(endCoord <= numColumn && beginningCoord >= 0)
                {
                    boolean clean = true;
                    //checking for any missedShots or sunken ship parts along the desired horizontal ship configuration
                    for(int k = 0; k<=(endCoord - beginningCoord); k++)
                    {
                        if(board[endCoord-k][y]==1 || board[endCoord-k][y]==3)
                        {
                            clean = false;
                            k = endCoord-beginningCoord;//break
                        }
                    }
                    if(clean)
                    {
                        ++numConfigs;
                    }
                }
                if(beginningCoord < 0)
                {
                    i = hXBoundary;
                }
            }//endFor
            for(int j = lYBoundary; j < hYBoundary; j++)
            {
                int topCoord = y-1+shipLength - j;
                int bottomCoord = y - j;
                if(topCoord <= numRow && bottomCoord >=0)
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
                        ++numConfigs;
                    }
                }
                if(bottomCoord < 0)
                {
                    j = hYBoundary; //break
                }
            }//endFor
		}//endIf
		else
		{
			//NOT IMPLEMENTED YET
		}
		return numConfigs;
	} //endConfigurations()
} // end of class MonteCarloGuessPlayer
