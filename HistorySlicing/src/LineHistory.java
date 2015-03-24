import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
//import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
//import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
//import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
//import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.MyersDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;

import java.io.ByteArrayOutputStream;

import org.eclipse.jgit.diff.DiffFormatter;


//this class mainly demonstrate how does API work
public class LineHistory {
	
	private static Git myGit;
	//private static ArrayList<ObjectId> output;
	
	//this hash table is used to store the final results
	private static Hashtable<Integer, List<String>> resultTable;
	
	//this list is used to store the list of the changed commit ID for each line
	//private static List<List<String>> resultLists;
	
	//How many lines does the target file contain
	private static int size;
	
	//exception need to be catched later first 3 belongs to repo.resolve and GitAPIException belongs to List<DiffEntry> diffs
	public static void main(String[] args) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException, GitAPIException {
		// TODO Auto-generated method stub
		
		//Initializing Git working directory
		File gitWorkDir = new File(args[0]);
				
		//Initializing input file path
		String filePath = args[1];
				
		try {
			myGit = Git.open(gitWorkDir);
		}
		catch (IOException e) {
			System.out.println("Invalid directory");
			System.exit(0);
		}
				
		//Initializing the Repository object
		Repository localRepo = myGit.getRepository();
				
				
				
				
		//try to get the current commit Id and its previous one
		ObjectId newId = localRepo.resolve(Constants.HEAD);
		ObjectId oldId = localRepo.resolve(newId.name() + "^");
		//oldId = localRepo.resolve(oldId.name() + "^");
		
		//try to get the current file
		RawText newFile = getFile(localRepo, filePath, newId);
		int size = newFile.size();
				
		//Initializing the output table
		InitOutput(localRepo, filePath);
						
		//check if old revision exist at the beginning
		if (oldId == null) {
			//end here and update the result table
			//System.out.println("immmmmmmmmmmm");
			for (int i = 1; i <= size; i++) {
				String output = newId.abbreviate(8).name();
				List<String> outputList = new ArrayList<String> ();
				outputList.add(output);
				resultTable.put(i, outputList);
			}
			System.out.println("this file is newly created");
			printOutput();
			System.exit(0);
		}
					
		//try go get the previous revision
		RawText oldFile = getFile(localRepo, filePath, oldId);
		//check if the oldFile exists at the beginning
		if (oldFile == null) {
			//end here and update the result table
			//System.out.println("immmmmmmmmmmm");
			for (int i = 1; i <= size; i++) {
				String output = newId.abbreviate(8).name();
				List<String> outputList = new ArrayList<String> ();
				outputList.add(output);
				resultTable.put(i, outputList);
			}
			System.out.println("this file is newly created");
			printOutput();         
			System.exit(0);
		}
		
		process(localRepo, filePath);
								
		printOutput();	    
	}
	
	private static void InitOutput(Repository localRepo, String filePath) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {
		//initializing the current revision
		ObjectId Id = localRepo.resolve(Constants.HEAD);
		RawText targetFile = getFile(localRepo, filePath, Id);
		
		size = targetFile.size();
		
		resultTable = new Hashtable<Integer, List<String>> (size);
		//resultLists = new ArrayList<List<String>> (size);

		//add current commit Id to the result
		List<String> initList = new ArrayList<String> ();
		//initList.add(Id.abbreviate(8).name());
		
		//Initialize result table with line numbers and empty arrayLists
		for (int i = 1; i <= size; i++) {
			resultTable.put(i, initList);
			//resultLists.add(new ArrayList<String> ());
		}
	}
	
