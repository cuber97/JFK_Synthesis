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
        try(FileInputStream in = new FileInputStream(parsableFileName)){
            cu = JavaParser.parse(in);
        }

        cu.getChildNodesByType(MethodCallExpr.class)
			.stream()
			.filter(m -> m.getNameAsString().equalsIgnoreCase("FOR"))
			.forEach(Main::replaceSnippetFORWithForStmt);

        //new Rewriter().visit(cu, null);
        cu.getClassByName("Class").get().setName("ClassAltered");

        try(FileWriter output = new FileWriter(new File(alteredFileName), false)) {
            output.write(cu.toString());
        }

        File[] files = {new File(alteredFileName)};
        String[] options = { "-d", "out//production//Synthesis" };

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
        try(BufferedReader in = new BufferedReader(new FileReader(sourceFile));
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(destinyFile)))) {
            String s;
            while((s = in.readLine()) != null) {
                s = s.replaceAll(", *>=", ", greaterEquals");
                s = s.replaceAll(", *<=", ", lessEquals");
                s = s.replaceAll(", *>", ", greater");
                s = s.replaceAll(", *<", ", less");
                s = s.replaceAll(", *\\+", ", increment");
                s = s.replaceAll(", *\\-", ", decrement");
                //System.out.println(s);
                list.add(s);
            }

            for(String line: list) {
                out.println(line);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ForStmt getForStmt (MethodCallExpr method){
        List<Node> childList = method.getChildNodes();

        VariableDeclarationExpr declarationExpr = new VariableDeclarationExpr(PrimitiveType.intType(), childList.get(1).toString());

        NameExpr nameExpr = new NameExpr(childList.get(2).toString());


        AssignExpr assignExpr = new AssignExpr();
        assignExpr.setOperator(AssignExpr.Operator.ASSIGN);
        assignExpr.setTarget(declarationExpr);
        assignExpr.setValue(nameExpr);


        childList.forEach(m -> System.out.println(m));
        

        NodeList<Expression> initExpressionsList = new NodeList<>();
        initExpressionsList.add(assignExpr);

        ForStmt forStatement = new ForStmt();
        forStatement.setInitialization(initExpressionsList);

        BinaryExpr binaryExpr = new BinaryExpr();
        binaryExpr.setLeft(new NameExpr(childList.get(1).toString()));
        binaryExpr.setRight(new NameExpr(childList.get(3).toString()));

        String comparisonOperator = childList.get(4).toString();

        if(comparisonOperator.equals("greater")) {
            binaryExpr.setOperator(BinaryExpr.Operator.GREATER);
        }

        else if(comparisonOperator.equals("greaterEquals")) {
            binaryExpr.setOperator(BinaryExpr.Operator.GREATER_EQUALS);
        }

        else if(comparisonOperator.equals("less")) {
            binaryExpr.setOperator(BinaryExpr.Operator.LESS);
        }

        else if(comparisonOperator.equals("lessEquals")) {
            binaryExpr.setOperator(BinaryExpr.Operator.LESS_EQUALS);
        }

        forStatement.setCompare(binaryExpr);

        UnaryExpr updateExpr = new UnaryExpr();
        updateExpr.setExpression(new NameExpr(childList.get(1).toString()));

        String updateOperator = childList.get(5).toString();

        if(updateOperator.equals("increment")) {
            updateExpr.setOperator(UnaryExpr.Operator.POSTFIX_INCREMENT);
        }

        else if(updateOperator.equals("decrement")) {
            updateExpr.setOperator(UnaryExpr.Operator.POSTFIX_DECREMENT);
        }

        NodeList<Expression> updateExpressionsList = new NodeList<>();
        updateExpressionsList.add(updateExpr);

        forStatement.setUpdate(updateExpressionsList);
        forStatement.setBody(new BlockStmt());

        System.out.println(forStatement);

        return forStatement;
    }

    private static void replaceSnippetFORWithForStmt(MethodCallExpr method) {
        ForStmt block = getForStmt(method);
        method.getParentNode().get().replace(block);
    }
}