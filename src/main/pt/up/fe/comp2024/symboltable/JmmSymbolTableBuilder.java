package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {

        var imports = buildImports(root);


        var classDecl = root.getObject("c", JmmNode.class);
        SpecsCheck.checkArgument(Kind.CLASS_DECLARATION.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("className");

        var fields = buildFields(classDecl);

        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        /*var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);*/
        var superClass = classDecl.hasAttribute("extendedClass") ? classDecl.get("extendedClass") : "";

        Map<String, List<Symbol>> params = new HashMap<>();
        Map<String, List<Symbol>> locals = new HashMap<>();

        return new JmmSymbolTable(imports, superClass, fields, className, methods, returnTypes, params, locals);
    }

    private static List<String> buildImports(JmmNode program) {
        var importNodes = program.getChildren(IMPORT_DECLARATION);
        List<String> list = new ArrayList<>();
        for (JmmNode imp : importNodes) {
            list.add(imp.get("value"));
        }
        return list;
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        var var_decls = classDecl.getChildren(VAR_DECL);
        List<Symbol> list = new ArrayList<>();
        for (JmmNode var_decl : var_decls) {
            list.add(new Symbol(new Type(var_decl.getJmmChild(0).get("value"), false), var_decl.get("name")));
        }
        return list;
    }


    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, Type> map = new HashMap<>();
        var method_decls = classDecl.getChildren(METHOD_DECL);
        for (JmmNode methods: method_decls){
            JmmNode fstChild = methods.getChildren().get(0);
            if(!fstChild.getChildren().get(0).hasAttribute("value")){
                map.put(methods.get("name"), new Type(methods.getJmmChild(0).get("value"), true));
            }

        }


        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), Arrays.asList(new Symbol(intType, method.getJmmChild(1).get("name")))));

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        List<String> lista = new ArrayList<>();
        var method_decls = classDecl.getChildren(METHOD_DECL);

        for (JmmNode imp : method_decls) {
            lista.add(imp.get("name"));
        }
        return lista;

    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        // TODO: Simple implementation that needs to be expanded

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(intType, varDecl.get("name")))
                .toList();
    }


}
