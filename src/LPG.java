import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;
import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.linear.LinearConstraint;
import org.apache.commons.math.optimization.linear.LinearObjectiveFunction;
import org.apache.commons.math.optimization.linear.Relationship;
import org.apache.commons.math.optimization.linear.SimplexSolver;

@SuppressWarnings("deprecation")
public class LPG {

	public static void main(String[] args) {
		try {
			Scanner scan = new Scanner(System.in);
			System.out.print("Enter number of points: ");
		
			int i = scan.nextInt();
			int[][] graph = new int[i][i];
			double[] xCoordinates = new double[i];
			double[] xLP = new double[i];
		
			if (fillGraph(graph,xCoordinates,xLP,i)) {
				System.out.println("Is a terrain");
				getLP(graph,xCoordinates,xLP);
			}
		
			/*
			for (int j = 0; j < i; j++) {
				for (int x = 0; x < i; x++) {
					System.out.print(" "+graph[j][x]+" ");
				}
				System.out.println("");
			}
			 */
		
			scan.close();
		}catch(Exception e){}
	}
	
	@SuppressWarnings("resource")
	public static boolean fillGraph(int[][] graph, double[] xCoordinates, double[] xLP, int i) {
		Scanner scan = new Scanner(System.in);
		
		for(int x=0; x<i; x++){
			System.out.print("Enter x coordinate for X"+x+": ");
			double j = scan.nextDouble();
			xCoordinates[x] = j;
			xLP[x] = 1.0;
		}
		
		for(int j=0; j<i; j++){
			graph[j][j] = 1;				
		}
		
		while(true) {
			try {
				System.out.print("Enter visibility coordinates or F to finish: ");
				String s = scan.next();
				if(s.contains("F")) break;
				String coordinates = s.replaceAll("\\(|\\)","");
				//System.out.println(coordinates);
				String[] points = coordinates.split(",");
				int point1 = Integer.valueOf(points[0]);
				int point2 = Integer.valueOf(points[1]);
				//System.out.println(point1+" - "+point2);
				graph[point1][point2] = 1;
				
				if(breaksOrderClaim(graph)){
					System.out.println("Order Claim Bad");
					return false;
				} else if(chordlessCycle(graph)){
					System.out.println("Chordless Cycle");
					return false;
				} 
			} catch (NumberFormatException nfe) {
				System.out.println("Error has been found");
				return false;
			}
		}
		scan.close();	
		return true;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void getLP(int[][] graph, double[] xCoordinates, double[] xLP){
		LinearObjectiveFunction f = new LinearObjectiveFunction(xLP, 0);
		System.out.print("Minimize p =");
		for(int i=0; i<graph.length; i++){
			System.out.print(" y"+i+ (i==graph.length-1 ? " " : " +") );
		}
		System.out.println("");
		System.out.println("Subject to ");
		
		Collection constraints = new ArrayList();
		for(int a=0; a<graph.length; a++){
			//int xa = a;
			int lastSeen = a+1;
			for(int c=a+2; c<graph.length; c++){
				if(graph[a][c]==1){
					//a sees c, ensure that everything between a and c is below the ac line, likely overkill
					for(int b = a+1; b<c; b++){
						double[] array = new double[graph.length];
						array[a] = (xCoordinates[c] - xCoordinates[b]);
						array[b] = (xCoordinates[c] - xCoordinates[a]) * (-1);
						array[c] = (xCoordinates[b] - xCoordinates[a]);
						System.out.println((array[a])+"y"+a+" "+(array[b])+"y"+b+" + "+(array[c])+"y"+c+" >= 0");
						constraints.add(new LinearConstraint(array, Relationship.GEQ, 0));
					}										
					lastSeen = c;					
				} else{
					double[] array = new double[graph.length];
					array[a] = (xCoordinates[c] - xCoordinates[lastSeen]) * (-1);
					array[lastSeen] = (xCoordinates[c] - xCoordinates[a]);
					array[c] = (xCoordinates[lastSeen] - xCoordinates[a]) * (-1);
					//a doesn't see c, who is the designated blocker... it was the last one seen
					System.out.println((array[a])+"y"+a+" + "+(array[lastSeen])+"y"+lastSeen+" "+(array[c])+"y"+c+" >= 1");
					constraints.add(new LinearConstraint(array, Relationship.GEQ, 1));
				}
			}
		}
		
		for (int a=0; a<graph.length; a++) {
			double[] array = new double[graph.length];
			array[a] = 1;
			constraints.add(new LinearConstraint(array, Relationship.GEQ, 0));
		}
		
		RealPointValuePair solution = null;
		try {
			solution = new SimplexSolver().optimize(f, constraints, GoalType.MINIMIZE, false);
		} catch (OptimizationException e) {
			e.printStackTrace();
		}
		   
		if (solution != null) {
			//get solution
			double max = solution.getValue();
			System.out.println("Opt: " + max);
		   
			//print decision variables
			for (int a = 0; a < graph.length; a++) {
				System.out.print(solution.getPoint()[a] + "\t");
			}
		}
	}
	
	/* ALL STUFF BELOW IS FOR RANDOM COORDINATE STUFF */
	
	//simple method to check if 2 points (row,col) see each other
	public static boolean sees(int row, int col, double[] x, double[] y){
		double m = (y[col] - y[row]) / (x[col] - x[row]);				
		double b = y[col] - (m * x[col]);
		
		//loop through row and col and see if anything is blocking row from col
		//row is always less than col when this method is called (see isValid below)
		for(int i=row+1; i<col; i++){
			double lineY = x[i]*m + b;
			if(lineY < y[i]){
				return false;
			}
		}
		return true;		
	}
	
	//method to check if the coordinates are valid given the coordinates in x and y
	public static boolean isValid(int[][] graph, double[] x, double[] y){
		for(int r=0; r<graph.length; r++){
			for(int c=r+1; c<graph.length; c++){
				//row is always less than column
				if(graph[r][c]==1 && !sees(r, c, x, y)){
					return false;
				}
				if(graph[r][c]==0 && sees(r, c, x, y)){
					return false;
				}
				
			}
		}
		return true;
	}
	
	
	//try and randomly guess coordinates of the terrain
	public static void getCoordinates(int[][] graph){
		double[] x = new double[graph.length];
		double[] y = new double[graph.length];
		//set x coordinates to be integer values 0, 1, ..., n-1
		for(int i=0; i<graph.length; i++){
			x[i] = i;
		}
		//randomly put in y coordinates to see if the visibilities hold
		for(int k=0; k<100000000; k++){
			for(int p=0; p<graph.length; p++){
				y[p] = Math.random();
			}
			//x and y coordinates are filled, see if they are valid
			if(isValid(graph, x, y)){
				//print out values
				for(int i=0; i<graph.length; i++){
					//fout.print("("+x[i]+","+y[i]+") ");
				}
				return;
			}					
		}		
	}
	
	
	public static boolean isChordless(int[][] graph, ArrayList<Integer> path){
		int[] vals = new int[path.size()];
		for(int i=0; i<path.size(); i++){
			vals[i] = path.get(i);
		}
		
		//check if the cycle is chordless, check if first is connected to anything, special case
		for(int second = 2; second < vals.length-1; second++){
			if(graph[vals[0]][vals[second]]==1){
				//not chordless, return false
				return false;
			}
		}
		
		//check if rest are connected to anything past it
		for(int first = 1; first<vals.length; first++){
			for(int second = first+2; second<vals.length; second++){
				if(graph[vals[first]][vals[second]]==1){
					//not chordless, return false
					return false;
				}
			}
		}
		return true;
	}
	
	//this method looks for cycles, calls chordlessCycle if it finds one to see if it's chordless
	public static boolean foundOne(int[][] graph, int start, int current, ArrayList<Integer> path){
		if(start!=current && path.size()>=4 && graph[start][current]==1){
			//cycle of at least 4, see if it's chordless
			if(isChordless(graph, path)){
				return true;
			}
		}
		int row = current;
		for(int col=row+1; col<graph.length; col++){
			ArrayList<Integer> temp = new ArrayList<Integer>();
			for(Integer val : path){
				temp.add(val);
			}
			temp.add(col);
			if(graph[row][col]==1 && foundOne(graph, start, col, temp)){
				return true;
			}
		}
		return false;
	}
	
	public static boolean chordlessCycle(int[][] graph){
		//start at i and see if there is a cycle back to i
		for(int i=0; i<graph.length; i++){
			ArrayList<Integer> temp = new ArrayList<Integer>();
			temp.add(i);
			if(foundOne(graph, i, i, temp)){
				return true;
			}
		}
		return false;
	}
	
	//a sees c. check every b and d such that a < b < c < d, b sees d and a doesn't see d, then 
	//the order claim is broken.
	public static boolean orderClaimBroken(int[][] graph, int a, int c){
		for(int b=a+1; b<c; b++){
			//check every vertex between a and c
			for(int d=c+1; d<graph.length; d++){
				//check every d after c 
				if(graph[b][d]==1 && graph[a][d]==0){
					//breaks the order claim
					return true;
				}
			}
		}
		//made it through all of it, order claim not broken
		return false;
	}
	
	//find 2 vertices that see each other, call them row and col, assume row < col
	//then check for every x/y such that row < x < col < y if x sees y and row doesn't see y.
	public static boolean breaksOrderClaim(int[][] graph){
		for(int row=0; row<graph.length; row++){
			for(int col=row+2; col<graph.length; col++){
				if(graph[row][col]==1 && orderClaimBroken(graph, row, col)){
					return true;
				}
			}
		}
		return false;
	}

}
