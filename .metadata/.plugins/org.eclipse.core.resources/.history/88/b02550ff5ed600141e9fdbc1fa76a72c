import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.MyersDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
		
public class DiffTest {

	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		/**Copyright Steve Jin 2013 */
	
		 
		
		  
		    ByteArrayOutputStream out = new ByteArrayOutputStream();
		     
		    try
		    {
		      RawText rt1 = new RawText(new File("/home/tcllib/diffTest/old"));
		      RawText rt2 = new RawText(new File("/home/tcllib/diffTest/new"));
		      EditList diffList = new EditList();
		      diffList.addAll(MyersDiff.INSTANCE.diff(RawTextComparator.DEFAULT,rt1,rt2));
		      new DiffFormatter(out).format(diffList, rt1, rt2);
		      
		      System.out.println("size of old is: " + rt1.size());
		      System.out.println("size of new is: " + rt2.size());
		      
		      
		      System.out.println("region: ");
			  for (Edit edit : diffList) {
				  System.out.println("length of A is: " + edit.getLengthA());
				  System.out.println("length of B is: " + edit.getLengthB());
				  System.out.println("beginA");
				  System.out.println(edit.getBeginA());
				  System.out.println("endA");
				  System.out.println(edit.getEndA());
				  System.out.println("beginB");
				  System.out.println(edit.getBeginB());
				  System.out.println("endB");
				  System.out.println(edit.getEndB());
			  }
		      
		      
		    } catch (IOException e)
		    {
		      e.printStackTrace();
		    }
		    System.out.println(out.toString());
		    
		  
		

	}

}