	private static void process(Repository localRepo, String filePath) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {
		
		//initializing the current revision
		ObjectId newId = localRepo.resolve(Constants.HEAD);
		
		
		RawText headFile = getFile(localRepo, filePath, newId);
		int headSize = headFile.size();
		
		//Initialize mapping list
		
		
		List<LinePair<Integer, Integer>> lineMappingList = new ArrayList<LinePair<Integer, Integer>>();/*****Provided to be useful******/
		for (int i = 0; i < headSize; i ++) {
			 lineMappingList.add(new LinePair<Integer, Integer>(i + 1, i + 1));
		}
		
		/***System.out.println("Latest Line Mapping: ");
				
		
		for(LinePair<Integer, Integer> a: lineMappingList) {
			System.out.println(a.getL() + "," + a.getR());
		}
		System.out.println("-----------");***/
		
		//count to determine the times of loop runs. count = 1 indicates that the file is a newly added file 
		//which does not have a history
		int count = 0;
		do {
				
			ObjectId oldId = localRepo.resolve(newId.name() + "^");
			
			//get target line numbers
			//if there is no previous revision, end the loop
			// try to get the new and old file
			RawText newFile = getFile(localRepo, filePath, newId);
			
			
			RawText oldFile = getFile(localRepo, filePath, oldId);
			
			if(oldFile == null) {
				//System.out.println("im inniinini");
			    ArrayList<LinePair<Integer,Integer>> allMatcher = new ArrayList<LinePair<Integer,Integer>>();

				allMatcher.addAll(lineMappingList);
				for (LinePair<Integer,Integer> Pair : lineMappingList){
					if (Pair.getL() == 0){
						allMatcher.remove(Pair);//remove recent added lines
					}
				}
				//System.out.println("allMatcher is " );
				for (LinePair<Integer, Integer> pair : allMatcher) {
					//System.out.println(pair.getL() + ", " + pair.getR());
					pair.setL(0);
				} 
				
				
				updateResultTable(allMatcher, oldId, newId, lineMappingList); 
				break;
			}

			

			//get matched lines
			//ArrayList<LinePair<Integer, Integer>> newLineMappingList = lineMatch(newFile, oldFile, count, oldId, lineMappingList);
			lineMappingList = lineMatch(newFile, oldFile, count, oldId, newId, lineMappingList);
			//update the mapping list

			//move to the previous revision
			newId = oldId;
			
			//increment count
			count ++;
			
		} while(true);
		
		
		if (count == 1) {
			//System.out.println("This is a new file");
			
		}
		
		//System.out.println("count is " + count);
		
	}
	
	//this method try to find a file in the repo for a given commit
	private static RawText getFile(Repository localRepo, String filePath, ObjectId commitId) throws MissingObjectException, IncorrectObjectTypeException, IOException, NullPointerException {
		
		RawText file = null;
		ObjectReader reader = localRepo.newObjectReader();
		
		try {
	  		// Get the commit object for that revision
	  	    RevWalk walk = new RevWalk(reader);
	  	    RevCommit commit = walk.parseCommit(commitId);

	  	        // Get the revision's file tree
	  	    RevTree tree = commit.getTree();
	  	        // .. and narrow it down to the single file's path
	  	    TreeWalk treewalk = TreeWalk.forPath(reader, filePath, tree);

	  	    if (treewalk != null) {
	  	    	// use the blob id to read the file's data
	  	        byte[] data = reader.open(treewalk.getObjectId(0)).getBytes();
	  	        //return new String(data, "utf-8");
	  	        file = new RawText(data);
	  	        //System.out.println(new String(data, "utf-8"));
	  	        
	  	        //newText = new RawText(data);
	  	    } else {
	  	            //System.out.println("File Not Found");
	  	    }
	  	} catch (NullPointerException e){
	  		return null;
	  	}
	  	finally {
	  		reader.release();
	  	}
		
		return file;
		
	}

