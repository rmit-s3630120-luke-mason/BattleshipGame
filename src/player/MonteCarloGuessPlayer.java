package player;
import java.io.PrintStream;
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
public class MonteCarloGuessPlayer  implements Player{//Check if update neighbours works correctly, and fix the duplicate guessing, should not be able to guess same coord, check if recording of enemies ship status is correct, check if our ship status is correct
    //Get if a == 1 statements remade
    World world;
    //Holds MonteCarloPlayer's shipTypes on coordinates
    private int[][] shipBoard;//-1 = previous guesses, 0 = water, 1 = a, 2 = b, 3 = c, 4 = d, 5 = s (a-s represent ships)
    //Holds Current Guesses so far made by MonteCarlo
    private int[][] board;//0 = water, 1 = miss, 2 = hit, 3 = sunkenShip
    //Holds configurations for next guess. Highest number on board will be next guess
	private int[][] configurationsBoard;
    //holds x,y values so that the next guess will be at this coordinate since previous guess was a hit
    ArrayList<Coordinate> hitCoords;
   	//Represent the boards dimensions
    int numRow = -1;
	int numColumn = -1;
    //
    int sunkCount = 0;
    //
    int[] direction = {0,0};
    //Mode activated for one guess, if previous guess was a hit
    boolean inTargetMode = false;
    boolean reverseAdd = false;
    boolean skipPrediction = false;
    int partCheck = 0;
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
            shipBoard = new int[world.numColumn][world.numRow];
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
                    shipBoard[i][j] = 0;
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
                    shipBoard[coord.column][coord.row] = shipTypeNum;
               }
            }
            //Finds all the configurations for ALL ships in specified area.
            printBoard(configurationsBoard);
            updateConfigurationsInArea(0,numColumn,0,numRow);
            printBoard(configurationsBoard);
            System.out.println("-------------------------------");
            //Testing Purposes
           /* printBoard(shipBoard);
            board[5][5] = 1;
            configurationsBoard[5][5] = 0;
            updateNeighboursOfCell(5,5);
            printBoard(board);
            printBoard(configurationsBoard);
            board[5][5] = 3;
            configurationsBoard[5][5] = 0;
            updateNeighboursOfCell(5,5);
            printBoard(board);
            printBoard(configurationsBoard);
            board[6][4] = 1;
            configurationsBoard[6][4] = 0;
            updateNeighboursOfCell(6,4);
            printBoard(board);
            printBoard(configurationsBoard);*/

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
        if(shipBoard[i][j] > 0)
        {
            //asigning the type of ship hit via numType
            int shipType = shipBoard[i][j];
            boolean sunk = shipHit(shipType);
            if(sunk)
            {
                 answer.shipSunk = getShip(shipType);
            }
            //Assigning coord as an invalid guess coord for future guesses
            shipBoard[i][j] = -1;
            //Make answer a hit
            answer.isHit = true;
        }
        else
        {
            answer.isHit = false;
        }
       // System.out.println("\nDEBUG: OpponentGuess >> "+guess.toString());
       // System.out.println("DEBUG: Answer >> "+answer.toString());
       //printBoard(shipBoard);
        return answer;
    } // end of getAnswer()


    @Override
    public Guess makeGuess() 
    {
        //initialising variables
        Guess guess = null;
        if(inTargetMode)
        {
            System.out.println("target mode = "+inTargetMode);
            guess = new Guess();
            System.out.println("t1 = "+t1+" t2 = "+t2);
            guess.row = t2;
            guess.column = t1;
            System.out.println("DEBUG: myGuess. "+guess.toString()+"\n");
            printBoard(configurationsBoard);
            if(board[t1][t2] > 0)
            {
                System.out.println("ABOUT TO SHOOT AT BOARD INCORRECTLY!!!!!!~~~~~~~~~~~~~~~~~~~~~~~~~~~~");               
            }
            if(configurationsBoard[t1][t2] == 0)
            {
                System.out.println("ABOUTN TO SHOOT AT CONFIGS!!!!!! 0 !!!! @@@@@@@@@@@@@@@@@@@@@@@@@");
            }
            return guess;
        }
        System.out.println("target mode = "+inTargetMode);
        int highestNum = 0;
        int x = -1;
        int y = -1;
        //Getting highest number of configurations over all coordinates
        printBoard(board);
        printBoard(configurationsBoard);
        for(int i = 0; i<numColumn; i++)
        {
            for(int j = 0; j <numRow; j++)
            {
                int configs = configurationsBoard[i][j];
                if(configs>highestNum)             
                {
                    System.out.println(configs+" is bigger than "+highestNum);
                    //Assigning highest number and coordinates of that number
                    highestNum = configs;
                    x = i;
                    y = j;
                }
            }
        }
        //System.out.println("HERE 1");
        System.out.println("Ships status: a = "+a+" b = "+b+" c = "+c+" d = "+d+" s = "+s); 
        
        System.out.println("x = "+x+" y = "+y);
        guess = new Guess();
        System.out.println("Highest Configurations = "+highestNum);
        guess.row = y;
        guess.column = x;
        printBoard(configurationsBoard);
        System.out.println("DEBUG: myGuess. "+guess.toString()+"\n");
        
        /*
            if(board[x][y] > 0)
            {
                System.out.println("ABOUT TO SHOOT AT BOARD INCORRECTLY!!!!!!~~~~~~~~~~~~~~~~~~~~~~~~~~~~");               
            }
            if(configurationsBoard[x][y] == 0)
            {
                System.out.println("ABOUTN TO SHOOT AT CONFIGS!!!!!! 0 !!!! @@@@@@@@@@@@@@@@@@@@@@@@@");
            }*/
        return guess;
    } // end of makeGuess()


    @Override
    public void update(Guess guess, Answer answer) 
    {
        int i = guess.column;
        int j = guess.row;
        configurationsBoard[i][j] = 0;
        if(answer.shipSunk == null && answer.isHit && inTargetMode)//if ship is not sunk and guess was a hit and was already in targeting mode
        {
            //streak underway!
            System.out.println("hit, still in target mode, shooting in next direction");                                    
            board[i][j] = 2;
            Coordinate coord = world.new Coordinate();
            coord.column = i;
            coord.row = j;
            hitCoords.add(coord);         
            int k = coord.column + direction[0];
            int l = coord.row + direction[1];
            boolean help = false;
            if(k<numColumn && k >=0 && l<numRow && l >=0)
            {
                if(board[k][l] == 0)
                {
                    t1 = k;
                    t2 = l;   
                }else{help = true;}
            }
            else{help = true;}
            if(help)                 
            {
                //basically skips a predicted guess of that it will be a miss and goes straight to the if statement below \/
                skipPrediction = true;
            }
        }
        if((!answer.isHit && inTargetMode)||skipPrediction)//if miss but still targeting detected ship
        {
            System.out.println("Missed but still seeking");
            if(!skipPrediction)
            {
                board[i][j] = 1;
                updateNeighboursOfCell(i,j);
                //updateConfigurationsInArea(0,numColumn,0,numRow);
            }
            skipPrediction = false;
            //System.out.println("d0 = "+direction[0]);
            //System.out.println("d1 = "+direction[1]);
            //reverse direction
            direction[0] = direction[0]*-1;
            direction[1] = direction[1]*-1;
            
            //System.out.println("Reverse--");
           // System.out.println("d0 = "+direction[0]);
           // System.out.println("d1 = "+direction[1]);
            int k = hitCoords.get(partCheck).column;
            int l = hitCoords.get(partCheck).row;
            int c = k + direction[0];
            int r = l + direction[1];
            boolean help = false;
            if(c < numColumn && r < numRow && c >= 0 && r >= 0)
            {              
               System.out.println("in Reverse Already? = "+reverseAdd); 
                if(board[c][r] == 0 && !reverseAdd)
                {
                    reverseAdd = true;
                    t1 = c;
                    t2 = r;   
                }else{help = true;}                
            }else{help = true;}
            if(help)
            {
                reverseAdd = false;
                System.out.println("start searching >> "+k+","+l);
                printBoard(configurationsBoard);
                Coordinate coord = startSearchConfigs(k,l);
                System.out.println("Chose next shot = "+coord.column+","+coord.row);
                t1 = coord.column;
                t2 = coord.row;
            }                        
        }
        else if(!inTargetMode && answer.isHit)//if first hit
        {
            System.out.println("NEW HIT!");
            inTargetMode = true;
            board[i][j] = 2;
            Coordinate coord = world.new Coordinate();
            coord.column = i;
            coord.row = j;
            hitCoords = new ArrayList<>();
            hitCoords.add(coord);          
            Coordinate coord2 = startSearchConfigs(i,j); 
            t1 = coord2.column;
            t2 = coord2.row;
            System.out.println("Shooting at "+t1+","+t2);
        }
        else if(answer.shipSunk != null && answer.isHit && inTargetMode)
        {
           System.out.println("Ships status: a = "+a+" b = "+b+" c = "+c+" d = "+d+" s = "+s); 
            int type = getShipNum(answer.shipSunk.name());
            sinkShip(type);
            board[i][j] = 2;
            System.out.println("HIT AND SUNK - "+answer.shipSunk.name());            
            //printBoard(board);
            Coordinate coord = world.new Coordinate();
            coord.column = i;
            coord.row = j;
            hitCoords.add(coord);                     
            sunkCount = sunkCount + answer.shipSunk.len();            
            System.out.println("Ship Length sunk = "+answer.shipSunk.len());
            System.out.println("sunkCount = "+sunkCount+" coord List size = "+hitCoords.size());           
           System.out.println("Ships status: a = "+a+" b = "+b+" c = "+c+" d = "+d+" s = "+s); 
            if(sunkCount == hitCoords.size())
            {
                inTargetMode = false;
                System.out.println("hitCoords SIZE = "+hitCoords.size());
                for(int q = 0; q < hitCoords.size(); q++)//makes all coords sunk
                {
                    //System.out.println(hitCoords.get(q).column+","+hitCoords.get(q).row);
                    board[hitCoords.get(q).column][hitCoords.get(q).row] = 3;
                    updateNeighboursOfCell(hitCoords.get(q).column,hitCoords.get(q).row);
                    sunkCount--;
                    //System.out.println("get(0) is now "+hitCoords.get(q).column+","+hitCoords.get(q).row);
                    //System.out.println("sunkCount = "+sunkCount);
                }
                //updateConfigurationsInArea(0,numColumn,0,numRow);
                hitCoords.clear();
                printBoard(board);
                partCheck = 0; 
            }  
            else
            {
                Coordinate shipPiece = hitCoords.get(partCheck);
                Coordinate coord2 = startSearchConfigs(shipPiece.column,shipPiece.row);
                t1 = coord2.column;
                t2 = coord2.row;
            }
            reverseAdd = false;
        }

        if(!inTargetMode && !answer.isHit)
        {
            System.out.println("Shot was not a hit :( ");
           // printBoard(board);
           // printBoard(configurationsBoard);
            board[i][j] = 1;
           // printBoard(board);
            updateNeighboursOfCell(i,j);
             System.out.println("Ships status: a = "+a+" b = "+b+" c = "+c+" d = "+d+" s = "+s);
            //updateConfigurationsInArea(0,numColumn,0,numRow);
            //System.out.println("after update neighbours");
            //printBoard(configurationsBoard);
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
        for(int j = height-1; j >= 0; j--)
        {
            for(int i = 0; i < length; i++)
            {
                System.out.printf("%2d|",board[i][j]);
            }
            System.out.println("");
        }
        System.out.println("\n");
    }

    //Getting highest configuration in neighbouring 4 tiles
    public Coordinate startSearchConfigs(int x, int y)
    {
        Coordinate coord = world.new Coordinate();
        int above = -1;
        int below = -1;
        int left = -1;
        int right = -1; 
        if(x+1 < numColumn && y+1 < numRow && x-1 >= 0 && y-1 >=0)
        {
            updateConfigurationsForCell(x,y+1);
            updateConfigurationsForCell(x,y-1);
            updateConfigurationsForCell(x+1,y);
            updateConfigurationsForCell(x-1,y);
        }
        //System.out.println("Updated 4 squares around previous hit coords");
        //printBoard(configurationsBoard);
        
        if(y+1 < numColumn)
        {
            updateConfigurationsForCell(x,y+1);
            above = configurationsBoard[x][y+1];
        }
        if(y-1>=0)
        {
            updateConfigurationsForCell(x,y-1);
            below = configurationsBoard[x][y-1];
        }
        if(x+1<numColumn)
        {
            updateConfigurationsForCell(x+1,y);
            left = configurationsBoard[x+1][y];
        }
        if(x-1>=0)
        {
            updateConfigurationsForCell(x-1,y);
            right = configurationsBoard[x-1][y];
        }
        int max1 = Math.max(above,below);
        int max2 = Math.max(left,right);
        int max = Math.max(max1,max2);
        if(above == max)
        {
            direction[0] = 0;
            direction[1] = 1;
            coord.column = x;
            coord.row = y+1;
        }
        else if(below == max)
        {
            direction[0] = 0;
            direction[1] = -1;
            coord.column = x;
            coord.row = y-1;
        }
        else if(left == max)
        {
            direction[0] = 1;
            direction[1] = 0;
            coord.column = x+1;
            coord.row = y;
        }
        else if(right == max)
        {
            direction[0] = -1;
            direction[1] = 0;
            coord.column = x-1;
            coord.row = y;
        }
        if(max < 1)
        {
            System.out.println("All squares around "+x+","+y+" are 0");
            printBoard(configurationsBoard);
            printBoard(board);
            //if it gets to this stage then all 4 squares around are equal
            ++partCheck;
            Coordinate coord2 = hitCoords.get(partCheck); 
            coord = startSearchConfigs(coord2.column,coord2.row);
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
            System.out.println("a");
            total = total + configurations(5,i,j,board);
        }
        if(b == 1)
        {
            System.out.println("b");
            total = total + configurations(4,i,j,board);
        }
        if(c == 1)
        {
            System.out.println("c");
            total = total +  configurations(3,i,j,board);
        }
        if(d == 1)
        {
            System.out.println("d");
            total = total + configurations(2,i,j,board);
        }
        if(s == 1)
        {
            System.out.println("s");
            total = total + configurations(3,i,j,board);
        }        
        configurationsBoard[i][j] = total;
        /*if(total == 0 && board[i][j] != 1)
        {
            board[i][j] = 1;
            System.out.println("Assigned "+i+","+j+" as miss 1, because 0 configs for cell");
            updateNeighboursOfCell(i,j);
        }*/
    }

    public void updateNeighboursOfCell(int x, int y)
    {
        if(a == 1)
        {
            System.out.println("updating neighbours 5 away");
            updateLineOfNeighboursOfCell(5,x,y);
        }
        else if(b == 1)
        {
            System.out.println("updating neighbours 4 away");
            updateLineOfNeighboursOfCell(4,x,y);
        }
        else if(c == 1)
        {
            System.out.println("updating neighbours 3 away");
            updateLineOfNeighboursOfCell(3,x,y);
        }
        else if(s == 1)
        {
            System.out.println("updating neighbours 3 away");
            updateLineOfNeighboursOfCell(3,x,y);
        }
        else if(d == 1)
        {
            System.out.println("updating neighbours 2 away");
            updateLineOfNeighboursOfCell(2,x,y);
        }
    }
    
    public void updateLineOfNeighboursOfCell(int spanningLength,int x, int y)
    {
        //System.out.println("Updating the x values");
        for(int i = 1; i< spanningLength; i++)
        {
            if(x+i < numColumn)
            {
                //System.out.println((x+i)+","+y);
                updateConfigurationsForCell(x+i,y);
            }
            if(x-i >= 0)
            {                
                //System.out.println((x-i)+","+y);
                updateConfigurationsForCell(x-i,y);
            }
        }
        //System.out.println("Updating the y values");
        for(int i = 1; i< spanningLength; i++)
        {
            if(y+i < numRow)
            {
                //System.out.println(x+","+(y+i));
                updateConfigurationsForCell(x,y+i);
            }
            if(y-i >= 0)
            {
                //System.out.println(x+","+(y-i));
                updateConfigurationsForCell(x,y-i);
            }
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
        //printBoard(board);
        //System.out.println("\nShip Length >> "+shipLength+" Lx = "+lXBoundary+" Hx = "+hXBoundary+" Ly = "+lYBoundary+" Hy = "+hYBoundary);
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
          // System.out.println("yTopRow = "+lYBoundary+" xLeftColumn = "+lXBoundary+" yBottomRow = "+hYBoundary+" xRightColumn = "+hXBoundary); 
			for(int i = lXBoundary; i <= hXBoundary; i++)
			{

                //x-1 is so that the first part of ship length fits into the current square, not joining on the end.
                int endCoord = lXBoundary+x-1+shipLength - i;
                int beginningCoord = lXBoundary + x-i;
          //      System.out.println("BackEndX = "+beginningCoord+" FrontEndX = "+endCoord);
                if(endCoord <= hXBoundary && beginningCoord >= lXBoundary)
                {
                    boolean clean = true;
                    //checking for any missedShots or sunken ship parts along the desired horizontal ship configuration
                    for(int k = 0; k<=(endCoord - beginningCoord); k++)
                    {
                        if(board[endCoord-k][y]==1 || board[endCoord-k][y]==3)
                        {
                            clean = false;
        //                    System.out.println("No Config Added");
                            break;
                        }
                    }
                    if(clean)
                    {
      //                  System.out.println("Config Added");
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
			//HEX BOARD NOT IMPLEMENTED
		}
        numConfigs = numXConfigs + numYConfigs;
//        System.out.println("Num Of X Configurations >> "+numXConfigs);
  //      System.out.println("Num Of Y Configurations >> "+numYConfigs);
    //    System.out.println("TOTAL Num Of Configurations >> "+numConfigs);
		return numConfigs;
	} //endConfigurations()
} // end of class MonteCarloGuessPlayer
