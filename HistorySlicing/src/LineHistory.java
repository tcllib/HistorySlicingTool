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
import org.eclipse.jgit.diff.RawText;

import java.io.ByteArrayOutputStream;

import org.eclipse.jgit.diff.DiffFormatter;

//this class mainly demonstrate how does API work
public class LineHistory {
	
	private static Git myGit;
	private static ArrayList<ObjectId> output;
	
	//exception need to be catched later first 3 belongs to repo.resolve and GitAPIException belongs to List<DiffEntry> diffs
	public static void main(String[] args) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException, GitAPIException {
		// TODO Auto-generated method stub
		//Initializing Git API
		File gitWorkDir = new File(args[0]);
		
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
		
		
		
	    
		
		ObjectId lastCommitId = localRepo.resolve(Constants.HEAD);
	    ObjectId newId = localRepo.resolve(lastCommitId.name() + "^{tree}");
	    ObjectId oldId = localRepo.resolve(lastCommitId.name() + "^^{tree}");
	    
	    //try to find the input file and get the total line number
	  	ObjectReader reader = localRepo.newObjectReader();
	  	final ObjectId inputId = localRepo.resolve(Constants.HEAD);
	  	try {
	  		// Get the commit object for that revision
	  	    RevWalk walk = new RevWalk(reader);
	  	    RevCommit commit = walk.parseCommit(inputId);

	  	        // Get the revision's file tree
	  	    RevTree tree = commit.getTree();
	  	        // .. and narrow it down to the single file's path
	  	    TreeWalk treewalk = TreeWalk.forPath(reader, args[1], tree);

	  	    if (treewalk != null) {
	  	    	// use the blob id to read the file's data
	  	        byte[] data = reader.open(treewalk.getObjectId(0)).getBytes();
	  	        //return new String(data, "utf-8");
	  	        RawText file = new RawText(data);
	  	        System.out.println(new String(data, "utf-8"));
	  	        
	  	        //Initializing output list, set the output size to the line number of input file
	  	        output = new ArrayList<ObjectId> (file.size());
	  	        
	  	        //newText = new RawText(data);
	  	    } else {
	  	            System.out.println("nothing");
	  	    }
	  	} 
	  	//catch ()
	  	finally {
	  		reader.release();
	  	}
		
	    CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
	    oldTreeIter.reset(reader, oldId);
	    CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
	    newTreeIter.reset(reader, newId);
	    
		List<DiffEntry> diffs= myGit.diff()
	            .setNewTree(newTreeIter)
	            .setOldTree(oldTreeIter)
	            .call();
	    
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    DiffFormatter df = new DiffFormatter(out);
	    df.setRepository(localRepo);
	    
	    for(DiffEntry diff : diffs)
	    {
	    	System.out.println(diff.getNewPath());
	    	if(diff.getNewPath().equals(args[1])) {
	    		//check if file is newly added
	    		if(diff.getChangeType().toString().equals("ADD")) {
	    			System.out.println("this is a new file which does not have a history.");
	    			
	    		} else {
	    			df.format(diff);
		  	      	diff.getOldId();
		  	      	String diffText = out.toString("UTF-8");
		  	      	System.out.println(diffText);
		  	      	out.reset();
	    		} 		
	    	}
	      
	    }
	    
	}
	


}