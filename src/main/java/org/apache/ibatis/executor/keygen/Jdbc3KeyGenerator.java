/**
 *    Copyright 2009-2018 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.executor.keygen;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.ArrayUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.defaults.DefaultSqlSession.StrictMap;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class Jdbc3KeyGenerator implements KeyGenerator {

  /**
   * A shared instance.
   *
   * @since 3.4.3
   */
  public static final Jdbc3KeyGenerator INSTANCE = new Jdbc3KeyGenerator();

  @Override
  public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
    // do nothing
  }

  @Override
  public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
    processBatch(ms, stmt, parameter);
  }

  /**
   * 获取主键数组(keyProperties)
   * 获取 ResultSet 元数据
   * 遍历参数列表，为每个主键属性获取 TypeHandler
   * 从 ResultSet 中获取主键数据，并填充到参数中
   * @param ms
   * @param stmt
   * @param parameter
   */
  public void processBatch(MappedStatement ms, Statement stmt, Object parameter) {
    // 获取主键字段
    final String[] keyProperties = ms.getKeyProperties();
    if (keyProperties == null || keyProperties.length == 0) {
      return;
    }
    try (ResultSet rs = stmt.getGeneratedKeys()) {
      final Configuration configuration = ms.getConfiguration();
      // 获取结果集 ResultSet 的元数据
      // ResultSet 中数据的列数要大于等于主键的数量
      if (rs.getMetaData().getColumnCount() >= keyProperties.length) {
        Object soleParam = getSoleParameter(parameter);
        if (soleParam != null) {
          assignKeysToParam(configuration, rs, keyProperties, soleParam);
        } else {
          assignKeysToOneOfParams(configuration, rs, keyProperties, (Map<?, ?>) parameter);
        }
      }
    } catch (Exception e) {
      throw new ExecutorException("Error getting generated key or setting result to parameter object. Cause: " + e, e);
    }
  }

  protected void assignKeysToOneOfParams(final Configuration configuration, ResultSet rs, final String[] keyProperties,
      Map<?, ?> paramMap) throws SQLException {
    // Assuming 'keyProperty' includes the parameter name. e.g. 'param.id'.
    int firstDot = keyProperties[0].indexOf('.');
    if (firstDot == -1) {
      throw new ExecutorException(
          "Could not determine which parameter to assign generated keys to. "
              + "Note that when there are multiple parameters, 'keyProperty' must include the parameter name (e.g. 'param.id'). "
              + "Specified key properties are " + ArrayUtil.toString(keyProperties) + " and available parameters are "
              + paramMap.keySet());
    }
    String paramName = keyProperties[0].substring(0, firstDot);
    Object param;
    if (paramMap.containsKey(paramName)) {
      param = paramMap.get(paramName);
    } else {
      throw new ExecutorException("Could not find parameter '" + paramName + "'. "
          + "Note that when there are multiple parameters, 'keyProperty' must include the parameter name (e.g. 'param.id'). "
          + "Specified key properties are " + ArrayUtil.toString(keyProperties) + " and available parameters are "
          + paramMap.keySet());
    }
    // Remove param name from 'keyProperty' string. e.g. 'param.id' -> 'id'
    String[] modifiedKeyProperties = new String[keyProperties.length];
    for (int i = 0; i < keyProperties.length; i++) {
      if (keyProperties[i].charAt(firstDot) == '.' && keyProperties[i].startsWith(paramName)) {
        modifiedKeyProperties[i] = keyProperties[i].substring(firstDot + 1);
      } else {
        throw new ExecutorException("Assigning generated keys to multiple parameters is not supported. "
            + "Note that when there are multiple parameters, 'keyProperty' must include the parameter name (e.g. 'param.id'). "
            + "Specified key properties are " + ArrayUtil.toString(keyProperties) + " and available parameters are "
            + paramMap.keySet());
      }
    }
    assignKeysToParam(configuration, rs, modifiedKeyProperties, param);
  }

  private void assignKeysToParam(final Configuration configuration, ResultSet rs, final String[] keyProperties,
      Object param)
      throws SQLException {
    final TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    final ResultSetMetaData rsmd = rs.getMetaData();
    // Wrap the parameter in Collection to normalize the logic.
    Collection<?> paramAsCollection;
    /*
     * 如果 parameter 是 Map 类型，则从其中提取指定 key 对应的值。
     * 至于 Map 中为什么会出现 collection/list/array 等键。大家
     * 可以参考 DefaultSqlSession 的 wrapCollection 方法
     */
    if (param instanceof Object[]) {
      paramAsCollection = Arrays.asList((Object[]) param);
    } else if (!(param instanceof Collection)) {
      paramAsCollection = Arrays.asList(param);
    } else {
      paramAsCollection = (Collection<?>) param;
    }
    TypeHandler<?>[] typeHandlers = null;
    // 遍历 parameters
    for (Object obj : paramAsCollection) {
      if (!rs.next()) {
        break;
      }
      MetaObject metaParam = configuration.newMetaObject(obj);
      if (typeHandlers == null) {
        // 为每个主键属性获取 TypeHandler
        typeHandlers = getTypeHandlers(typeHandlerRegistry, metaParam, keyProperties, rsmd);
      }
      // 填充结果到运行时参数中
      populateKeys(rs, metaParam, keyProperties, typeHandlers);
    }
  }

  private Object getSoleParameter(Object parameter) {
    if (!(parameter instanceof ParamMap || parameter instanceof StrictMap)) {
      return parameter;
    }
    Object soleParam = null;
    for (Object paramValue : ((Map<?, ?>) parameter).values()) {
      if (soleParam == null) {
        soleParam = paramValue;
      } else if (soleParam != paramValue) {
        soleParam = null;
        break;
      }
    }
    return soleParam;
  }

  private TypeHandler<?>[] getTypeHandlers(TypeHandlerRegistry typeHandlerRegistry, MetaObject metaParam, String[] keyProperties, ResultSetMetaData rsmd) throws SQLException {
    TypeHandler<?>[] typeHandlers = new TypeHandler<?>[keyProperties.length];
    for (int i = 0; i < keyProperties.length; i++) {
      if (metaParam.hasSetter(keyProperties[i])) {
        Class<?> keyPropertyType = metaParam.getSetterType(keyProperties[i]);
        typeHandlers[i] = typeHandlerRegistry.getTypeHandler(keyPropertyType, JdbcType.forCode(rsmd.getColumnType(i + 1)));
      } else {
        throw new ExecutorException("No setter found for the keyProperty '" + keyProperties[i] + "' in '"
            + metaParam.getOriginalObject().getClass().getName() + "'.");
      }
    }
    return typeHandlers;
  }

  private void populateKeys(ResultSet rs, MetaObject metaParam, String[] keyProperties, TypeHandler<?>[] typeHandlers) throws SQLException {
    // 遍历 keyProperties
    for (int i = 0; i < keyProperties.length; i++) {
      // 获取主键属性
      String property = keyProperties[i];
      TypeHandler<?> th = typeHandlers[i];
      if (th != null) {
        // 从 ResultSet 中获取某列的值
        Object value = th.getResult(rs, i + 1);
        // 设置结果值到运行时参数中
        metaParam.setValue(property, value);
      }
    }
  }

}