	private static ArrayList<LinePair<Integer, Integer>> lineMatch(RawText newFile, RawText oldFile , int count, ObjectId oldId, ObjectId newId, List<LinePair<Integer, Integer>> lineMappingList) throws IOException {
		List<Integer> changedLines = new ArrayList<Integer> ();
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		
	    //System.out.println("ALL DIFF OUTPUT " + count);
	    //System.out.println("-----------");
		
	   	int newFileLineNumber = newFile.size();
	   	int oldFileLineNumber = oldFile.size();
	   	//Find which file has the maximum line number for the following loop

	    ArrayList<LinePair<Integer,Integer>> matcher = new ArrayList<LinePair<Integer,Integer>>();
	    ArrayList<LinePair<Integer,Integer>> changedMatcher = new ArrayList<LinePair<Integer,Integer>>();
	    ArrayList<Integer> targetLines = new ArrayList<Integer>();
	    
	    //initial the array list with paired numbers/ check array list elements
	    List<Integer> beginA = new ArrayList<Integer>();
	    List<Integer> beginB = new ArrayList<Integer>();
	    List<Integer> endA = new ArrayList<Integer>();
	    List<Integer> endB = new ArrayList<Integer>();
	    
	    //int beginA[] = new int[200],beginB[] = new int[200];
	    //int endA[] = new int[200],endB[] = new int[200];
	    
	    try {    	
		     EditList diffList = new EditList();
		     diffList.addAll(MyersDiff.INSTANCE.diff(RawTextComparator.DEFAULT,oldFile,newFile));
		     new DiffFormatter(out).format(diffList, oldFile, newFile);
		     //System.out.println(out);
		     int i = 0;
			 for ( ; i < diffList.size(); i++) {
				 Edit edit = diffList.get(i);
				 
				 beginA.add(edit.getBeginA()); 
				 endA.add(edit.getEndA());
				 beginB.add(edit.getBeginB());
				 endB.add(edit.getEndB());
				 
				 
				 
				 //System.out.println("BeginA: "+beginA[i]+" EndA: "+endA[i]+" BeginB: "+beginB[i]+" EndB: "+endB[i]);			  
			 }
			 
			 //System.out.println(lineA.toString()+lineB.toString()+lenA.toString()+lenB.toString());
			 ArrayList<Integer> oldList = new ArrayList<Integer> ();
			 ArrayList<Integer> newList = new ArrayList<Integer> ();
			 List<List<LinePair<Integer, String>>> oldOnlyLists = new ArrayList<List<LinePair<Integer, String>>> ();
			 List<List<LinePair<Integer, String>>> newOnlyLists = new ArrayList<List<LinePair<Integer, String>>> ();
			 
			 
			 for(int Left = 1; Left <= oldFileLineNumber; Left++ ){
				 oldList.add(Left);
			 }		 
			 
			 for(int Right = 1; Right <= newFileLineNumber; Right++ ){
				 newList.add(Right);
			 }	
			 
			 for (int j = 0; j< beginA.size() ; j++){
				 List<LinePair<Integer, String>> changedTempListL = new ArrayList<LinePair<Integer, String>>();
				 List<LinePair<Integer, String>> changedTempListR = new ArrayList<LinePair<Integer, String>>();
				 List<Integer> unchangedTempListL = new ArrayList<Integer>();
				 List<Integer> unchangedTempListR = new ArrayList<Integer>();

				 for (int Left = beginA.get(j)+1; Left<=endA.get(j); Left++){
					 //System.out.println("im in-------------------------");
					 changedTempListL.add(new LinePair<Integer, String> (Left, oldFile.getString(Left - 1)));
					 unchangedTempListL.add(Left);
				 }
				 
				 for (int Right = beginB.get(j)+1; Right<=endB.get(j); Right++){
					 changedTempListR.add(new LinePair<Integer, String> (Right, newFile.getString(Right - 1)));
					 unchangedTempListR.add(Right);
					 changedLines.add(Right);
				 }
				 
				 newList.removeAll(unchangedTempListR);
			 	 oldList.removeAll(unchangedTempListL);
			 	 
			 	 oldOnlyLists.add(changedTempListL);
				 newOnlyLists.add(changedTempListR);	 
				 
			 }		 	 
			 
		
			// System.out.println("newOnly list is ");
			 //printLists(newOnlyLists);
			 //System.out.println("oldOnly list is ");
			 //printLists(oldOnlyLists);

			 //line mapping for unchanged lines
			 matcher = lineMappingForUnchangedLines(oldList, newList);
			 //unchangedMatcher = matcher;

			 // Unchanged list confirmed, 2 change only
			 //line mapping for changed lines in hunks
					
			 changedMatcher = lineMappingForChangedLines(oldOnlyLists, newOnlyLists, oldFileLineNumber, newFileLineNumber);
			 
			 
			 /***System.out.println("Changed Matcher: ");
				for (LinePair<Integer,Integer> pair : changedMatcher) {
					System.out.println(pair.getL() + "," + pair.getR());
				}
					System.out.println("--------------");***/
			 //System.out.println("size is changedMatcher is" + changedMatcher.size());
			 
			 //Combine the two matcher
			 matcher.addAll(changedMatcher);/****!!!*/
			 
			 
			 
			 
			 //System.out.println("Final Matcher: ");
			 //printMatcher(matcher);
			 //remove unneeded lines
			 
			 
		     } catch (IOException e) {
		    	 e.printStackTrace();
		     }

			     for (int i = 0; i < lineMappingList.size(); i++) {
					LinePair<Integer, Integer> oldPair = lineMappingList.get(i);
					int targetLine = oldPair.getL();
					//System.out.println(oldPair.getL()+","+oldPair.getR());
					boolean isValid = false;
					if(targetLine != 0){
					for (LinePair<Integer, Integer> newPair: matcher) {
					
						//If find a match modify it and stop the loop
				
						
						if(newPair.getL()!=0){
								if(newPair.getR() == targetLine) {
									oldPair.setL(newPair.getL());
									isValid = true;
									break;
								}
							}
							if(newPair.getL()==0){
								if(newPair.getR() == targetLine) {
									oldPair.setL(newPair.getL());
									newPair.setR(oldPair.getR());
									isValid = true;
									break;
								}
							}
						}
					}
					
					//If there is no match remove it
					if(!isValid) {
						lineMappingList.remove(oldPair);
						i--;
						//add it to the targetLines
						}
			     	}
			     
			


	    //update the resultLists
	    updateResultTable(changedMatcher, oldId, newId, lineMappingList); 

	   	    
	    return (ArrayList<LinePair<Integer, Integer>>) lineMappingList;
	}
	
