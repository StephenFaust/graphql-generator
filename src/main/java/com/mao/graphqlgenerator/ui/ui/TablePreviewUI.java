package com.mao.graphqlgenerator.ui.ui;


import com.mao.graphqlgenerator.dto.TableUIInfo;
import com.intellij.database.model.DasColumn;
import com.intellij.database.model.DasTable;
import com.intellij.database.psi.DbTable;
import com.intellij.database.util.DasUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ui.configuration.ChooseModulesDialog;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class TablePreviewUI extends DialogWrapper {
    private JPanel rootPanel;
    private JPanel listPanel;
    private JTextField moduleChooseTextField;
    private List<DbTable> dbTables;

    ListTableModel<TableUIInfo> model = new ListTableModel<>(
            new TableColumnInfo("TableName"),
            new TableColumnInfo("SchemaName")
    );

    private String typeMap(String dataType) {
        switch (dataType) {
            case "bigint":
            case "timestamp":
                return "Long";
            case "integer":
            case "smallint":
                return "Int";
            case "bigint[]":
                return "[Long]";
            case "integer[]":
            case "smallint[]":
                return "[Int]";
            default:
                return "String";
        }
    }

    private String getGraphqlStr(DasTable dasTable, DasColumn column) {
        String str = "    \"\"\"" + column.getComment() + "\"\"\"\n";
        if (dasTable.getColumnAttrs(column).contains(DasColumn.Attribute.PRIMARY_KEY)) {
            str += "    " + toLowerCamelCase(column.getName()) + ":ID\n";
        } else {
            str += "    " + toLowerCamelCase(column.getName()) + ":" + typeMap(column.getDataType().typeName) + "\n";
        }
        return str;
    }


    private String toLowerCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;
        for (int i = 0; i < str.length(); i++) {
            char currentChar = str.charAt(i);
            if (currentChar == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(currentChar));
                    nextUpperCase = false;
                } else {
                    result.append(Character.toLowerCase(currentChar));
                }
            }
        }
        return result.toString();
    }

    private String toUpperCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String[] words = str.split("[^a-zA-Z0-9]+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (!word.isEmpty()) {
                result.append(word.substring(0, 1).toUpperCase());
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }
        return result.toString();
    }


    private void formatGraphQLFile(String schemaString, String filePath, String fileName) throws IOException {
        File dir = new File(filePath);
        dir.mkdirs();
        File file = new File(filePath, fileName);
        file.createNewFile();
        try (FileWriter fileWriter = new FileWriter(file, StandardCharsets.UTF_8)) {
            fileWriter.write(schemaString);
        }
    }


    private boolean isIgnoreField(DasColumn column) {
        List<String> ignoreFieldList = List.of(
                "deleted", "version"
        );
        return ignoreFieldList.contains(column.getName());
    }

    private boolean generateFiles() {
        if (StringUtils.isEmpty(this.moduleChooseTextField.getText())) {
            Messages.showWarningDialog("Directory not empty!", "Warning");
            return false;
        }
        for (DbTable dbTable : dbTables) {
            JBIterable<? extends DasColumn> columns = DasUtil.getColumns(dbTable.getDasObject());
            StringBuilder sb = new StringBuilder("type " + toUpperCamelCase(dbTable.getName()) + "Payload {\n");
            for (DasColumn column : columns) {
                if (!isIgnoreField(column)) {
                    sb.append(getGraphqlStr(dbTable.getDasObject(), column));
                }
            }
            sb.append("}");
            try {
                String moduleDirPath = this.moduleChooseTextField.getText();
                String textName = toLowerCamelCase(dbTable.getName()) + ".graphql";
                String pathName = moduleDirPath + "/src/main/resources/schema/";
                formatGraphQLFile(sb.toString(), pathName, textName);
            } catch (IOException ex) {
                Messages.showErrorDialog(ex.getMessage(), "Error");
            }
        }
        return true;
    }


    @Override
    protected void doOKAction() {
        if (generateFiles()) {
            VirtualFileManager.getInstance().syncRefresh();
            super.doOKAction();
        }
    }

    private static class TableColumnInfo extends ColumnInfo<TableUIInfo, String> {
        public TableColumnInfo(String name) {
            super(name);
        }

        @Override
        public @Nullable String valueOf(TableUIInfo item) {
            String value = null;
            if (getName().equals("TableName")) {
                value = item.getTableName();
            } else if (getName().equals("SchemaName")) {
                value = item.getSchemaName();
            }
            return value;
        }

    }


    public TablePreviewUI(@Nullable Project project) {
        super(project);
        init();
        setTitle("Graphql-Generator");
    }


    private void refreshTableNames(List<DbTable> dbTables) {
        for (int currentRowIndex = model.getRowCount() - 1; currentRowIndex >= 0; currentRowIndex--) {
            model.removeRow(currentRowIndex);
        }
        for (DbTable dbTable : dbTables) {
            String tableName = dbTable.getName();
            String schemaName = toLowerCamelCase(dbTable.getName());
            model.addRow(new TableUIInfo(tableName, schemaName));
        }
    }


    public void fillData(Project project, List<DbTable> dbTables) {
        this.dbTables = dbTables;
        TableView<TableUIInfo> tableView = new TableView<>(model);
        GridConstraints gridConstraints = new GridConstraints();
        gridConstraints.setFill(GridConstraints.FILL_HORIZONTAL);
        listPanel.add(ToolbarDecorator.createDecorator(tableView)
                        .setPreferredSize(new Dimension(400, 200))
                        .disableAddAction()
                        .disableRemoveAction()
                        .disableUpDownActions()
                        .createPanel(),
                gridConstraints);
        moduleChooseTextField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                chooseModule(project);
            }
        });
        refreshTableNames(dbTables);
    }


    private void chooseModule(Project project) {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        ChooseModulesDialog dialog = new ChooseModulesDialog(project, Arrays.asList(modules), "Choose Module", "Choose single module");
        dialog.setSingleSelectionMode();
        dialog.show();

        List<Module> chosenElements = dialog.getChosenElements();
        if (!chosenElements.isEmpty()) {
            Module module = chosenElements.get(0);
            chooseModulePath(module);
        }
    }

    private void chooseModulePath(Module module) {
        String moduleDirPath = ModuleUtilCore.getModuleDirPath(module);
        int childModuleIndex = indexFromChildModule(moduleDirPath);
        if (hasChildModule(childModuleIndex)) {
            Optional<String> pathFromModule = getPathFromModule(module);
            if (pathFromModule.isPresent()) {
                moduleDirPath = pathFromModule.get();
            } else {
                moduleDirPath = moduleDirPath.substring(0, childModuleIndex);
            }
        }

        moduleChooseTextField.setText(moduleDirPath);
    }

    private boolean hasChildModule(int childModuleIndex) {
        return childModuleIndex > -1;
    }

    private int indexFromChildModule(String moduleDirPath) {
        return moduleDirPath.indexOf(".idea");
    }

    private Optional<String> getPathFromModule(Module module) {
        // 兼容gradle获取子模块
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
        if (contentRoots.length == 1) {
            return Optional.of(contentRoots[0].getPath());
        }
        return Optional.empty();
    }


    @Override
    protected @Nullable JComponent createCenterPanel() {
        return rootPanel;
    }
}
