package com.example.wangyang.mywechatplugin;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        del(list);
    }

    private void del(List<Integer> list) {
        if (list != null)
            for (int i = 0; i < list.size(); i++) {

            }
    }
}