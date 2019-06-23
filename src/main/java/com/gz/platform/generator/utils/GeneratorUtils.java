package com.gz.platform.generator.utils;

import com.gz.platform.generator.entity.ColumnEntity;
import com.gz.platform.generator.entity.TableEntity;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 代码生成器   工具类
 *
 */
public class GeneratorUtils {

	private static final String DOMAIN = "domain";
	
	private static final String SERVICE = "service";

	private static final String TEMPLATE = "template/";
	
	private static final String MAPPER_VM = "mapper.xml.vm";
	
	private static final String ISERVICE_VM = "iService.java.vm";
	
	private static final String SERVICE_IMPL_VM = "serviceImpl.java.vm";

	private static final String ENTITY_VM = "entity.java.vm";

	private static final String PAGE_LIST_DTO_VM = "pageListDto.java.vm";

	private static final String ALL_DTO_VM = "allDto.java.vm";
	
	private static final String CONTROLLER_VM = "controller.java.vm";
	
	private static final String REPOSTIORY_VM = "repository.java.vm";

    public static List<String> getTemplates() {
        List<String> templates = new ArrayList<String>();
//        templates.add("template/index.js.vm");
//        templates.add("template/index.vue.vm");
        templates.add(TEMPLATE.concat(MAPPER_VM));
        templates.add(TEMPLATE.concat(ISERVICE_VM));
        templates.add(TEMPLATE.concat(SERVICE_IMPL_VM));
        templates.add(TEMPLATE.concat(ENTITY_VM));
        templates.add(TEMPLATE.concat(PAGE_LIST_DTO_VM));
//        templates.add("template/mapper.java.vm");
        templates.add(TEMPLATE.concat(CONTROLLER_VM));
        templates.add(TEMPLATE.concat(ALL_DTO_VM));
        templates.add(TEMPLATE.concat(REPOSTIORY_VM));
        return templates;
    }

