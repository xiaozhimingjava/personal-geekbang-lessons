package org.geektimes.projects.user.sql;

import org.geektimes.function.ThrowableFunction;
import org.geektimes.projects.user.domain.User;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.apache.commons.lang.ClassUtils.wrapperToPrimitive;

/**
 * JDBC 模板工具类
 *
 * @author tanheyuan
 * @version 1.0
 * @since 2021/3/3
 */
public class JdbcTemplate {

    /**
     * JavaBean field -> 数据库字段映射
     *
     * @param fieldName
     * @return
     */
    private static String mapColumnLabel(String fieldName) {
        return fieldName;
    }

    /**
     * 数据类型与 ResultSet 方法名映射
     */
    static Map<Class, String> resultSetMethodMappings = new HashMap<>();

    static Map<Class, String> preparedStatementMethodMappings = new HashMap<>();

    static {
        resultSetMethodMappings.put(Long.class, "getLong");
        resultSetMethodMappings.put(String.class, "getString");
        resultSetMethodMappings.put(Integer.class, "getInt");
        resultSetMethodMappings.put(Short.class, "getShort");
        resultSetMethodMappings.put(Boolean.class, "getBoolean");
        resultSetMethodMappings.put(Double.class, "getDouble");
        resultSetMethodMappings.put(BigDecimal.class, "getBigDecimal");
        resultSetMethodMappings.put(Date.class, "getDate");

        preparedStatementMethodMappings.put(Long.class, "setLong"); // long
        preparedStatementMethodMappings.put(String.class, "setString"); //
        preparedStatementMethodMappings.put(Integer.class, "setInt");
        preparedStatementMethodMappings.put(Short.class, "setShort");
        preparedStatementMethodMappings.put(Boolean.class, "setBoolean");
        preparedStatementMethodMappings.put(Double.class, "setDouble");
        preparedStatementMethodMappings.put(BigDecimal.class, "setBigDecimal");
        preparedStatementMethodMappings.put(Date.class, "setDate");


    }

    private final DBConnectionManager dbConnectionManager;

    public JdbcTemplate(DBConnectionManager dbConnectionManager) {
        this.dbConnectionManager = dbConnectionManager;
    }

    private Connection getConnection() {
        return dbConnectionManager.getConnection();
    }

    public void releaseConnection() {
        dbConnectionManager.releaseConnection();
    }

