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
	private static ArrayList<ObjectId> output;
	private static Hashtable<String, Integer> matchTable;
	
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
		
		diff(localRepo, filePath);
	    
	}
	
	private static void diff(Repository localRepo, String filePath) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {
		
		ObjectReader reader = localRepo.newObjectReader();
		
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
			System.out.println("this is new file: ");
			RawText newFile = getFile(localRepo, filePath, newId);
			System.out.println("-----------");
			System.out.println("this is old file: ");
			RawText oldFile = getFile(localRepo, filePath, oldId);
			System.out.println("---------");
			
			lineMatch(newFile,oldFile,count);
	
			
			//move the the previous revision
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
	  	        System.out.println(new String(data, "utf-8"));
	  	        
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

	private static void lineMatch(RawText newFile, RawText oldFile , int count){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
	    
	    System.out.println("ALL DIFF OUTPUT " + count);
	    System.out.println("-----------");
	    
	   
	   	int oldFileLineNumber = oldFile.size();
	   	int newFileLineNumber = newFile.size();
	   	int minLineN = oldFileLineNumber < newFileLineNumber ? oldFileLineNumber:newFileLineNumber;
	   	//Find which file has the maximum line number for the follwoing loop
	   	System.out.println(oldFileLineNumber+","+newFileLineNumber+","+ minLineN);
	    ArrayList<LinePair<Integer,Integer>> matcher = new ArrayList<LinePair<Integer,Integer>>();
	    for (int Line = 1;  Line <= minLineN;Line++ ){
	    	LinePair<Integer,Integer> lp = new LinePair<Integer,Integer>(Line,Line);
	    	matcher.add(lp);
	    	System.out.println(lp.getL() + "," + lp.getR() ); 
	    } 
	    //initial the array list with paired numbers/ check array list elements
	    
	    //System.out.println(matcher.get(maxLineN-1).getL());
	    
	    
	    if (oldFile != null) {
	    	int lineA[] = new int[200],lineB[] = new int[200];
	    	int lenA[] = new int[200],lenB[] = new int[200];
	    	try
		    {    	
	    	  int i = 0;
		      EditList diffList = new EditList();
		      diffList.addAll(MyersDiff.INSTANCE.diff(RawTextComparator.DEFAULT,oldFile,newFile));
		      new DiffFormatter(out).format(diffList, oldFile, newFile);
			  for (Edit edit : diffList) {
				  lineA[i] = edit.getBeginA(); 
				  lenA[i] = edit.getLengthA();
				  lineB[i] = edit.getBeginB();
				  lenB[i] = edit.getLengthB();
				  System.out.println("BeginA: "+lineA[i]+" lengthA: "+lenA[i]+" BeginB: "+lineB[i]+" lengthB: "+lenB[i]);

				  i++;
				  
			  }
			 //System.out.println(lineA.toString()+lineB.toString()+lenA.toString()+lenB.toString());
			  int Left,Right,index = 0;
			  for(int j = 0;j<i;j++){
				  /*if(lenA[j] == 0 && lenB[j] > 0) {
					  
				  }*/
				  if(lenA[j] > 0 && lenB[j] == 0){
					  Right = lineB[j]+1;
					  Left = lineA[j]+lenA[j]+1;
					  index = lineB[j];
					  for (int n=0; n<(lenB[j+1]-lenB[j]); n++){
						  matcher.get(index).setL(Left);
						  matcher.get(index).setR(Right);
						  Left++;
						  Right++;
						  index++;
					  }
				  }
				  if(lenA[j] > 0 && lenB[j] > 0){
					  Right = lineB[j]+1;
					  Left = 0;
					  index = lineB[j];
					  for (int n=0; n< lenB[j];n++){
						  matcher.get(index).setL(Left);
						  matcher.get(index).setR(Right);
						  Right++;
						  index++;
					  }
				  }
			  }
			    for (int Line = 0;  Line < minLineN;Line++ ){
			    	System.out.println(matcher.get(Line).getL() + "," + matcher.get(Line).getR()); 
			    } 
			  
			    
		    } catch (IOException e)
		    {
		      e.printStackTrace();
		    }
		    System.out.println(out.toString());
		    System.out.println("-----------");
		    out.reset();	
	    }
	    
		
	}

}