    /**
     * 生成代码
     */
    public static void generatorCode(Map<String, String> table, List<Map<String, String>> columns, ZipOutputStream zip) {
        //配置信息
        Configuration config = getConfig();

        //表信息
        TableEntity tableEntity = new TableEntity();
        tableEntity.setTableName(table.get("tableName"));
        tableEntity.setComments(table.get("tableComment"));
        //表名转换成Java类名
        String className = tableToJava(tableEntity.getTableName(), config.getString("tablePrefix"));
        tableEntity.setClassName(className);
        tableEntity.setClassname(StringUtils.uncapitalize(className));

        //列信息
        List<ColumnEntity> columsList = new ArrayList<>();
        for (Map<String, String> column : columns) {
            ColumnEntity columnEntity = new ColumnEntity();
            columnEntity.setColumnName(column.get("columnName"));
            columnEntity.setDataType(column.get("dataType"));
            columnEntity.setComments(column.get("columnComment"));
            columnEntity.setExtra(column.get("extra"));

            //列名转换成Java属性名
            String attrName = columnToJava(columnEntity.getColumnName());
            columnEntity.setAttrName(attrName);
            columnEntity.setAttrname(StringUtils.uncapitalize(attrName));

            //列的数据类型，转换成Java类型
            String attrType = config.getString(columnEntity.getDataType(), "unknowType");
            columnEntity.setAttrType(attrType);

            //是否主键
            if ("PRI".equalsIgnoreCase(column.get("columnKey")) && tableEntity.getPk() == null) {
                tableEntity.setPk(columnEntity);
            }

            columsList.add(columnEntity);
        }
        tableEntity.setColumns(columsList);

        //没主键，则第一个字段为主键
        if (tableEntity.getPk() == null) {
            tableEntity.setPk(tableEntity.getColumns().get(0));
        }

        //设置velocity资源加载器
        Properties prop = new Properties();
        prop.put("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        Velocity.init(prop);

        //封装模板数据
        Map<String, Object> map = new HashMap<>();
        map.put("tableName", tableEntity.getTableName());
        map.put("comments", tableEntity.getComments());
        map.put("pk", tableEntity.getPk());
        map.put("className", tableEntity.getClassName());
        map.put("classname", tableEntity.getClassname());
        map.put("pathName", tableEntity.getClassname().toLowerCase());
        map.put("columns", tableEntity.getColumns());
        map.put("package", config.getString("package"));
        map.put("author", config.getString("author"));
        map.put("email", config.getString("email"));
        map.put("datetime", DateUtils.format(new Date(), DateUtils.DATE_TIME_PATTERN));
        map.put("moduleName", config.getString("mainModule"));
        map.put("moduleCnName", config.getString("moduleCnName"));
        map.put("secondModuleName", toLowerCaseFirstOne(className));
        map.put("upCaseModuleName", className.toUpperCase());
        map.put("lowCaseModuleName", className.toLowerCase());
        VelocityContext context = new VelocityContext(map);

        //获取模板列表
        List<String> templates = getTemplates();
        for (String template : templates) {
            //渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, "UTF-8");
            tpl.merge(context, sw);

            try {
                //添加到zip
                zip.putNextEntry(new ZipEntry(getFileName(template, tableEntity.getClassName(), config.getString("package"), config.getString("mainModule"))));
                IOUtils.write(sw.toString(), zip, "UTF-8");
                IOUtils.closeQuietly(sw);
                zip.closeEntry();
            } catch (IOException e) {
                throw new RuntimeException("渲染模板失败，表名：" + tableEntity.getTableName(), e);
            }
        }
    }


    /**
     * 列名转换成Java属性名
     */
    public static String columnToJava(String columnName) {
        return WordUtils.capitalizeFully(columnName, new char[]{'_'}).replace("_", "");
    }

    /**
     * 表名转换成Java类名
     */
    public static String tableToJava(String tableName, String tablePrefix) {
        if (StringUtils.isNotBlank(tablePrefix)) {
            tableName = tableName.replace(tablePrefix, "");
        }
        return columnToJava(tableName);
    }

    /**
     * 获取配置信息
     */
    public static Configuration getConfig() {
        try {
            return new PropertiesConfiguration("generator.properties");
        } catch (ConfigurationException e) {
            throw new RuntimeException("获取配置文件失败，", e);
        }
    }

    /**
     * 获取文件名
     */
    public static String getFileName(String template, String className, String packageName, String moduleName) {
        String packagePath = "main" + File.separator + "java" + File.separator;
//        String frontPath = "ui" + File.separator;
        if (StringUtils.isNotBlank(packageName)) {
            packagePath += packageName.replace(".", File.separator) + File.separator;
        }

//        if (template.contains("index.js.vm")) {
//            return frontPath + "api" + File.separator + moduleName + File.separator + toLowerCaseFirstOne(className) + File.separator + "index.js";
//        }
//
//        if (template.contains("index.vue.vm")) {
//            return frontPath + "views" + File.separator + moduleName + File.separator + toLowerCaseFirstOne(className) + File.separator + "index.vue";
//        }

        if (template.contains(ISERVICE_VM)) {
            return packagePath + moduleName + File.separator + DOMAIN + File.separator + SERVICE + File.separator + "I" + className + "Service.java";
        }
        if (template.contains(SERVICE_IMPL_VM)) {
            return packagePath + moduleName + File.separator + DOMAIN + File.separator + SERVICE + File.separator+ "impl" + File.separator + className + "ServiceImpl.java";
        }
//        if (template.contains("mapper.java.vm")) {
//            return packagePath + "mapper" + File.separator + className + "Mapper.java";
//        }
        if (template.contains(ENTITY_VM)) {
            return packagePath + moduleName + File.separator + "infra" + File.separator + "entity" + File.separator + className + "Entity.java";
        }
        if (template.contains(PAGE_LIST_DTO_VM)) {
            return packagePath + moduleName + File.separator + "dto" + File.separator + className + "4PageListDto.java";
        }
        if (template.contains(ALL_DTO_VM)) {
            return packagePath + moduleName + File.separator + "dto" + File.separator + className + "4AllDto.java";
        }
        if (template.contains(CONTROLLER_VM)) {
            return packagePath  + moduleName + File.separator + "controller" + File.separator + className + "Controller.java";
        }
        if (template.contains(MAPPER_VM)) {
            return "main" + File.separator + "resources" + File.separator + "mapper" + File.separator + className + "Mapper.xml";
        }
        if (template.contains(REPOSTIORY_VM)) {
        	return packagePath + moduleName + File.separator + "infra" + File.separator + "repository" + File.separator + className + "Repository.java";
        }

        return null;
    }

    //首字母转小写
    public static String toLowerCaseFirstOne(String s) {
        if (Character.isLowerCase(s.charAt(0))) {
            return s;
        } else {
            return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
        }
    }
}
