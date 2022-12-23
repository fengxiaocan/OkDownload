package com.app.test;

import com.x.down.m3u8.M3U8Info;
import com.x.down.m3u8.M3U8Utils;
import com.x.down.tool.XDownUtils;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
//        assertEquals(4, 2 + 2);

        File file = new File(".", "a.php");
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            M3U8Info info = M3U8Utils.parseNetworkM3U8Info(null, bufferedReader);
            System.err.println(info.toString());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            XDownUtils.closeIo(bufferedReader);
        }
    }


}