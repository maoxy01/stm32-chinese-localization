import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.*;
import javax.swing.*;
import javax.swing.table.*;

public class LocalizationRegression {
    static int checks;
    static void check(boolean value, String name) {
        checks++;
        if (!value) throw new AssertionError(name);
        System.out.println("PASS " + name);
    }
    public static void main(String[] args) throws Exception {
        var dictionaryType = Class.forName("com.codex.cubemx.zh.TranslationDictionary");
        var dictionary = dictionaryType.getMethod("load",Path.class).invoke(null,Path.of(args[0]));
        var translatorType = Class.forName("com.codex.cubemx.zh.SwingTranslator");
        var translator = translatorType.getConstructor(dictionaryType).newInstance(dictionary);
        var translate = translatorType.getMethod("translateTree",java.awt.Component.class);
        SwingUtilities.invokeAndWait(() -> {
            try {
                var root = new JPanel();
                var input = new JTextField("Overview");
                root.add(input);
                var combo = new JComboBox<String>(new String[]{"Overview","Default","STM32F103C8Tx"});
                root.add(combo);
                var data = new DefaultTableModel(new Object[][]{{"STM32F103C8Tx",46.0,"Active"},{"STM32G473ZET6",20.0,"Obsolete"}},new Object[]{"Mounted Device","Unit Price (US$)","Marketing Status"});
                var table = new JTable(data);
                table.setAutoCreateRowSorter(true);
                root.add(table);
                var tabs = new JTabbedPane();
                tabs.addTab("Project Manager",new JPanel());
                root.add(tabs);
                var button = new JButton("Browse");
                var clickedCommand = new AtomicReference<String>();
                button.addActionListener(e -> clickedCommand.set(e.getActionCommand()));
                root.add(button);
                var menu = new JMenuItem("Save");
                var menuCommand = new AtomicReference<String>();
                menu.addActionListener(e -> menuCommand.set(e.getActionCommand()));
                root.add(menu);
                translate.invoke(translator,root);
                check(input.getText().equals("Overview"),"editable project text is unchanged");
                check(combo.getSelectedItem().equals("Overview") && combo.getItemAt(1).equals("Default"),"combo data model is unchanged");
                check(data.getValueAt(0,0).equals("STM32F103C8Tx") && data.getValueAt(0,2).equals("Active"),"chip names and table values are unchanged");
                check(data.getColumnName(0).equals("Mounted Device"),"table model column identifiers are unchanged");
                check(table.getColumnModel().getColumn(0).getHeaderValue().equals("板载器件"),"display table header is translated");
                table.getRowSorter().toggleSortOrder(1);
                check(table.getValueAt(0,0).equals("STM32G473ZET6"),"numeric table sorting works after translation");
                check(tabs.getTitleAt(0).equals("Project Manager"),"tab lookup identifier is unchanged");
                check(((JLabel)tabs.getTabComponentAt(0)).getText().equals("工程管理器"),"visual tab title is translated");
                button.doClick(0);
                check("Browse".equals(clickedCommand.get()),"button click action command stays English");
                check(button.getText().equals("浏览"),"button returns to Chinese after click");
                menu.doClick(0);
                check("Save".equals(menuCommand.get()),"menu click action command stays English");
                button.setModel(new DefaultButtonModel());
                translate.invoke(translator,root);
                button.doClick(0);
                check("Browse".equals(clickedCommand.get()),"button model replacement preserves action semantics");
                for (int i=0;i<20;i++) translate.invoke(translator,root);
                check(((DefaultButtonModel)button.getModel()).getChangeListeners().length <= 3,"repeated translation does not accumulate model listeners");
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        var bundles = Files.readAllLines(Path.of(args[1]));
        var installed = new HashMap<String,String>();
        for (var line : bundles) if (!line.isBlank()&&!line.startsWith("#")) {var p=line.split(",");installed.put(p[0],p[1]);}
        int resources=0, entries=0;
        try (var files=Files.list(Path.of(args[2]))) {
            for(var file:files.filter(p->p.toString().endsWith(".jar")).sorted().toList()) {
                try(var jar=new JarFile(file.toFile())) {
                    var attrs=jar.getManifest().getMainAttributes();
                    var host=attrs.getValue("Fragment-Host").split(";")[0];
                    check(attrs.getValue("Fragment-Host").contains("["+installed.get(host)+","+installed.get(host)+"]"),"fragment version matches installed host "+host);
                    check(jar.stream().noneMatch(e->e.getName().endsWith(".class")),"resource-only fragment "+host);
                    for(var entry:jar.stream().filter(e->e.getName().endsWith(".properties")).toList()) {
                        var props=new Properties();try(var in=jar.getInputStream(entry)){props.load(in);}
                        resources++;entries+=props.size();
                        for(var key:props.stringPropertyNames()) {
                            var value=props.getProperty(key);
                            if(value.matches("(?s).*\\{\\d+}.*")) {
                                var formatted=new MessageFormat(value,Locale.SIMPLIFIED_CHINESE).format(new Object[]{"ARG0","ARG1","ARG2"});
                                check(formatted.contains("ARG0"),"MessageFormat substitution "+key);
                            }
                        }
                    }
                }
            }
        }
        check(entries==373,"all 373 translated properties can be decoded");
        System.out.println("RESULT checks="+checks+" resources="+resources+" translatedProperties="+entries);
    }
}
