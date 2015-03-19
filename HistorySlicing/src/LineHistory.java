import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.HistogramDiff;
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
	private static List<List<String>> resultLists;
	
	//How many lines does the target file contain
	private static int size;
	
	//exception need to be catched later first 3 belongs to repo.resolve and GitAPIException belongs to List<DiffEntry> diffs
	public static void main(String[] args) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException, GitAPIException {
		// TODO Auto-generated method stub
		
		//Initializing Git working directory
		File gitWorkDir = new File(args[0]);
		
		//Initializing input file path
		String filePath = args[1];
		
		try
	    {
			myGit = Git.open(gitWorkDir);
	    }
	    catch (IOException e)
	    {
	    	System.out.println("Invalid directory");
	    	System.exit(0);
	    }
		
		//Initializing the Repository object
		Repository localRepo = myGit.getRepository();
		
		//Initializing the output table
		InitOutput(localRepo, filePath);
		
		diff(localRepo, filePath);
		
		updateResultTable();
		
		printOutput();
	    
	}
	
	private static void InitOutput(Repository localRepo, String filePath) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {
		//initializing the current revision
		ObjectId Id = localRepo.resolve(Constants.HEAD);
		RawText targetFile = getFile(localRepo, filePath, Id);
		
		size = targetFile.size();
		
		resultTable = new Hashtable<Integer, List<String>> (size);
		resultLists = new ArrayList<List<String>> (size);

		List<String> initList = new ArrayList<String> ();
		
		//Initialize result table with line numbers and empty arrayLists
		for (int i = 1; i <= size; i++) {
			resultTable.put(i, initList);
			resultLists.add(new ArrayList<String> ());
		}
	}
	
	private static void diff(Repository localRepo, String filePath) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {
		
		//initializing the current revision
		ObjectId newId = localRepo.resolve(Constants.HEAD);
		
		//boolean value used to decide if there exists a previous revision, if not end the loop
		boolean isEnd = false;
		
		//count to determine the times of loop runs. count = 1 indicates that the file is a newly added file 
		//which does not have a history
		int count = 0;
		do {
			ObjectId oldId = localRepo.resolve(newId.name() + "^");
			
			//if there is no previous revision, end the loop
			if (oldId == null) {
				break;
			}
			
			// try to get the new and old file
			//System.out.println("this is new file: ");
			RawText newFile = getFile(localRepo, filePath, newId);
			//System.out.println("-----------");
			//System.out.println("this is old file: ");
			RawText oldFile = getFile(localRepo, filePath, oldId);
			//System.out.println("---------");
			
			//update the line mapping result and the reusltLists
			lineMatch(newFile, oldFile, count, oldId);

	
			
			//move to the previous revision
			newId = oldId;
			
			//increment count
			count ++;
		} while(!isEnd);
		
		
		if (count == 1) {
			System.out.println("This is a new file");
			
		}
		
	}
	
	//this method try to find a file in the repo for a given commit
	private static RawText getFile(Repository localRepo, String filePath, ObjectId commitId) throws MissingObjectException, IncorrectObjectTypeException, IOException {
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
	  	        
	  	        //Initializing output list, set the output size to the line number of input file
	  	        //output = new ArrayList<ObjectId> (file.size());
	  	        
	  	        //newText = new RawText(data);
	  	    } else {
	  	            System.out.println("File Not Found");
	  	    }
	  	} 
	  	//catch ()
	  	finally {
	  		reader.release();
	  	}
		
		return file;
		
	}

	private static void lineMatch(RawText newFile, RawText oldFile , int count, ObjectId commitId) throws IOException {
		List<Integer> changedLines = new ArrayList<Integer> ();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
	    
	    //System.out.println("ALL DIFF OUTPUT " + count);
	    //System.out.println("-----------");
	    
	   
	   	int newFileLineNumber = newFile.size();
	   	int oldFileLineNumber = oldFile.size();
	   	//Find which file has the maximum line number for the following loop
	   	//System.out.println("New Line Numbers: "+newFileLineNumber);
	   	//System.out.println("Old Line Numbers: "+oldFileLineNumber);
	    ArrayList<LinePair<Integer,Integer>> matcher = new ArrayList<LinePair<Integer,Integer>>();
	    /**for (int Line = 1;  Line <= newFileLineNumber;Line++ ){
	    	LinePair<Integer,Integer> lp = new LinePair<Integer,Integer>(Line,Line);
	    	matcher.add(lp);
	    	} */
	    //initial the array list with paired numbers/ check array list elements
	    
	    //System.out.println(matcher.get(maxLineN-1).getL());
	    
	    
	    if (oldFile != null) {
	    	int beginA[] = new int[200],beginB[] = new int[200];
	    	int endA[] = new int[200],endB[] = new int[200];
	    	try
		    {    	
	    	  int i = 0;
		      EditList diffList = new EditList();
		      diffList.addAll(MyersDiff.INSTANCE.diff(RawTextComparator.DEFAULT,oldFile,newFile));
		      new DiffFormatter(out).format(diffList, oldFile, newFile);
			  for (Edit edit : diffList) {
				  beginA[i] = edit.getBeginA(); 
				  endA[i] = edit.getEndA();
				  beginB[i] = edit.getBeginB();
				  endB[i] = edit.getEndB();
				  //System.out.println("BeginA: "+beginA[i]+" EndA: "+endA[i]+" BeginB: "+beginB[i]+" EndB: "+endB[i]);
				  i++;			  
			  }
			 //System.out.println(lineA.toString()+lineB.toString()+lenA.toString()+lenB.toString());
			 ArrayList<Integer> oldList = new ArrayList<Integer> ();
			 ArrayList<Integer> newList = new ArrayList<Integer> ();
			 List<Hashtable<Integer, String>> oldOnlyLists = new ArrayList<Hashtable<Integer, String>> ();
			 List<Hashtable<Integer, String>> newOnlyLists = new ArrayList<Hashtable<Integer, String>> ();
			 
			 for(int Left = 1; Left <= oldFileLineNumber; Left++ ){
				 oldList.add(Left);
			 }		 
			 
			 for(int Right = 1; Right <= newFileLineNumber; Right++ ){
				 newList.add(Right);
			 }	
			 
			 for (int j = 0; j< i ; j++){
				 Hashtable<Integer, String> tempTableL = new Hashtable<Integer, String>();
				 Hashtable<Integer, String> tempTableR = new Hashtable<Integer, String> ();
				 List<Integer> tempListL = new ArrayList<Integer>();
				 List<Integer> tempListR = new ArrayList<Integer>();

				 for (int Left = beginA[j]+1; Left<=endA[j]; Left++){
					 tempTableL.put(Left, oldFile.getString(Left - 1));
					 tempListL.add(Left);
				 }
				 
				 for (int Right = beginB[j]+1; Right<=endB[j]; Right++){
					 tempTableR.put(Right, newFile.getString(Right - 1));
					 tempListR.add(Right);
					 changedLines.add(Right);
				 }
				 
				 //check that if the hunk is a large modification
				 if(isLargeModification(tempTableL.size(), oldFileLineNumber, tempTableR.size(), newFileLineNumber)) {
					 oldOnlyLists.add(tempTableL);
				 	 oldList.removeAll(tempListL);
				 	 newOnlyLists.add(tempTableR);
				 	 newList.removeAll(tempListR);
				 }
			 }		 	 

			 //line mapping for unchanged lines
			 int Left,Right;
			 for(int index = 0; index < oldList.size() ; index++){
				 Left = (int) oldList.toArray()[index];
				 Right = (int) newList.toArray()[index];
				 //System.out.println(Left+","+Right);
			     LinePair<Integer,Integer> lp = new LinePair<Integer,Integer>(Left,Right);
			     matcher.add(lp);
			 }
			 
			 //line mapping for changed lines in hunks
			 //write something here!!
			 lineMappingFOrChangedLines(oldOnlyLists, newOnlyLists);
			  
			    
		     } catch (IOException e) {
		    	 e.printStackTrace();
		     }
		    out.reset();	
	    }	    
		//System.out.println("size of changedLines is " + changedLines.size());

	    //update the resultLists
	    for (int c = 0; c < changedLines.size(); c++) {
	    	(resultLists.get(changedLines.get(c) - 1)).add(commitId.name());
	    }
	}
	
	private static void lineMappingFOrChangedLines(List<Hashtable<Integer, String>> oldOnlyLists, List<Hashtable<Integer, String>> newOnlyLists) {
		
		
	}
	
	private static boolean isLargeModification(int lengthL, int file_lengthL, int lengthR, int file_lengthR) {
		boolean isLargeModification = false;
		
		return isLargeModification;
	}
	
	private static void updateResultTable() {
		for (int i = 1; i <= size; i++) {
			List<String> tempList = resultLists.get(i - 1);
			resultTable.put(i, tempList);
		}
		
	}
	
	private static void printOutput() {
		
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
  /**
		  
**/