	private static ArrayList<LinePair<Integer,Integer>> lineMappingForUnchangedLines(ArrayList<Integer> oldList, ArrayList<Integer> newList) {
		ArrayList<LinePair<Integer,Integer>> matcher = new ArrayList<LinePair<Integer,Integer>>();

		int Left,Right;
		 for(int index = 0; index < oldList.size() ; index++){
			 //Left = (int) oldList.toArray()[index];
			 //Right = (int) newList.toArray()[index];
			 Left = oldList.get(index);
			 Right = newList.get(index);
		     LinePair<Integer,Integer> lp = new LinePair<Integer,Integer>(Left,Right);
		     matcher.add(lp);
		 }
		
		return matcher;
	}
	
	private static ArrayList<LinePair<Integer,Integer>> lineMappingForChangedLines(List<List<LinePair<Integer, String>>> oldOnlyLists, 
																				   List<List<LinePair<Integer, String>>> newOnlyLists,
																				   int file_LengthL, int file_LengthR) {
ArrayList<LinePair<Integer,Integer>> matcher = new ArrayList<LinePair<Integer,Integer>>();
		
		//loop for traversing hunks
		for(int i = 0; i < oldOnlyLists.size(); i++) {
			List<LinePair<Integer, String>> newList = newOnlyLists.get(i);
			List<LinePair<Integer, String>> oldList = oldOnlyLists.get(i);
			
			if(newList.size() == 0) {
				break;
			}
			
			//check large modification here
			if( isLargeModification(oldList.size(), file_LengthL, newList.size(), file_LengthR)) {
				for (LinePair<Integer, String> Line : newList) {
					LinePair<Integer, Integer> addedLine = new LinePair<Integer, Integer> (0 ,Line.getL());
					matcher.add(addedLine);
				}
			} else {
				//Use HungrarianMethod to find the optimise matching
				//construct costMatrix
				double[][] costMatrix = new double [newList.size()][oldList.size()];
				
				for(int row = 0; row < newList.size(); row++) {
					LinePair<Integer, String> newLine = newList.get(row);
					String newString = newLine.getR();
					
					for (int col = 0; col < oldList.size(); col++) {
						LinePair<Integer,String> oldLine = oldList.get(col);
						String oldString = oldLine.getR();
						double distance = calculateNormalizedDistance(oldString, newString);
						costMatrix[row][col] = distance;
					}
				}
				//Create  a Hungraianmethod Object
				HungrarianMethod Hung = new HungrarianMethod(costMatrix);
				int[] assignmentResult = Hung.execute();
				
				//construct new newList and oldList
				List<LinePair<Integer, String>> newNewList = new ArrayList<LinePair<Integer, String>>();
				List<LinePair<Integer, String>> newOldList = new ArrayList<LinePair<Integer, String>>();
				List<LinePair<Integer, String>> tempList = new ArrayList<LinePair<Integer, String>>();
				for (LinePair<Integer, String> line : newList) {
					tempList.add(line);
				}
				
				for (int a = 0; a < assignmentResult.length; a++) {
					if(assignmentResult[a] != -1) {
						newNewList.add(newList.get(a));
						newOldList.add(oldList.get(assignmentResult[a]));
						tempList.remove(newList.get(a));
					}
				}
				
				//what remain in oldList is the line that didnt find a match
				//so add it to update the result table
				for (LinePair<Integer, String> line: tempList) {
					LinePair<Integer, Integer> addedLine = new LinePair<Integer, Integer> (0 ,line.getL());
					matcher.add(addedLine);
				}
				
				
				
				
				for(int a = 0; a < newNewList.size(); a++) {
					LinePair<Integer, String> newLine = newNewList.get(a);
					String newString = newLine.getR();
					LinePair<Integer, String> oldLine = newOldList.get(a);
					String oldString = oldLine.getR();					

					//check if this is a small change
					double distance = calculateNormalizedDistance(oldString, newString);
					
					if(distance < 0.4) {
						LinePair<Integer, Integer> matchedLine = new LinePair<Integer, Integer> (oldLine.getL(), newLine.getL());
						matcher.add(matchedLine);
					} else {
						//System.out.println("im innnnnnnnnnnnnnnnnnnnnnnnnnnnnnnn");
						LinePair<Integer, Integer> addedLine = new LinePair<Integer, Integer> (0 ,newLine.getL());
						matcher.add(addedLine);
					}
					
				}
				
				/*
				int m = 0;
				//loop for traversing each line in new hunk
				for (int j = 0; j < newList.size(); j++) {
					LinePair<Integer, String> newLine = newList.get(j);
					String newString = newLine.getR();
					boolean smallChanges = false;
					//calculate the Leven distance for each line in old hunk
					for (int n = m; n < oldList.size(); n++) {
						//need to compute minimum!!!
						LinePair<Integer,String> oldLine = oldList.get(n);
						String oldString = oldLine.getR();
						double distance = calculateNormalizedDistance(oldString, newString);
						//System.out.println("distance is " + distance);
						
						if(distance < 0.4) {
							LinePair<Integer, Integer> matchedLine = new LinePair<Integer, Integer> (oldLine.getL(), newLine.getL());
							matcher.add(matchedLine);
							m = n + 1;
						    smallChanges = true;
							break;
						}

					}
					
					
					if (smallChanges == false){
						LinePair<Integer, Integer> addedLine = new LinePair<Integer, Integer> (0 ,newLine.getL());
						matcher.add(addedLine);
					}
					
				}
				*/
			}//end else
			
		}
		
		return matcher;	
	}
	
