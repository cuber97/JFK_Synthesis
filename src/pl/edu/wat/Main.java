package pl.edu.wat;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.type.PrimitiveType;

import javax.tools.*;
import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {
        final String fileName = "src\\Class.java";
        final String parsableFileName = "src\\ParsableFile";
        final String alteredFileName = "src\\ClassAltered.java";
        convertForSnippetToParsableForm(fileName, parsableFileName);
        CompilationUnit cu;
        try (FileInputStream in = new FileInputStream(parsableFileName)) {
            cu = JavaParser.parse(in);
        }

        cu.getChildNodesByType(MethodCallExpr.class)
                .stream()
                .filter(m -> m.getNameAsString().equals("FOR") && m.getArguments().size() == 5)
                .forEach(Main::replaceSnippetFORWithForStmt);

        cu.getClassByName("Class").get().setName("ClassAltered");

        try (FileWriter output = new FileWriter(new File(alteredFileName), false)) {
            output.write(cu.toString());
        }

        File[] files = {new File(alteredFileName)};
        String[] options = {"-d", "out//production//Synthesis"};

        convertForSnippetToPreviousForm(alteredFileName);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromFiles(Arrays.asList(files));
            compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    Arrays.asList(options),
                    null,
                    compilationUnits).call();

            diagnostics.getDiagnostics().forEach(d -> System.out.println(d.getMessage(null)));
        }
    }

    private static void convertForSnippetToParsableForm(String sourceFile, String destinyFile) {
        List<String> list = new LinkedList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(sourceFile));
             PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(destinyFile)))) {
            String s;
            while ((s = in.readLine()) != null) {
                s = s.replaceAll(", *>= *, *\\+", ", greaterEquals, increment");
                s = s.replaceAll(", *<= *, *\\+", ", lessEquals, increment");
                s = s.replaceAll(", *> *, *\\+", ", greater, increment");
                s = s.replaceAll(", *< *, *\\+", ", less, increment");
                s = s.replaceAll(", *>= *, *\\-", ", greaterEquals, decrement");
                s = s.replaceAll(", *<= *, *\\-", ", lessEquals, decrement");
                s = s.replaceAll(", *> *, *\\-", ", greater, decrement");
                s = s.replaceAll(", *< *, *\\-", ", less, decrement");
                list.add(s);
            }

            for (String line : list) {
                out.println(line);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void convertForSnippetToPreviousForm(String sourceFile) {
        List<String> list = new LinkedList<>();

        try (BufferedReader in = new BufferedReader(new FileReader(sourceFile))) {
            String s;

            while ((s = in.readLine()) != null) {
                s = s.replaceAll(", greaterEquals, increment", ", >=, +");
                s = s.replaceAll(", lessEquals, increment", ", <=, +");
                s = s.replaceAll(", greater, increment", ", >, +");
                s = s.replaceAll(", less, increment", ", <, +");
                s = s.replaceAll(", greaterEquals, decrement", ", >=, -");
                s = s.replaceAll(", lessEquals, decrement", ", <=, -");
                s = s.replaceAll(", greater, decrement", ", >, -");
                s = s.replaceAll(", less, decrement", ", <, -");
                list.add(s);
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(sourceFile)))) {
            for (String line : list) {
                out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ForStmt getForStmt(MethodCallExpr method) {
        List<Expression> childList = method.getArguments();
        if (!childList.get(0).toString().matches("[_a-zA-z]\\w*")) {
            return null;
        }
        VariableDeclarationExpr declarationExpr = new VariableDeclarationExpr(PrimitiveType.intType(), childList.get(0).toString());

        NameExpr nameExpr = new NameExpr(childList.get(1).toString());


        AssignExpr assignExpr = new AssignExpr();
        assignExpr.setOperator(AssignExpr.Operator.ASSIGN);
        assignExpr.setTarget(declarationExpr);
        assignExpr.setValue(nameExpr);

        NodeList<Expression> initExpressionsList = new NodeList<>();
        initExpressionsList.add(assignExpr);

        ForStmt forStatement = new ForStmt();
        forStatement.setInitialization(initExpressionsList);

        if (!childList.get(2).toString().matches("\\d+")) {
            return null;
        }

        BinaryExpr binaryExpr = new BinaryExpr();
        binaryExpr.setLeft(new NameExpr(childList.get(0).toString()));
        binaryExpr.setRight(new NameExpr(childList.get(2).toString()));

        String comparisonOperator = childList.get(3).toString();

        if (comparisonOperator.equals("greater")) {
            binaryExpr.setOperator(BinaryExpr.Operator.GREATER);
        } else if (comparisonOperator.equals("greaterEquals")) {
            binaryExpr.setOperator(BinaryExpr.Operator.GREATER_EQUALS);
        } else if (comparisonOperator.equals("less")) {
            binaryExpr.setOperator(BinaryExpr.Operator.LESS);
        } else if (comparisonOperator.equals("lessEquals")) {
            binaryExpr.setOperator(BinaryExpr.Operator.LESS_EQUALS);
        } else {
            return null;
        }

        forStatement.setCompare(binaryExpr);

        UnaryExpr updateExpr = new UnaryExpr();
        updateExpr.setExpression(new NameExpr(childList.get(0).toString()));

        String updateOperator = childList.get(4).toString();

        if (updateOperator.equals("increment")) {
            updateExpr.setOperator(UnaryExpr.Operator.POSTFIX_INCREMENT);
        } else if (updateOperator.equals("decrement")) {
            updateExpr.setOperator(UnaryExpr.Operator.POSTFIX_DECREMENT);
        } else {
            return null;
        }

        NodeList<Expression> updateExpressionsList = new NodeList<>();
        updateExpressionsList.add(updateExpr);

        forStatement.setUpdate(updateExpressionsList);
        forStatement.setBody(new BlockStmt());

        return forStatement;
    }

    private static void replaceSnippetFORWithForStmt(MethodCallExpr method) {
        ForStmt forStmt = getForStmt(method);
        if (forStmt == null) {
            return;
        } else {
            method.getParentNode().get().replace(forStmt);
        }
    }
}