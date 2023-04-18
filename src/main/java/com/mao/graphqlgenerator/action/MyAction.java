package com.mao.graphqlgenerator.action;

import com.mao.graphqlgenerator.ui.ui.TablePreviewUI;
import com.intellij.database.psi.DbTable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

public class MyAction extends AnAction {

    private static final Logger logger = LoggerFactory.getLogger(MyAction.class);


    private TablePreviewUI tablePreviewUI;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        /**
         * 代码生成
         */
        Project project = e.getProject();
        PsiElement[] tableElements = e.getData(LangDataKeys.PSI_ELEMENT_ARRAY);
        if (tableElements == null) {
            logger.error("error");
            return;
        }
        List<DbTable> dbTables = Stream.of(tableElements).filter(t -> t instanceof DbTable).map(t -> (DbTable) t).collect(Collectors.toList());
        tablePreviewUI = new TablePreviewUI(project);
        tablePreviewUI.fillData(project, dbTables);
        tablePreviewUI.show();
    }

    private static volatile Boolean existsDatabaseTools = null;

    public static boolean existsDbTools() {
        if (existsDatabaseTools == null) {
            synchronized (MyAction.class) {
                if (existsDatabaseTools == null) {
                    try {
                        Class.forName("com.intellij.database.psi.DbTable");
                        existsDatabaseTools = true;
                    } catch (ClassNotFoundException ex) {
                        existsDatabaseTools = false;
                    }
                }
            }
        }
        return existsDatabaseTools;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Boolean visible = null;
        PsiElement[] psiElements = e.getData(LangDataKeys.PSI_ELEMENT_ARRAY);
        if (psiElements == null || psiElements.length == 0) {
            visible = false;
        }
        boolean existsDbTools = existsDbTools();
        if (!existsDbTools) {
            visible = false;
        }
        if (visible == null) {
            if (!Stream.of(psiElements).allMatch(item -> CheckMatch.checkAssignableFrom(item.getClass()))) {
                visible = false;
            }
        }
        // 未安装Database Tools插件时，不展示菜单
        if (visible != null) {
            e.getPresentation().setEnabledAndVisible(visible);
        }

    }

    private static class CheckMatch {
        public static boolean checkAssignableFrom(Class<? extends PsiElement> aClass) {
            try {
                return DbTable.class.isAssignableFrom(aClass);
            } catch (Exception e) {
                return false;
            }
        }
    }


}
