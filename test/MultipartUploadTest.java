package channels;

import com.google.protobuf.ByteString;
import java.net.URL;
import java.security.MessageDigest;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import snowblossom.channels.MultipartSlicer;
import snowblossom.lib.HexUtil;

public class MultipartUploadTest
{
  public static final String test_file_url="https://snowblossom.org/unittest/multipart-test-file";

  @Test
  public void testMultipart()
    throws Exception
  {
    URL u = new URL(test_file_url);

    MultipartSlicer ms = new MultipartSlicer(u.openStream());

    List<MultipartSlicer.FileData> files = ms.getFiles();

    Assert.assertEquals(5, files.size());

    {
      MultipartSlicer.FileData fd = files.get(0);
      Assert.assertEquals("a.exe", fd.meta.get("filename"));
      Assert.assertEquals(91313, fd.file_data.size());
      Assert.assertEquals("application/x-ms-dos-executable", fd.meta.get("Content-Type"));
      assertHash("e8a2a48a15f50031de81f29b492baed22cba12ec691351dc971a1e77882e8e9d",fd);
    }
    {
      MultipartSlicer.FileData fd = files.get(1);
      Assert.assertEquals("b.pdf", fd.meta.get("filename"));
      Assert.assertEquals(8192, fd.file_data.size());
      Assert.assertEquals("application/pdf", fd.meta.get("Content-Type"));
      assertHash("9f936ba45280577f82313f6b99f121a658dfd66aa8886c9d48743a7060bcb4e8",fd);
    }

    {
      MultipartSlicer.FileData fd = files.get(2);
      Assert.assertEquals("c.mp3", fd.meta.get("filename"));
      Assert.assertEquals(9, fd.file_data.size());
      Assert.assertEquals("audio/mp3", fd.meta.get("Content-Type"));
      assertHash("4f422827fe75de6fe41ba1916fe1bbaad3abb71f90198aa009b0e1a3791e142a",fd);
    }
    {
      MultipartSlicer.FileData fd = files.get(3);
      Assert.assertEquals("d.jpg", fd.meta.get("filename"));
      Assert.assertEquals(900, fd.file_data.size());
      Assert.assertEquals("image/jpeg", fd.meta.get("Content-Type"));
      assertHash("1c8be53bb4657cd6bf918f7eb1f0ed5d408cfb7b3d73be0460d247ae668c429e",fd);
    }
    {
      MultipartSlicer.FileData fd = files.get(4);
      Assert.assertEquals("e.jpeg", fd.meta.get("filename"));
      Assert.assertEquals(29183, fd.file_data.size());
      Assert.assertEquals("image/jpeg", fd.meta.get("Content-Type"));
      assertHash("fd741bb3ffe4bc7b3a7da0b20bda265a7ce3b304752eded4175923a099d96411",fd);
    }
    

  }

  private void assertHash(String hash_hex, MultipartSlicer.FileData fd)
    throws Exception
  {

    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(fd.file_data.toByteArray());
    ByteString hash = ByteString.copyFrom(md.digest());

    String hash_str = HexUtil.getHexString(hash);

    Assert.assertEquals(hash_hex, hash_str);


  }
}