	private static boolean isLargeModification(int lengthL, int file_lengthL, int lengthR, int file_lengthR) {
		boolean isLargeModification = false;
		if(lengthR != 0) {

	    	
	    	double alpha = 0.5;
	    	int beta = 4;
	    	int gamma = 4;
	    	//boolean regionLength = false;
	    	boolean regionLength = (lengthL > Math.max(alpha * file_lengthL, beta) || 
	    							lengthR > Math.max(alpha * file_lengthR, beta));

	    	boolean ratioOfLength = (lengthL/lengthR < 1/gamma || gamma < lengthL/lengthR);
	    
	    	isLargeModification = regionLength || ratioOfLength;
		}
		
		return isLargeModification;
	}
	
	private static double calculateNormalizedDistance (String s1, String s2) {
		int max = Math.min(s1.length(), s2.length());
		return (double) getDistance(s1, s2)/max;
	}
	
	private static int getDistance(String s1, String s2) {
   
        // check preconditions
        int m = s1.length();
        int n = s2.length();
        if (m == 0) {
            //return n; // some simple heuristics
        } else if (n == 0) {
            //return m; // some simple heuristics
        } else if (m > n) {
            String tempString = s1; // swap m with n to get O(min(m, n)) space
            s1 = s2;
            s2 = tempString;
            int tempInt = m;
            m = n;
            n = tempInt;
        }
        
        // normalize case
        s1 = s1.toUpperCase();
        s2 = s2.toUpperCase();

        
        // Instead of a 2d array of space O(m*n) such as int d[][] = new int[m +
        // 1][n + 1], only the previous row and current row need to be stored at
        // any one time in prevD[] and currD[]. This reduces the space
        // complexity to O(min(m, n)).
        int prevD[] = new int[n + 1];
        int currD[] = new int[n + 1];
        int temp[]; // temporary pointer for swapping

        
        // the distance of any second string to an empty first string
        for (int j = 0; j < n + 1; j++) {
            prevD[j] = j;
        }

        
        // for each row in the distance matrix
        for (int i = 0; i < m; i++) {

            
            // the distance of any first string to an empty second string
            currD[0] = i + 1;
            char ch1 = s1.charAt(i);

            
            // for each column in the distance matrix
            for (int j = 1; j <= n; j++) {

                
                char ch2 = s2.charAt(j - 1);
                if (ch1 == ch2) {
                    currD[j] = prevD[j - 1];
                } else {
                    currD[j] = minOfThreeNumbers(prevD[j] + 1,
                                                 currD[j - 1] + 1, prevD[j - 1] + 1);
                }

                
            }

            
            temp = prevD;
            prevD = currD;
            currD = temp;

            
        }

        
        // after swapping, the final answer is now in the last column of prevD

        return prevD[prevD.length - 1];
	}
	
