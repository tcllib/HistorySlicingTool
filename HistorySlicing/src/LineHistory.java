import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
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
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;

import java.io.ByteArrayOutputStream;

import org.eclipse.jgit.diff.DiffFormatter;

//this class mainly demonstrate how does API work
public class LineHistory {
	
	private static Git myGit;
	private static ArrayList<ObjectId> output;
	
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
		//Commit head = repo.mapCommit(Constants.HEAD);
		
		//files to be compared
		RawText newFile = null;
		RawText oldFile = null;
		
		//boolean value used to decide if there exists a previous revision, if not end the loop
		boolean isEnd = false;
		
		//count to determine the times of loop runs. count = 1 indicates that the file is a newly added file 
		//which does not have a history
		int count = 0;
		do {
			ObjectId oldId = localRepo.resolve(newId.name() + "^");
			
			// try to get the new file
			try {
		  		// Get the commit object for that revision
		  	    RevWalk walk = new RevWalk(reader);
		  	    RevCommit commit = walk.parseCommit(newId);

		  	        // Get the revision's file tree
		  	    RevTree tree = commit.getTree();
		  	        // .. and narrow it down to the single file's path
		  	    TreeWalk treewalk = TreeWalk.forPath(reader, filePath, tree);

		  	    if (treewalk != null) {
		  	    	// use the blob id to read the file's data
		  	        byte[] data = reader.open(treewalk.getObjectId(0)).getBytes();
		  	        //return new String(data, "utf-8");
		  	        newFile = new RawText(data);
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
			
			//try to get the old file
			try {
				// Get the commit object for that revision
		  	    RevWalk walk = new RevWalk(reader);
		  	    if(oldId != null) {
		  	    	RevCommit commit = walk.parseCommit(oldId);

		  	        // Get the revision's file tree
		  	    	RevTree tree = commit.getTree();
		  	        // .. and narrow it down to the single file's path
		  	    	TreeWalk treewalk = TreeWalk.forPath(reader, filePath, tree);

		  	    	if (treewalk != null) {
		  	    		// use the blob id to read the file's data
		  	    		byte[] data = reader.open(treewalk.getObjectId(0)).getBytes();
		  	    		//return new String(data, "utf-8");
		  	    		oldFile = new RawText(data);
		  	    		//System.out.println(new String(data, "utf-8"));
		  	    		//newText = new RawText(data);
		  	    	} else {
		  	            System.out.println("loop ended");
		  	            isEnd = true;
		  	    	}
		  	    } else {
		  	    	break;
		  	    }
		  	    
				
			}
			
			//run diff for two files
			finally {
				reader.release();
			}
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
		    
		    System.out.println("ALL DIFF OUTPUT " + count);
		    System.out.println("-----------");
		    
		    if (oldFile != null) {
		    	try
			    {
			      EditList diffList = new EditList();
			      diffList.addAll(new HistogramDiff().diff(RawTextComparator.DEFAULT, oldFile, newFile));
			      new DiffFormatter(out).format(diffList, oldFile, newFile);
			    } catch (IOException e)
			    {
			      e.printStackTrace();
			    }
			    System.out.println(out.toString());
			    System.out.println("-----------");
			    out.reset();	
		    }
		    
			
			//move the the previous revision
			newId = oldId;
			
			//increment count
			count ++;
		} while(!isEnd);
		
		
		if (count == 1) {
			System.out.println("This is a new file");
			
		}
		
	}
	


}