package org.apache.ibatis.jdbc;

import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * https://gitee.com/rainwen/spring-boot-showcase
 * https://gitee.com/rainwen/mybatis
 * https://my.oschina.net/wenjinglian/blog/1502414
 * https://www.w3school.com.cn/xpath/index.asp
 * https://www.yiibai.com/java_xml/java_xpath_parse_document.html
 * 初始化数据库表
 * 插入一条记录
 * 批量插入记录
 * 更新记录
 * 查询记录，解析表元数据、解析数据
 * 调用存储过程
 */
public class JdbcTest {
    protected static Connection connection;

    private static String url = "jdbc:mysql://localhost:3306/test";

    private static String username = "root";

    private static String password = "root";

    private static Boolean autoCommit = Boolean.TRUE;

    private static Statement statement;
    private static ResultSet resultSet;
    private static List<String> columnNames = new ArrayList<String>();


    public static void main(String[] args) throws SQLException {
        //初始化连接
        JdbcTest.initConnection();
        System.out.println("==> connection success");
        //插入
        insert();
        //批量插入
        insertBatch();
        //更新
        update();
        //查询解析
        parseQuery();
        //执行存储过程
        callProcedure();
    }

    public static void initConnection() throws SQLException {
        if(connection == null) {
            connection = DriverManager.getConnection(url, username, password);
            configureConnection(connection);
        }
    }

    private static void configureConnection(Connection conn) throws SQLException {
        if (autoCommit != null && autoCommit != conn.getAutoCommit()) {
            conn.setAutoCommit(autoCommit);
        }
    }

    /**
     * 解析查询
     *
     * @throws SQLException
     */
    public static void parseQuery() throws SQLException {
        //创建Statement对象
        statement = connection.createStatement();
        //定义查询语句
        String sql = "select * from t_user";
        //执行查询
        statement.executeQuery(sql);
        //获取查询结果集
        resultSet = statement.getResultSet();
        //解析表元数据
        parseTableMetaData();
        //解析表数据
        parseData();
    }

    /**
     * 解析表的元数据
     *
     * @throws SQLException
     */
    public static void parseTableMetaData() throws SQLException {
        System.out.println("==> parse meta data");
        //#mark 结果集元数据,表头信息
        final ResultSetMetaData metaData = resultSet.getMetaData();
        //表头数量
        final int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            //获取字段名
            System.out.print("columnName=[" + metaData.getColumnName(i) + "]");
            //字段数据类型
            System.out.print(" jdbcType=[" + JdbcType.forCode(metaData.getColumnType(i)) + "] ");
            //字段对应Java数据类型
            System.out.print(" classNames=[" + metaData.getColumnClassName(i) + "]");
            System.out.print(" tableName = " + metaData.getTableName(i));
            System.out.println();
            //保存所有表头，用于遍历数据
            columnNames.add(metaData.getColumnLabel(i));
        }
    }

    /**
     * 更多玩法: http://www.oschina.net/uploads/doc/javase-6-doc-api-zh_CN/java/sql/ResultSet.html
     * <p/>
     * 解析数据
     *
     * @throws SQLException
     */
    public static void parseData() throws SQLException {
        System.out.println("==> parse data");
        //首次输出
        print();
        //移动到最第一条之前
        resultSet.beforeFirst();
        System.out.println("isBeforeFirst:" + resultSet.isBeforeFirst());
        //第二次输出
        print();
        //移动到最第一条之前
        resultSet.beforeFirst();
        //移动到最后一条之后
        resultSet.afterLast();
        System.out.println("isAfterLast:" + resultSet.isBeforeFirst());
        //第三次输出
        print();

    }

    public static void print() throws SQLException {
        //游标移到下一位置，首次调用为第一条记录，第二次调用移动到第二条记录
        while (resultSet.next()) {
            //是否为第一条记录
            if (resultSet.isFirst()) {
                System.out.println(" first row");
            }
            //输出行下标
            System.out.print(" row num=" + resultSet.getRow() + " ");
            //输出表数据
            for (String column : columnNames) {
                System.out.print(" |(" + resultSet.findColumn(column) + ")" + column + "=" + resultSet.getObject(column));
            }
            System.out.println();
            //是否为最后一条记录
            if (resultSet.isLast()) {
                System.out.println(" last row");
            }
        }
    }

    /**
     * 更新
     */
    public static void update() throws SQLException {
        String sql = "UPDATE `t_user` SET name=? , mobile=? WHERE id=?";
        //创建preparedStatement对象
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        //参数设置从1开始
        preparedStatement.setString(1, "testUpdate");
        preparedStatement.setString(2, "18888888889");
        preparedStatement.setInt(3, 1);
        //执行插入
        int effectRow = preparedStatement.executeUpdate();
        System.out.println("effectRow:" + effectRow);
    }

    /**
     * 插入
     *
     * @throws SQLException
     */
    public static void insert() throws SQLException {
        String sql = "INSERT INTO  `t_user`(name,mobile,create_time, last_update_time) VALUES (?,?,?,?)";
        //创建preparedStatement对象
        PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        //参数设置从1开始
        preparedStatement.setString(1, "rain.wen");
        preparedStatement.setString(2, "18888888888");
        preparedStatement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
        preparedStatement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
        //执行插入
        int effectRow = preparedStatement.executeUpdate();
        System.out.println("effectRow:" + effectRow);
        //返回自增ID
        resultSet = preparedStatement.getGeneratedKeys();
        resultSet.next();
        //下标1开始
        System.out.println("generatedKey:" + resultSet.getInt(1));
    }

    /**
     * 批量插入
     * 更多玩法：http://www.oschina.net/uploads/doc/javase-6-doc-api-zh_CN/java/sql/PreparedStatement.html
     *
     * @throws SQLException
     */
    public static void insertBatch() throws SQLException {
        String sql = "INSERT INTO  `t_user`(name,mobile,create_time, last_update_time) VALUES (?,?,?,?)";
        //创建preparedStatement对象
        PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < 5; i++) {
            //参数设置从1开始
            preparedStatement.setString(1, "rain.wen" + i);
            preparedStatement.setString(2, "1888888888" + i);
            preparedStatement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            preparedStatement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            //添加一组参数到PreparedStatement对象的批处理命令中。
            preparedStatement.addBatch();
        }
        //执行插入
        int[] effectRow = preparedStatement.executeBatch();
        for (int i = 0; i < effectRow.length; i++) {
            System.out.println("effectRow:" + effectRow[0]);
        }

        //返回所有自增ID
        resultSet = preparedStatement.getGeneratedKeys();
        while (resultSet.next()) {
            //下标1开始
            System.out.println("generatedKey:" + resultSet.getInt(1));
        }
    }

    /**
     * 调用存储过程
     */
    public static void callProcedure() throws SQLException {
        String sql = "CALL proc_table_rows(?,?,?)";
        //创建CallableStatement对象
        CallableStatement prepareCall = connection.prepareCall(sql);
        //注册输出参数以及其值类型
        prepareCall.setString(1, "t_user");
        prepareCall.registerOutParameter(2, Types.INTEGER);
        //无用参数，测试
        prepareCall.setInt(3, 0);
        prepareCall.execute();
        System.out.println("call result:" + prepareCall.getInt(2));
    }


}