	private static int minOfThreeNumbers(int num1, int num2, int num3) {
	        return Math.min(num1, Math.min(num2, num3));
	}
	
	private static void updateResultTable(ArrayList<LinePair<Integer, Integer>> changedMatcher, ObjectId oldId, ObjectId newId, List<LinePair<Integer, Integer>> lineMappingList) {
		//add oldId for changed lines
		//System.out.println("changedMatcher is ");
		for (LinePair<Integer, Integer> pair : changedMatcher) {
			//System.out.println(pair.getL() + ", " + pair.getR());
			int targetLine = pair.getL();
			
			if (targetLine != 0){
				for (LinePair<Integer, Integer> validPair : lineMappingList) {
					//if exist update and end
					if(validPair.getL() == targetLine ) {
						//System.out.println("New Version Added: "+ targetLine+","+validPair.getR()+" "+newId.abbreviate(9).name());
						List<String> SHA = new ArrayList<String>(resultTable.get(validPair.getR()));
						SHA.add(newId.abbreviate(9).name()+"(Line:"+ validPair.getL()+")");
						resultTable.put(validPair.getR(),SHA);
						break;
					}
				}
			}else if(targetLine == 0 ){
				for (LinePair<Integer, Integer> validPair : lineMappingList) {
					//if exist update and end
					if(validPair.getL() == targetLine && validPair.getR() == pair.getR() ) {
						//System.out.println("New Version Added: "+ targetLine+","+validPair.getR()+" "+newId.abbreviate(9).name());
						List<String> SHA = new ArrayList<String>(resultTable.get(validPair.getR()));
						SHA.add(newId.abbreviate(9).name()+"(Line added)");
						resultTable.put(validPair.getR(),SHA);
						break;
					}
				}
			}
		}	
	}
	

	
	private static void printOutput() {
		System.out.println("------------------------");
		System.out.println("Final output is: ");
		for (int i = 1; i <= size; i++) {
			List<String> tempList = resultTable.get(i);
			int l = tempList.size();
			String commitList = "";
			
			for (int j = 0; j < l; j++) {
				commitList = commitList + " " + tempList.get(j);
			}
			
			System.out.println("Line " + i + ": " + commitList);
		}
	}

}
















