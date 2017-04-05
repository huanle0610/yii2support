package com.nvlad.yii2support.database;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.database.model.DasColumn;
import com.intellij.database.model.DasObject;
import com.intellij.database.model.DasTable;
import com.intellij.database.psi.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocProperty;
import com.jetbrains.php.lang.psi.elements.*;
import com.nvlad.yii2support.common.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by oleg on 23.03.2017.
 */
public class DatabaseUtils {

    @Nullable
    static ArrayList<LookupElementBuilder> getLookupItemsByTable(String table, Project project, PhpExpression position) {
        ArrayList<LookupElementBuilder> list = new ArrayList<>();
        if (table == null ||table.isEmpty())
            return list;

        DbPsiFacade facade =  DbPsiFacade.getInstance(project);
        List<DbDataSource> dataSources = facade.getDataSources();
        for (DbDataSource source: dataSources) {
            for (Object item : source.getModel().traverser().children(source.getModel().getCurrentRootNamespace()) ) {
                if (item instanceof DbTable && ((DbTable) item).getName().equals(table)) {
                    TableInfo tableInfo = new TableInfo((DbTable) item);
                    for (DasColumn column : tableInfo.getColumns()) {
                        list.add(DatabaseUtils.buildLookup(column, position));
                    }
                }
            }
        }
        return list;
    }

    static ArrayList<LookupElementBuilder> getLookupItemsTables(Project project, PhpExpression position) {
        DbPsiFacade facade =  DbPsiFacade.getInstance(project);
        List<DbDataSource> dataSources = facade.getDataSources();

        // Code to test tests :)
        //dataSources.clear();
        //dataSources.add(new TestDataSource(project));

        ArrayList<LookupElementBuilder> list = new  ArrayList<>();
        for (DbDataSource source: dataSources) {
            for (Object item : source.getModel().traverser().children(source.getModel().getCurrentRootNamespace()) ) {
                if (item instanceof DbTable) {
                    list.add(DatabaseUtils.buildLookup(item, position));
                }
            }
        }
        return list;
    }

    static ArrayList<LookupElementBuilder> getLookupItemsByAnnotations(PhpClass phpClass, PhpExpression position) {
        if (phpClass == null)
            return null;
        final ArrayList<LookupElementBuilder> result = new ArrayList<>();
        final Field[] fields = phpClass.getOwnFields();
        for (Field field : fields) {
            if (field instanceof PhpDocProperty) {
                result.add(buildLookup(field, position));
            }
        }

        return result;
    }

    static String[] extractParamsFromCondition(String condition) {
        LinkedHashSet<String> matches = new LinkedHashSet<>();
        String pattern = ":\\w+";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(condition);
        while (m.find()) {
            matches.add(m.group());
        }
        return (String[])matches.toArray(new String[0]);
    }

    @NotNull
    static private LookupElementBuilder buildLookup(Object field, PhpExpression position) {
        String lookupString = "-";
        if (field instanceof DasObject)
            lookupString = ((DasObject)field).getName();
        if (field instanceof Field) {
            lookupString = ((Field) field).getName();
        }
        LookupElementBuilder builder =  LookupElementBuilder.create(field, lookupString);

        if (field instanceof Field) {
            builder = builder.withTypeText(((Field) field).getType().toString())
                             .withIcon(((Field) field).getIcon());
        }
        if (field instanceof DasColumn) {
            DasColumn column = (DasColumn)field;
            builder = builder.withTypeText(column.getDataType().typeName);
            if (column.getDbParent() != null)
                builder = builder.withTailText(" => " + column.getDbParent().getDbParent().getName() + '.' +column.getTableName(), true);
            if (column instanceof  DbColumnImpl)
                builder = builder.withIcon(((DbColumnImpl) column).getIcon());
        }
        if (field instanceof DasTable) {
            DasTable table = (DasTable)field;
            DasObject tableSchema = table.getDbParent();
            builder = builder.withTypeText("DbTable");
            if (tableSchema != null)
                builder = builder.withTailText(" => " + table.getDbParent().getName(), true);
            if (table instanceof DbTableImpl )
                builder = builder.withIcon(((DbTableImpl) table).getIcon());
        }
        return builder;
    }

    @Nullable
    static String getTableByActiveRecordClass(PhpClass phpClass) {
        Method method = phpClass.findMethodByName("tableName");
        Collection<PhpReturn> returns = PsiTreeUtil.findChildrenOfType(method, PhpReturn.class);
        for (PhpReturn element: returns) {
            if ((element).getChildren().length > 0) {
                return clearTablePrefixTags((element).getChildren()[0].getText());
            }
        }

        return null;
    }

    static String clearTablePrefixTags(String str) {
        return str.replace("{{%", "").replace("}}", "").replace("{{", "");
    }

}
