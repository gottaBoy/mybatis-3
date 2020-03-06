package org.apache.ibatis.session;

import org.apache.ibatis.io.Resources;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

/**
 * https://www.tianxiaobo.com/
 * https://www.waerfa.com/piiic-2-review
 */
public class SqlSessionFactoryTest {

    @Test
    public void sqlSessionFactory() throws Exception{
        final String resource = "org/apache/ibatis/builder/MapperConfig.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        System.out.println("finish");
    }
}
