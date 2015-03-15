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

	public static void main(String[] args) throws GitAPIException, IncorrectObjectTypeException, IOException {
		// TODO Auto-generated method stub
		
		
		//printBranch(localRepo);
		printBlame();
		printDiff();
		
	}
	
	private static Repository setUpRepo(String localPath) throws IOException {
		return new FileRepository(localPath + "/.git");
		
	}
	
	private static void printBranch(Repository localRepo) throws IOException{
		String currentBranch = localRepo.getBranch();
		System.out.println(currentBranch);
	}
	
	private static void printBlame() throws IOException, GitAPIException {
		String localPath = "/home/tcllib/TestGitRepo"; //change this to your local repo path
		Repository  localRepo = setUpRepo(localPath);
		BlameCommand blame = new BlameCommand(localRepo);
		blame.setFilePath("test1");
		BlameResult result = blame.call();
		
		String author = result.getSourceCommit(0).toString();
		System.out.println(author);
		localRepo.close();
	}
	
	private static void printDiff() throws GitAPIException, IncorrectObjectTypeException, IOException {
		String localPath = "/home/tcllib/TestGitRepo"; //change this to your local repo path
		Repository  localRepo = setUpRepo(localPath);
		BlameCommand blame = new BlameCommand(localRepo);
		blame.setFilePath("test1");
		BlameResult result = blame.call();
		ObjectReader reader = localRepo.newObjectReader();
		ObjectId newId = result.getSourceCommit(0).getTree().getId();
		ObjectId oldId = result.getSourceCommit(0).getParent(0).getTree().getId();
		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
	    oldTreeIter.reset(reader, oldId);
	    CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
	    newTreeIter.reset(reader, newId);
	    
	    Git myGit = new Git(localRepo);
	    List<DiffEntry> diffs= myGit.diff()
	            .setNewTree(newTreeIter)
	            .setOldTree(oldTreeIter)
	            .call();
	    
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    DiffFormatter df = new DiffFormatter(out);
	    df.setRepository(localRepo);
	    
	    for(DiffEntry diff : diffs)
	    {
	      df.format(diff);
	      diff.getOldId();
	      String diffText = out.toString("UTF-8");
	      System.out.println(diffText);
	      out.reset();
	    }
	    
	    localRepo.close();
	    
	}

}