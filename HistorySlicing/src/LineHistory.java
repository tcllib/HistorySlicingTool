import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.diff.DiffEntry;

import java.io.ByteArrayOutputStream;

import org.eclipse.jgit.diff.DiffFormatter;

//this class mainly demonstrate how does API work
public class LineHistory {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//Initializing Git API
		File gitWorkDir = new File(args[0]);
		
		try
	    {
			Git.open(gitWorkDir);
	    }
	    catch (IOException e)
	    {
	    	System.out.println("Invalid directory");
	    	System.exit(0);
	    }
	    
		
		
	}
	


}