    /**
     * 结果集字段 -> JavaBean field 映射
     *
     * @param claz
     * @param resultSet
     * @param <T>
     * @return
     * @throws Exception
     */
    public <T> T fieldMapping(Class<T> claz, ResultSet resultSet) throws Exception {
        T instance = claz.newInstance();
        BeanInfo beanInfo = Introspector.getBeanInfo(claz, Object.class);
        for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
            String fieldName = propertyDescriptor.getName();
            Class fieldType = propertyDescriptor.getPropertyType();
            String methodName = resultSetMethodMappings.get(fieldType);
            // 可能存在映射关系（不过此处是相等的）
            String columnLabel = mapColumnLabel(fieldName);
            Method resultSetMethod = ResultSet.class.getMethod(methodName, String.class);
            // 通过放射调用 getXXX(String) 方法
            Object resultValue = resultSetMethod.invoke(resultSet, columnLabel);
            // 获取 User 类 Setter方法
            // PropertyDescriptor ReadMethod 等于 Getter 方法
            // PropertyDescriptor WriteMethod 等于 Setter 方法
            Method setterMethodFromUser = propertyDescriptor.getWriteMethod();
            // 以 id 为例，  user.setId(resultSet.getLong("id"));
            setterMethodFromUser.invoke(instance, resultValue);
        }
        return instance;
    }

    /**
     * @param sql
     * @param function
     * @param <T>
     * @return
     */
    public <T> T executeQuery(String sql, ThrowableFunction<ResultSet, T> function,
                                 Consumer<Throwable> exceptionHandler, Object... args) {
        Connection connection = getConnection();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                Class argType = arg.getClass();

                Class wrapperType = wrapperToPrimitive(argType);

                if (wrapperType == null) {
                    wrapperType = argType;
                }

                // Boolean -> boolean
                String methodName = preparedStatementMethodMappings.get(argType);
                Method method = PreparedStatement.class.getMethod(methodName, int.class, wrapperType);
                method.invoke(preparedStatement, i + 1, arg);
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            // 返回一个 POJO List -> ResultSet -> POJO List
            // ResultSet -> T
            return function.apply(resultSet);
        } catch (Throwable e) {
            exceptionHandler.accept(e);
        }
        return null;
    }

    /**
     * 插入 SQL 通用处理方法
     *
     * @param tableName 需要插入的数据表
     * @param argObj 需要插入的对象
     * @param function 异常处理函数接口
     * @param exceptionHandler 异常处理拦截器
     * @param <T> 返回的结果对象
     * @return
     */
    public <T> T executeInsert(String tableName, Object argObj, ThrowableFunction<Boolean, T> function,
                                  Consumer<Throwable> exceptionHandler) {
        Connection connection = getConnection();
        StringBuilder sqlColumnBuilder = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder sqlValueBuilder = new StringBuilder(" VALUES (");
        try {
            Field[] declaredFields = argObj.getClass().getDeclaredFields();
            // 拼接预编译 sql
            // 从 1 开始，排除主键 id 插入
            for (int i = 1; i < declaredFields.length; i++) {
                Field field = declaredFields[i];
                // 字段名
                //String fieldName = propertyDescriptor.getName();
                String fieldName = field.getName();
                // 可能存在映射关系（不过此处是相等的）
                String columnLabel = mapColumnLabel(fieldName);
                sqlColumnBuilder.append(columnLabel).append(", ");
                sqlValueBuilder.append("?, ");
            }
            sqlColumnBuilder.replace(sqlColumnBuilder.length() - 2, sqlColumnBuilder.length(), " )");
            sqlValueBuilder.replace(sqlValueBuilder.length() - 2, sqlColumnBuilder.length(), " )");
            String sql = sqlColumnBuilder.append(sqlValueBuilder).toString();
            System.out.println("插入 sql 语句：" + sql);
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            // sql 参数处理
            BeanInfo beanInfo = Introspector.getBeanInfo(argObj.getClass(), Object.class);
            Field[] argObjDeclaredFields = argObj.getClass().getDeclaredFields();
            for (PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors()) {
                for (int i = 1; i < argObjDeclaredFields.length; i++) {
                    Field field = argObjDeclaredFields[i];
                    if (field.getName().equals(propertyDescriptor.getName())) {
                        // 获取对应属性的值
                        Method getFieldMethod = propertyDescriptor.getReadMethod();
                        Object fieldValue = getFieldMethod.invoke(argObj);
                        if (null == fieldValue) {
                            Method method = PreparedStatement.class.getMethod("setNull", int.class, int.class);
                            method.invoke(preparedStatement, i + 1, Types.VARCHAR);
                            continue;
                        }
                        Class argType = fieldValue.getClass();

                        Class wrapperType = wrapperToPrimitive(argType);

                        if (wrapperType == null) {
                            wrapperType = argType;
                        }

                        // Boolean -> boolean
                        String methodName = preparedStatementMethodMappings.get(argType);
                        Method method = PreparedStatement.class.getMethod(methodName, int.class, wrapperType);
                        method.invoke(preparedStatement, i, wrapperType.cast(fieldValue));
                    }
                }
            }
            int execute = preparedStatement.executeUpdate();
            // 返回前释放数据库连接
            releaseConnection();
            return function.apply(execute > 0);
        } catch (Throwable e) {
            e.printStackTrace();
            exceptionHandler.accept(e);
        }
        return null;
    }
}
