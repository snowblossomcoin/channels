package channels;

import org.junit.Test;
import org.junit.Assert;
import java.util.Iterator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Files;
import snowblossom.channels.MiscUtils;
import snowblossom.channels.HtmlUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.text.Normalizer;


public class UnicodeTest
{

  public static final String input_string="ゲド戦記";

  @Rule
  public TemporaryFolder test_folder = new TemporaryFolder();


  @Test
  public void testNothing()
  {
    System.out.println(MiscUtils.getStringCodePoints(input_string));
  }

  @Test
  public void testStringBuilder()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(input_string);
    Assert.assertEquals(input_string, sb.toString());
  }

  @Test
  public void testStringFormat()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(input_string);

    String formed = String.format("%s", input_string);

    Assert.assertEquals(input_string, formed);

    System.out.println(MiscUtils.getStringCodePoints(formed));
  }

  @Test
  public void testStringFormatInner()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("html\n");
    sb.append(input_string);

    sb.append(String.format("zing %s zing", input_string));

    Assert.assertTrue(sb.toString().contains(input_string));

  }


  /**
   * If this test is failing, you likely have an old version of java
   * https://github.com/snowblossomcoin/channels/issues/35
   */
  @Test
  public void testFile()
    throws Exception
  {
    File base_dir = test_folder.newFolder();
    Path base_p = base_dir.toPath();

    String norm =  Normalizer.normalize(input_string, Normalizer.Form.NFKC);

    File n = new File(base_dir, norm);

    n.mkdir();
    //Path sub_p = n.toPath();
    //Files.createDirectory(sub_p);

    System.out.println("Base dir: " + base_dir);
    System.out.println("In: " + MiscUtils.getStringCodePoints(n.getName()));
    
    for (Iterator<Path> i = Files.newDirectoryStream(base_p).iterator(); i.hasNext(); ) 
    {
      Path p = i.next();
      System.out.println("Path: " + p.toString());

    }

    for(File f : base_dir.listFiles())
    {
      String name = "" + "/" + f.getName();
      System.out.println("Name: "  + MiscUtils.getStringCodePoints(name));
      //Assert.assertTrue(name.contains(input_string));

    }


  }

  @Test
  public void testHtml()
    throws Exception
  {
    File base_dir = test_folder.newFolder();

    File n = new File(base_dir, input_string);
    n.mkdir();

    System.out.println("Base dir: " + base_dir);


    String html = HtmlUtils.getIndex(base_dir);
    System.out.println(MiscUtils.getStringCodePoints(html));
    System.out.println(html);
    //Assert.assertTrue(html.contains( input_string) );

  }


}
